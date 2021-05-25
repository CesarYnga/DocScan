package com.cesarynga.docscan.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.widget.ImageView
import com.cesarynga.docscan.DocScan
import com.cesarynga.docscan.R
import com.cesarynga.docscan.ScannerView
import com.cesarynga.docscan.exception.NoDocumentDetectedException
import com.cesarynga.docscan.saveInFile
import java.io.File
import kotlin.math.min

class ImageScannerView(
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
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.ImageScannerView,
            defStyleAttr,
            defStyleRes
        ).apply {
            val showPointers = getBoolean(R.styleable.ImageScannerView_showPointers, true)
            quadrangleView.showPointers = showPointers
        }
        addView(imageView, 0)
    }

    fun scan(bitmap: Bitmap, selectAllOnError: Boolean = true, callback: ScannerImageCallback) {
        imageView.post {
            // Display bitmap efficiently
            val scaleFactor =
                getScaleFactor(bitmap.width, bitmap.height, imageView.width, imageView.height)
            val scaledBitmap = scaleBitmap(bitmap, scaleFactor)
            imageView.setImageBitmap(scaledBitmap)

            quadrangleView.pointerMinPosition.x = (imageView.width - scaledBitmap.width) / 2f
            quadrangleView.pointerMinPosition.y = (imageView.height - scaledBitmap.height) / 2f

            quadrangleView.pointerMaxPosition.x =
                (imageView.width - scaledBitmap.width) / 2f + scaledBitmap.width
            quadrangleView.pointerMaxPosition.y =
                (imageView.height - scaledBitmap.height) / 2f + scaledBitmap.height

            val corners = DocScan.scan(scaledBitmap)

            if (corners.isNotEmpty()) {
                val fixedCorners =
                    fixPointsPosition(corners, scaledBitmap.width, scaledBitmap.height)
                quadrangleView.setCorners(fixedCorners)

                val scaledCorners = scalePoints(corners, 1f / scaleFactor)
                val scanResult = ScanResult(context, bitmap, scaledCorners)

                callback.onScanSuccess(scanResult)
            } else {
                if (selectAllOnError) {
                    val cornersWholeImage = listOf(
                        PointF(0f, 0f),
                        PointF(scaledBitmap.width.toFloat(), 0f),
                        PointF(scaledBitmap.width.toFloat(), scaledBitmap.height.toFloat()),
                        PointF(0f, scaledBitmap.height.toFloat())
                    )

                    val fixedCorners = fixPointsPosition(
                        cornersWholeImage,
                        scaledBitmap.width,
                        scaledBitmap.height
                    )
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

    private fun scalePoints(points: List<PointF>, scaleFactor: Float): List<PointF> {
        val scaledPoints = mutableListOf<PointF>()
        for (point in points) {
            scaledPoints.add(
                PointF(
                    point.x * scaleFactor,
                    point.y * scaleFactor
                )
            )
        }
        return scaledPoints
    }

    /**
     * Fix the position of the Points to be centered in the QuadrangleView according to the input bitmap.
     */
    private fun fixPointsPosition(
        points: List<PointF>,
        bitmapWidth: Int,
        bitmapHeight: Int
    ): List<PointF> {
        val fixedPoints = mutableListOf<PointF>()

        val quadrangleViewWidth = quadrangleView.width
        val quadrangleViewHeight = quadrangleView.height

        val xDiff = (quadrangleViewWidth - bitmapWidth) / 2
        val yDiff = (quadrangleViewHeight - bitmapHeight) / 2

        points.forEach {
            fixedPoints.add(
                PointF(
                    it.x + xDiff,
                    it.y + yDiff
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
        val corners: List<PointF>
    ) {

        fun crop(
            outputFile: File = File.createTempFile(
                "docscan_cropped",
                PHOTO_EXTENSION,
                context.cacheDir
            ),
            quality: Int = 100
        ): Uri {
            val croppedBitmap = DocScan.crop(bitmap, corners)
            Log.d(TAG, "crop: size ${croppedBitmap.width}x${croppedBitmap.height}")
            croppedBitmap.saveInFile(outputFile, quality)

            return Uri.fromFile(outputFile)
        }
    }

    companion object {
        private const val TAG = "ImageScannerView"
        private const val PHOTO_EXTENSION = ".jpg"
    }
}