@file:JvmName("DocScan")
@file:JvmMultifileClass

package com.cesarynga.docscan

import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc


fun init() {
    OpenCVLoader.initDebug()
}

fun scan(bitmap: Bitmap, threshold1: Int, threshold2: Int): List<android.graphics.Point> {
    var mat = bitmapToMat(bitmap)

    mat = grayScale(mat)

    val blur = blur(mat)

    mat = canny(blur, threshold1, threshold2)

    mat = dilate(mat)

    mat = erode(mat)

    val contours = findContours(mat)
    val biggest = biggestContours(contours)
    val reordered = reorderPoints(biggest)

    for (point in reordered) {
        Log.d("CONTOURS", "${point}")
    }

//    val result = matToBitmap(mat)
    mat.release()

    return reordered
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
    Imgproc.findContours(src, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
    return contours
}

private fun biggestContours(contours: List<MatOfPoint>): MatOfPoint2f {
    var biggest = MatOfPoint2f()
    var maxArea = 0
    for (contour in contours) {
        val area = Imgproc.contourArea(contour)
        if (area > 5000) {
            val matFloat = MatOfPoint2f()
            contour.convertTo(matFloat, CvType.CV_32FC2)
            val arcLength = Imgproc.arcLength(matFloat, true)

            val approx = MatOfPoint2f()
            Imgproc.approxPolyDP(matFloat, approx, 0.02 * arcLength, true)

            if (area > maxArea && approx.rows() == 4) {
                biggest = approx
            }
        }
    }
    return biggest
}

private fun reorderPoints(point2f: MatOfPoint2f): List<android.graphics.Point> {
    val points = listOf(*point2f.toArray())

    val centerPoint = android.graphics.Point()
    val size = points.size
    for (pointF in points) {
        centerPoint.x += (pointF.x / size).toInt()
        centerPoint.y += (pointF.y / size).toInt()
    }
    val orderedPoints = mutableListOf(
        android.graphics.Point(),
        android.graphics.Point(),
        android.graphics.Point(),
        android.graphics.Point()
    )
    for (point in points) {
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
        orderedPoints[index].x = point.x.toInt()
        orderedPoints[index].y = point.y.toInt()
    }
    return orderedPoints
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
