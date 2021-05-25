package com.cesarynga.docscan.camera

import android.content.Context
import android.graphics.Color
import android.graphics.PointF
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.util.TypedValue
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.cesarynga.docscan.ScannerView
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

typealias TakePictureCallback = (uri: Uri?) -> Unit

class CameraScannerView(
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

    private val previewView = PreviewView(context)

    /** Blocking camera operations are performed using this executor */
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val lifecycleOwner: LifecycleOwner

    private lateinit var imageCapture: ImageCapture

    init {
        if (context !is LifecycleOwner) {
            throw Exception("The activity/fragment that contains this view must implement LifecycleOwner interface")
        }
        lifecycleOwner = context

        lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
            fun connectListener() {
                quadrangleView.clear()
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun disconnectListener() {
                cameraExecutor.shutdown()
            }
        })

        addView(previewView, 0)
        previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
    }

    fun startCamera() {
        previewView.post {
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
//                        .setTargetAspectRatio(screenAspectRatio)
                        .setTargetResolution(Size(1080, 1920))
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
                                ) { bitmap, corners ->
                                    if (corners.isNotEmpty()) {
                                        val previewCorners = convertPointsToPreviewCoordinates(corners, previewView.width, previewView.height, bitmap.width, bitmap.height)
                                        quadrangleView.setCorners(previewCorners)
                                    } else {
                                        quadrangleView.clear()
                                    }
                                })
                        }

                    // Image Capture
                    imageCapture = ImageCapture.Builder()
                        .setTargetAspectRatio(screenAspectRatio)
                        .setTargetRotation(rotation)
                        .setIoExecutor(cameraExecutor)
                        .build()

                    // Select back camera as a default
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    // Must unbind the use-cases before rebinding them
                    cameraProvider.unbindAll()

                    try {
                        // Bind use cases to camera
                        // A variable number of use-cases can be passed here -
                        // camera provides access to CameraControl & CameraInfo
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner, cameraSelector, preview, imageAnalyzer, imageCapture
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Use case binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(context)
            )
        }
    }

    var fillColor: Int = Color.argb(63, 255, 255, 255)
        set(value) {
            field = value
            quadrangleView.fillColor = value
        }

    var strokeColor: Int = Color.WHITE
        set(value) {
            field = value
            quadrangleView.strokeColor = value
        }

    var strokeWidth: Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics)
        set(value) {
            field = value
            quadrangleView.strokeWidth = value
        }

    fun takePicture(
        outputFile: File = File.createTempFile(
            "docscan",
            PHOTO_EXTENSION,
            context.cacheDir
        ), callback: TakePictureCallback
    ) {
        if (!this::imageCapture.isInitialized) return

        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(error: ImageCaptureException) {
                    Log.e(TAG, "Image capture error", error.cause)
                    quadrangleView.clear()
                    callback(null)
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(outputFile)
                    Log.d(TAG, "Image capture succeeded: $savedUri")
                    callback(savedUri)
                }
            })
    }

    private fun convertPointsToPreviewCoordinates(
        corners: List<PointF>,
        previewWidth: Int,
        previewHeight: Int,
        imageWidth: Int,
        imageHeight: Int
    ) : List<PointF> {
        val widthRatio = previewWidth.toFloat() / imageWidth
        val heightRatio = previewHeight.toFloat() / imageHeight
        val previewCorners = mutableListOf<PointF>()
        corners.forEach { point ->
            previewCorners.add(PointF(point.x * widthRatio, point.y * heightRatio))
        }
        return previewCorners
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
        private const val TAG = "CameraScannerView"
        private const val PHOTO_EXTENSION = ".jpg"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }
}