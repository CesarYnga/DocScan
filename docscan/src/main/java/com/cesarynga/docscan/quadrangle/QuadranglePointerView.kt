package com.cesarynga.docscan.quadrangle

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

internal typealias OnPointerMoveListener = (pointerView: QuadranglePointerView) -> Unit

internal class QuadranglePointerView(
    context: Context?,
    quadrangleView: QuadrangleView,
    pointerIndex: Int,
    onPointerMoveListener: OnPointerMoveListener
) : View(context) {

    var center = PointF(0f, 0f)
        set(value) {
            field = value
            x = value.x - radius
            y = value.y - radius
        }

    var fillColor: Int = Color.WHITE
        set(value) {
            field = value
            fillPaint.color = value
        }

    var radius: Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics)


    private val fillPaint = Paint().apply {
        color = fillColor
        style = Paint.Style.FILL
    }

    init {
        setOnTouchListener(PointerTouchListener(quadrangleView, onPointerMoveListener))
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawCircle(radius, radius, radius, fillPaint)
    }

    private class PointerTouchListener(
        private val quadrangleView: QuadrangleView,
        private val onPointerMoveListener: OnPointerMoveListener
    ) :
        OnTouchListener {

        val startPointerCenter = PointF()

        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    val center = (v as QuadranglePointerView).center
                    startPointerCenter.x = center.x
                    startPointerCenter.y = center.y
                }
                MotionEvent.ACTION_MOVE -> {
                    val newCenter = PointF(
                        startPointerCenter.x + event.x - (v as QuadranglePointerView).radius,
                        startPointerCenter.y + event.y - v.radius
                    )

                    if (newCenter.x >= quadrangleView.pointerMinPosition.x && newCenter.y >= quadrangleView.pointerMinPosition.y
                        && newCenter.x <= quadrangleView.pointerMaxPosition.x && newCenter.y <= quadrangleView.pointerMaxPosition.y) {
                        val c = v.center
                        c.x = newCenter.x
                        c.y = newCenter.y
                        v.center = c

                        startPointerCenter.x = v.center.x
                        startPointerCenter.y = v.center.y

                        quadrangleView.invalidate()
                    }
                }
                MotionEvent.ACTION_UP -> {
                    onPointerMoveListener((v as QuadranglePointerView))
                }
            }
            return true
        }
    }
}