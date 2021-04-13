package com.cesarynga.docscan

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View

internal class QuadrangleView(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int
) :
    View(context, attrs, defStyleAttr, defStyleRes) {

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
        context,
        attrs,
        defStyleAttr,
        0
    )

    var fillColor: Int = Color.argb(63, 255, 255, 255)
        set(value) {
            field = value
            fillPaint.color = value
        }

    var strokeColor: Int = Color.WHITE
        set(value) {
            field = value
            strokePaint.color = value
        }

    var strokeWidth: Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics)
        set(value) {
            field = value
            strokePaint.strokeWidth = value
        }

    private val strokePaint = Paint().apply {
        color = strokeColor
        style = Paint.Style.STROKE
        strokeWidth = this@QuadrangleView.strokeWidth
        isAntiAlias = true
    }

    private val fillPaint = Paint().apply {
        color = fillColor
        style = Paint.Style.FILL
    }

    private val quadranglePath = Path()

    private var corners = emptyList<Point>()

    fun setCorners(corners: List<Point>) {
        if (corners.size != 4) {
            throw IllegalArgumentException("setCorners: corner list must have 4 items. Current corners params has ${corners.size} items.")
        } else {
            this.corners = corners
            invalidate()
        }
    }


    fun clear() {
        this.corners = emptyList()
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.apply {
            quadranglePath.reset()

            if (corners.isNotEmpty()) {
                quadranglePath.moveTo(corners[0].x.toFloat(), corners[0].y.toFloat())
                quadranglePath.lineTo(corners[1].x.toFloat(), corners[1].y.toFloat())
                quadranglePath.lineTo(corners[2].x.toFloat(), corners[2].y.toFloat())
                quadranglePath.lineTo(corners[3].x.toFloat(), corners[3].y.toFloat())
                quadranglePath.close()
            }

            drawPath(quadranglePath, fillPaint)
            drawPath(quadranglePath, strokePaint)
        }
    }
}