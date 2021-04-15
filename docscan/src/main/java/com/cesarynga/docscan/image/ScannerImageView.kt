package com.cesarynga.docscan.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.net.Uri
import android.util.AttributeSet
import android.widget.ImageView
import com.cesarynga.docscan.DocScan
import com.cesarynga.docscan.ScannerView
import com.cesarynga.docscan.exception.NoDocumentDetectedException
import com.cesarynga.docscan.saveInFile
import java.io.File
import kotlin.math.min

class ScannerImageView(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int
) : ScannerView(context, attrs, defStyleAttr, defStyleRes) {

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
        context,
        attrs,
        defStyleAttr,
        0
    )

    private val imageView: ImageView = ImageView(context)

    init {
        addView(imageView, 0)
    }

    fun scan(bitmap: Bitmap, selectAllOnError: Boolean = true, callback: ScannerImageCallback) {
        imageView.post {
            // Display bitmap efficiently
            val scaleFactor =
                getScaleFactor(bitmap.width, bitmap.height, imageView.width, imageView.height)
            val scaledBitmap = scaleBitmap(bitmap, scaleFactor)
            imageView.setImageBitmap(scaledBitmap)

            val corners = DocScan.scan(scaledBitmap)

            if (corners.isNotEmpty()) {
                val fixedCorners = fixPointsPosition(corners, scaledBitmap.width, scaledBitmap.height)
                quadrangleView.setCorners(fixedCorners)

                val scaledCorners = scalePoints(corners, 1f / scaleFactor)
                val scanResult = ScanResult(context, bitmap, scaledCorners)

                callback.onScanSuccess(scanResult)
            } else {
                if (selectAllOnError) {
                    val cornersWholeImage = listOf(
                        Point(0, 0),
                        Point(scaledBitmap.width, 0),
                        Point(scaledBitmap.width, scaledBitmap.height),
                        Point(0, scaledBitmap.height)
                    )

                    val fixedCorners = fixPointsPosition(cornersWholeImage, scaledBitmap.width, scaledBitmap.height)
                    quadrangleView.setCorners(fixedCorners)

                    val scaledCorners = scalePoints(cornersWholeImage, 1f / scaleFactor)
                    val scanResult = ScanResult(context, bitmap, scaledCorners)

                    callback.onScanSuccess(scanResult)
                } else {
                    callback.onScanError(NoDocumentDetectedException("Document not detected on given bitmap"))
                }
            }
        }
    }

    private fun scalePoints(points: List<Point>, scaleFactor: Float): List<Point> {
        val scaledPoints = mutableListOf<Point>()
        for (point in points) {
            scaledPoints.add(
                Point(
                    (point.x * scaleFactor).toInt(),
                    (point.y * scaleFactor).toInt()
                )
            )
        }
        return scaledPoints
    }

    /**
     * Fix the position of the Points to be centered in the QuadrangleView according to the input bitmap.
     */
    private fun fixPointsPosition(points: List<Point>, bitmapWidth: Int, bitmapHeight: Int): List<Point> {
        val fixedPoints = mutableListOf<Point>()

        val quadrangleViewWidth = quadrangleView.width
        val quadrangleViewHeight = quadrangleView.height

        val xDiff = (quadrangleViewWidth - bitmapWidth) / 2
        val yDiff = (quadrangleViewHeight - bitmapHeight) / 2

        for (point in points) {
            fixedPoints.add(
                Point(
                    point.x + xDiff,
                    point.y + yDiff
                )
            )
        }
        return fixedPoints
    }

    /**
     * Determine how much to scale down the image
     */
    private fun getScaleFactor(
        bitmapWidth: Int,
        bitmapHeight: Int,
        targetWidth: Int,
        targetHeight: Int
    ) = min(targetWidth / bitmapWidth.toFloat(), targetHeight / bitmapHeight.toFloat())


    private fun scaleBitmap(src: Bitmap, scaleFactor: Float): Bitmap {
        val photoWidth: Int = src.width
        val photoHeight: Int = src.height

        return Bitmap.createScaledBitmap(
            src,
            (photoWidth * scaleFactor).toInt(),
            (photoHeight * scaleFactor).toInt(),
            false
        )
    }

    interface ScannerImageCallback {
        fun onScanSuccess(scanResult: ScanResult)

        fun onScanError(e: Exception)
    }

    class ScanResult(
        private val context: Context,
        val bitmap: Bitmap,
        val corners: List<Point>
    ) {

        fun crop(
            outputFile: File = File.createTempFile(
                "docscan_cropped",
                PHOTO_EXTENSION,
                context.cacheDir
            )
        ): Uri {
            val croppedBitmap = DocScan.crop(bitmap, corners)

            croppedBitmap.saveInFile(outputFile)

            return Uri.fromFile(outputFile)
        }
    }

    companion object {
        private const val TAG = "ScannerCameraView"
        private const val PHOTO_EXTENSION = ".jpg"
    }
}