package com.cesarynga.docscan

import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.sqrt

object DocScan {

    init {
        OpenCVLoader.initDebug()
    }

    // =====================================
    // Edge Detection
    //======================================
    fun scan(
        bitmap: Bitmap,
        threshold1: Int = 50,
        threshold2: Int = 100
    ): List<android.graphics.Point> {
        var mat = bitmapToMat(bitmap)

        mat = grayScale(mat)

        val blur = blur(mat)

        mat = canny(blur, threshold1, threshold2)

        mat = dilate(mat)

        mat = erode(mat)

        val contours = findContours(mat)
        mat.release()

        if (contours.isNotEmpty()) {
            val biggest = biggestContours(contours) ?: return emptyList()

            val reordered = reorderPoints(biggest)

            val stringBuilder = StringBuilder()
            reordered.forEachIndexed { index, point ->
                if (index == 0) stringBuilder.append("[")
                stringBuilder.append("(${point.x},${point.y})")
                if (index == reordered.size - 1) stringBuilder.append("]")
            }
            Log.d("DocScan", "Corners: $stringBuilder")
            return reordered
        }

        return emptyList()
    }

    private fun grayScale(src: Mat): Mat {
        val gray = Mat()
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)
        return gray
    }

    private fun blur(src: Mat): Mat {
        val blurred = Mat()
        Imgproc.GaussianBlur(src, blurred, Size(5.0, 5.0), 1.0)
        return blurred
    }

    private fun canny(src: Mat, threshold1: Int, threshold2: Int): Mat {
        val canny = Mat()
        Imgproc.Canny(src, canny, threshold1.toDouble(), threshold2.toDouble())
        return canny
    }

    private fun dilate(src: Mat): Mat {
        val kernelSize = 2.0

        val dilate = Mat()
        Imgproc.dilate(
            src,
            dilate,
            Mat.ones(Size(2 * kernelSize + 1, 2 * kernelSize + 1), CvType.CV_8U),
            Point(kernelSize, kernelSize),
            2
        )
        return dilate
    }

    private fun erode(src: Mat): Mat {
        val kernelSize = 2.0

        val erode = Mat()
        Imgproc.erode(
            src,
            erode,
            Mat.ones(Size(2 * kernelSize + 1, 2 * kernelSize + 1), CvType.CV_8U),
            Point(kernelSize, kernelSize),
            1
        )
        return erode
    }

    private fun findContours(src: Mat): List<MatOfPoint> {
        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(
            src,
            contours,
            Mat(),
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )
        return contours
    }

    private fun biggestContours(contours: List<MatOfPoint>, minArea: Int = 5000): MatOfPoint2f? {
        var biggest: MatOfPoint2f? = null
        var maxArea = 0.0

        contours.forEach { contour ->
            val area = Imgproc.contourArea(contour)
            if (area > minArea) {
                val matFloat = MatOfPoint2f()
                contour.convertTo(matFloat, CvType.CV_32FC2)
                val arcLength = Imgproc.arcLength(matFloat, true)

                val approx = MatOfPoint2f()
                Imgproc.approxPolyDP(matFloat, approx, 0.02 * arcLength, true)

                if (area > maxArea && isRectangle(approx)) {
                    biggest = approx
                    maxArea = area
                }
            }
        }
        return biggest
    }

    private fun reorderPoints(point2f: MatOfPoint2f): List<android.graphics.Point> {
        val points = listOf(*point2f.toArray())

        val centerPoint = android.graphics.Point()
        val size = points.size

        points.forEach { pointF ->
            centerPoint.x += (pointF.x / size).toInt()
            centerPoint.y += (pointF.y / size).toInt()
        }

        val orderedPoints = mutableListOf(
            android.graphics.Point(),
            android.graphics.Point(),
            android.graphics.Point(),
            android.graphics.Point()
        )

        points.forEach { point ->
            var index = -1
            if (point.x < centerPoint.x && point.y < centerPoint.y) {
                index = 0
            } else if (point.x > centerPoint.x && point.y < centerPoint.y) {
                index = 1
            } else if (point.x > centerPoint.x && point.y > centerPoint.y) {
                index = 2
            } else if (point.x < centerPoint.x && point.y > centerPoint.y) {
                index = 3
            }
            if (index != -1) {
                orderedPoints[index].x = point.x.toInt()
                orderedPoints[index].y = point.y.toInt()
            } else {
                return emptyList()
            }
        }
        return orderedPoints
    }

    private fun isRectangle(polygon: MatOfPoint2f): Boolean {
        if (polygon.rows() != 4) {
            return false
        }

        val polygonInt = MatOfPoint()
        polygon.convertTo(polygonInt, CvType.CV_32S)

        if (!Imgproc.isContourConvex(polygonInt)) {
            return false
        }

        // Check if the all angles are more than 72.54 degrees (cos 0.3).
        var maxCosine = 0.0
        val approxPoints = polygon.toArray()
        for (i in 2..4) {
            val cosine: Double = abs(
                angle(
                    approxPoints[i % 4],
                    approxPoints[i - 2], approxPoints[i - 1]
                )
            )
            maxCosine = cosine.coerceAtLeast(maxCosine)
        }
        return maxCosine < 0.3
    }

    private fun angle(p1: Point, p2: Point, p0: Point): Double {
        val dx1 = p1.x - p0.x
        val dy1 = p1.y - p0.y
        val dx2 = p2.x - p0.x
        val dy2 = p2.y - p0.y
        return (dx1 * dx2 + dy1 * dy2) / sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10)
    }

    // =====================================
    // Dewarping
    //======================================
    fun crop(bitmap: Bitmap, corners: List<android.graphics.Point>): Bitmap {
        if (corners.size != 4) {
            throw IllegalArgumentException("corners param must have 4 items. Current corners params has ${corners.size} items.")
        }

        val imageMat = bitmapToMat(bitmap)

        val sortedCorners = MatOfPoint2f()
        val pointArray = arrayOf(
            Point(corners[0].x.toDouble(), corners[0].y.toDouble()),
            Point(corners[1].x.toDouble(), corners[1].y.toDouble()),
            Point(corners[2].x.toDouble(), corners[2].y.toDouble()),
            Point(corners[3].x.toDouble(), corners[3].y.toDouble())
        )
        sortedCorners.fromArray(*pointArray)

        val size = getRectangleSize(sortedCorners)

        val result: Mat = Mat.zeros(size, imageMat.type())
        val imageOutline = getOutline(result)

        val transformation = Imgproc.getPerspectiveTransform(sortedCorners, imageOutline)

        Imgproc.warpPerspective(imageMat, result, transformation, size)

        return matToBitmap(result)
    }

    private fun getRectangleSize(rectangle: MatOfPoint2f): Size? {
        val corners = rectangle.toArray()
        val top = getDistance(corners[0], corners[1])
        val right = getDistance(corners[1], corners[2])
        val bottom = getDistance(corners[2], corners[3])
        val left = getDistance(corners[3], corners[0])
        val averageWidth = (top + bottom) / 2f
        val averageHeight = (right + left) / 2f
        return Size(Point(averageWidth, averageHeight))
    }

    private fun getDistance(p1: Point, p2: Point): Double {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun getOutline(image: Mat): MatOfPoint2f? {
        val topLeft = Point(0.0, 0.0)
        val topRight = Point(image.cols().toDouble(), 0.0)
        val bottomRight = Point(image.cols().toDouble(), image.rows().toDouble())
        val bottomLeft = Point(0.0, image.rows().toDouble())
        val points = arrayOf(topLeft, topRight, bottomRight, bottomLeft)
        val result = MatOfPoint2f()
        result.fromArray(*points)
        return result
    }

    private fun bitmapToMat(bitmap: Bitmap): Mat {
        val mat = Mat(bitmap.height, bitmap.width, CvType.CV_8U, Scalar(4.0))
        val bitmap32: Bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        Utils.bitmapToMat(bitmap32, mat)
        return mat
    }

    private fun matToBitmap(mat: Mat): Bitmap {
        val bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, bitmap)
        return bitmap
    }
}