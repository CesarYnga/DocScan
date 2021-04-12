package com.cesarynga.docscan.camera

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.cesarynga.docscan.QuadrangleView
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ScannerCameraView(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
        context,
        attrs,
        defStyleAttr,
        0
    )

    private val previewView = PreviewView(context)
    private val quadrangleView = QuadrangleView(context)

    /** Blocking camera operations are performed using this executor */
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val lifecycleOwner: LifecycleOwner

    init {
        if (context !is LifecycleOwner) {
            throw Exception("The activity/fragment that contains this view must implement LifecycleOwner interface")
        }
        lifecycleOwner = context

        addView(previewView)
        previewView.scaleType = PreviewView.ScaleType.FILL_CENTER

        addView(quadrangleView)

        setupCamera()
    }

    private fun setupCamera() = previewView.post {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                val previewWidth = previewView.width
                val previewHeight = previewView.height
                Log.d(TAG, "Preview size: $previewWidth x $previewHeight")

                val screenAspectRatio = aspectRatio(previewWidth, previewHeight)
                Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

                val rotation = previewView.display.rotation
                Log.d(TAG, "Preview rotation: $rotation")

                // Preview
                val preview = Preview.Builder()
                    .setTargetAspectRatio(screenAspectRatio)
                    .setTargetRotation(rotation)
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                // Image Analyzer
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setTargetAspectRatio(screenAspectRatio)
                    .setTargetRotation(rotation)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(
                            cameraExecutor,
                            QuadrangleAnalyzer(
                                context,
                                previewView.width,
                                previewView.height
                            ) { corners ->
                                if (corners.size == 4) {
                                    quadrangleView.setCorners(corners)
                                } else {
                                    quadrangleView.clear()
                                }
                            })
                    }

                // Select back camera as a default
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Must unbind the use-cases before rebinding them
                cameraProvider.unbindAll()

                try {
                    // A variable number of use-cases can be passed here -
                    // camera provides access to CameraControl & CameraInfo
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner, cameraSelector, preview, imageAnalyzer
                    )
                } catch (exc: Exception) {
                    Log.e(TAG, "Use case binding failed", exc)
                }
            }, ContextCompat.getMainExecutor(context)
        )
    }

    /**
     *  [androidx.camera.core.ImageAnalysisConfig] requires enum value of
     *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
     *
     *  Detecting the most suitable ratio for dimensions provided in @params by counting absolute
     *  of preview ratio to one of the provided values.
     *
     *  @param width - preview width
     *  @param height - preview height
     *  @return suitable aspect ratio {@link androidx.camera.core.AspectRatio}
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    companion object {
        private const val TAG = "ScannerCameraView"
        private const val PHOTO_EXTENSION = ".jpg"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }
}