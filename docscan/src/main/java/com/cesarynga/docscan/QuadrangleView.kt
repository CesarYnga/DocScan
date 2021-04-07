package com.cesarynga.docscan

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class QuadrangleView(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) :
    View(context, attrs, defStyleAttr, defStyleRes) {

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?,defStyleAttr: Int) : this(context, attrs, 0, 0)

    private val borderPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 8f
        isAntiAlias = true
    }

    private val fillPaint = Paint().apply {
        color = Color.argb(75, 0, 255, 0)
        style = Paint.Style.FILL
    }

    private val quadranglePath = Path()

    private var corners = emptyList<Point>()

    fun setCorners(corners : List<Point>) {
        if (corners.size != 4) throw IllegalArgumentException("Corners list for QuadrangleView must have 4 items.")

        this.corners = corners

        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.apply {
            save()

            quadranglePath.reset()

            if (corners.isNotEmpty()) {
                quadranglePath.moveTo(corners[0].x.toFloat(), corners[0].y.toFloat())
                quadranglePath.lineTo(corners[1].x.toFloat(), corners[1].y.toFloat())
                quadranglePath.lineTo(corners[2].x.toFloat(), corners[2].y.toFloat())
                quadranglePath.lineTo(corners[3].x.toFloat(), corners[3].y.toFloat())
                quadranglePath.close()
            }

            canvas.drawPath(quadranglePath, fillPaint)
            canvas.drawPath(quadranglePath, borderPaint)

            restore()
        }
    }
}