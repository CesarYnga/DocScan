package com.cesarynga.docscan

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.FrameLayout

open class ScannerView(
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

    internal val quadrangleView = QuadrangleView(context)

    var fillColor: Int
    var strokeColor: Int
    var strokeWidth: Float

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.ScannerView,
            defStyleAttr,
            defStyleRes
        ).apply {
            fillColor =
                getColor(R.styleable.ScannerView_fillColor, Color.argb(63, 255, 255, 255))
            strokeColor = getColor(R.styleable.ScannerView_strokeColor, Color.WHITE)
            strokeWidth = getDimensionPixelSize(
                R.styleable.ScannerView_strokeWidth,
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics)
                    .toInt()
            ).toFloat()
        }

        quadrangleView.fillColor = fillColor
        quadrangleView.strokeColor = strokeColor
        quadrangleView.strokeWidth = strokeWidth

        addView(quadrangleView)
    }
}