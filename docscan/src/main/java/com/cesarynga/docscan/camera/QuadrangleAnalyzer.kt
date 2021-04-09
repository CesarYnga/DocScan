package com.cesarynga.docscan.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.cesarynga.docscan.rotate
import com.cesarynga.docscan.scan
import com.cesarynga.docscan.yuvToRgb

typealias QuadrangleAnalyzerListener = (corners: List<Point>) -> Unit

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
            Log.d(TAG, "analyze: bitmap size=${bitmap.width}x${bitmap.height}]")

            val corners = scan(bitmap, 50, 100)

            convertPointsToPreviewCoordinates(corners, previewWidth, previewHeight, bitmap.width, bitmap.height)

            analyzerListener(corners)
        }

        imageProxy.close()
    }

    private fun convertPointsToPreviewCoordinates(
        corners: List<Point>,
        previewWidth: Int,
        previewHeight: Int,
        imageWidth: Int,
        imageHeight: Int
    ) {
        val widthRatio = previewWidth.toFloat() / imageWidth
        val heightRatio = previewHeight.toFloat() / imageHeight
        corners.forEach { point ->
            point.x = (point.x * widthRatio).toInt()
            point.y = (point.y * heightRatio).toInt()
        }
    }

    companion object {
        private const val TAG = "QuadrangleAnalyzer"
    }
}