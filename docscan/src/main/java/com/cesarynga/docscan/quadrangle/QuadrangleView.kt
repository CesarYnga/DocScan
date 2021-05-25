package com.cesarynga.docscan.quadrangle

import android.content.Context
import android.graphics.*
import android.util.TypedValue
import android.widget.FrameLayout
import kotlin.math.ceil

internal class QuadrangleView(context: Context) : FrameLayout(context) {

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

    private var corners = emptyList<PointF>()

    private val pointers = mutableListOf<QuadranglePointerView>()

    var showPointers = false
    set(value) {
        field = value
        if (!value) {
            clearPointers()
        }
        invalidate()
    }

    val pointerMinPosition = PointF()
    val pointerMaxPosition = PointF()

    fun setCorners(corners: List<PointF>, onPointerMoveListener: OnPointerMoveListener? = null) {
        if (corners.size != 4) {
            throw IllegalArgumentException("setCorners: corner list must have 4 items. Current corners params has ${corners.size} items.")
        } else {
            this.corners = corners
            if (showPointers) {
                corners.forEachIndexed { index, pointF ->
                    addCornerPointer(pointF, index)
                }
            }
            invalidate()
        }
    }

    private fun addCornerPointer(center: PointF, index: Int) {
        pointers.add(
            QuadranglePointerView(context, this, index) {

            }.also {
                val size = ceil(it.radius * 2).toInt()
                it.layoutParams = LayoutParams(size, size)
                it.fillColor = this@QuadrangleView.strokeColor
                it.center = center
                addView(it)
            }
        )
    }

    fun clear() {
        this.corners = emptyList()
        clearPointers()
        invalidate()
    }

    private fun clearPointers() {
        pointers.forEach {
            removeView(it)
        }
        pointers.clear()
    }

    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)
        canvas?.apply {
            // Draw quadrangle
            quadranglePath.reset()

            if (corners.isNotEmpty()) {
                quadranglePath.moveTo(corners[0].x, corners[0].y)
                quadranglePath.lineTo(corners[1].x, corners[1].y)
                quadranglePath.lineTo(corners[2].x, corners[2].y)
                quadranglePath.lineTo(corners[3].x, corners[3].y)
                quadranglePath.close()
            }

            drawPath(quadranglePath, fillPaint)
            drawPath(quadranglePath, strokePaint)
        }
    }
}