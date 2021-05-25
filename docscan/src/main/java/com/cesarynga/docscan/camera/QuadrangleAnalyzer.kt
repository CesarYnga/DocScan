package com.cesarynga.docscan.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.cesarynga.docscan.DocScan
import com.cesarynga.docscan.rotate
import com.cesarynga.docscan.yuvToRgb
import kotlin.math.min

typealias QuadrangleAnalyzerListener = (bitmap: Bitmap, corners: List<PointF>) -> Unit

class QuadrangleAnalyzer(
    private val context: Context,
    private val previewWidth: Int,
    private val previewHeight: Int,
    private val analyzerListener: QuadrangleAnalyzerListener
) : ImageAnalysis.Analyzer {

    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        imageProxy.image?.let {
            var bitmap = it.yuvToRgb(context)

            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            Log.d(TAG, "analyze: rotationDegrees=$rotationDegrees")

            bitmap = bitmap.rotate(rotationDegrees.toFloat())
            Log.d(TAG, "analyze: bitmap size=${bitmap.width}x${bitmap.height}")

            bitmap = cropBitmapToMatchPreviewSize(bitmap, previewWidth, previewHeight)
            Log.d(TAG, "analyze: cropped bitmap size=${bitmap.width}x${bitmap.height}]")

            val corners = DocScan.scan(bitmap)

            Handler(Looper.getMainLooper()).post {
                analyzerListener(bitmap, corners)
            }
        }

        imageProxy.close()
    }

    private fun cropBitmapToMatchPreviewSize(bitmap: Bitmap, previewWidth: Int, previewHeight: Int): Bitmap {
        val scaleFactor: Float = min(
            bitmap.width / previewWidth.toFloat(),
            bitmap.height / previewHeight.toFloat()
        )

        Log.d(TAG, "cropBitmapToMatchPreviewSize: scale factor: $scaleFactor")

        val scalePreviewWidth = (previewWidth * scaleFactor)
        val scalePreviewHeight = (previewHeight * scaleFactor)

        Log.d(TAG, "cropBitmapToMatchPreviewSize: scaled size: $scalePreviewWidth x $scalePreviewHeight")

        var croppedBitmap = bitmap
        if (bitmap.width > scalePreviewWidth) {
            // bitmap needs to be cropped horizontally
            croppedBitmap = Bitmap.createBitmap(bitmap, ((bitmap.width - scalePreviewWidth) / 2).toInt(), 0, scalePreviewWidth.toInt(), bitmap.height)
        } else if (bitmap.height > scalePreviewHeight) {
            // bitmap needs to be cropped vertically
            croppedBitmap = Bitmap.createBitmap(bitmap, 0, ((bitmap.height - scalePreviewHeight) / 2).toInt(), bitmap.width, scalePreviewHeight.toInt())
        }
        return croppedBitmap
    }

    companion object {
        private const val TAG = "QuadrangleAnalyzer"
    }
}