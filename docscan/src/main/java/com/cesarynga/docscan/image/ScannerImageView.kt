package com.cesarynga.docscan.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import com.cesarynga.docscan.DocScan
import com.cesarynga.docscan.QuadrangleView
import kotlin.math.min

class ScannerImageView(
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

    private val imageView: ImageView = ImageView(context)
    private val quadrangleView: QuadrangleView = QuadrangleView(context)

    private var scaleFactor = 1f
    private var points = emptyList<Point>()
    private val docScan = DocScan

    init {
        addView(imageView)
        addView(quadrangleView)
    }

    fun setBitmap(bitmap: Bitmap) {
        imageView.post {
            // Display bitmap efficiently
            scaleFactor =
                getScaleFactor(bitmap.width, bitmap.height, imageView.width, imageView.height)
            val scaledBitmap = scaleBitmap(bitmap, scaleFactor)
            imageView.setImageBitmap(scaledBitmap)

            points = docScan.scan(bitmap)

            val scaledPoints = scalePoints(points, scaleFactor)

            movePoints(scaledPoints, scaledBitmap.width, scaledBitmap.height)

            quadrangleView.setCorners(scaledPoints)
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
}