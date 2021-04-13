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

    private lateinit var bitmap: Bitmap
    private var scaleFactor = 1f

    init {
        addView(imageView, 0)
    }

    fun scan(bitmap: Bitmap, selectAllOnError: Boolean = false, callback: ScannerImageCallback) {
        imageView.post {
            this.bitmap = bitmap
            // Display bitmap efficiently
            scaleFactor =
                getScaleFactor(bitmap.width, bitmap.height, imageView.width, imageView.height)
            val scaledBitmap = scaleBitmap(bitmap, scaleFactor)
            imageView.setImageBitmap(scaledBitmap)

            val corners = DocScan.scan(bitmap)

            if (corners.isNotEmpty()) {
                val scaledCorners = scalePoints(corners, scaleFactor)
                movePoints(scaledCorners, scaledBitmap.width, scaledBitmap.height)
                quadrangleView.setCorners(scaledCorners)

                val scanResult = ScanResult(context, bitmap, corners)

                callback.onScanSuccess(scanResult)
            } else {
                if (selectAllOnError) {
                    val cornersWholeImage = listOf(
                        Point(0, 0),
                        Point(bitmap.width, 0),
                        Point(bitmap.width, bitmap.height),
                        Point(0, bitmap.height)
                    )
                    val scaledCorners = scalePoints(cornersWholeImage, scaleFactor)
                    movePoints(scaledCorners, scaledBitmap.width, scaledBitmap.height)
                    quadrangleView.setCorners(scaledCorners)

                    val scanResult = ScanResult(context, bitmap, cornersWholeImage)

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

    private fun movePoints(points: List<Point>, bitmapWidth: Int, bitmapHeight: Int) {
        val quadrangleViewWidth = quadrangleView.width
        val quadrangleViewHeight = quadrangleView.height

        val xDiff = (quadrangleViewWidth - bitmapWidth) / 2
        val yDiff = (quadrangleViewHeight - bitmapHeight) / 2

        for (point in points) {
            point.x += xDiff
            point.y += yDiff
        }
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