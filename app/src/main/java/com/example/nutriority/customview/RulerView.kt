package com.example.nutriority.customview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.Scroller
import com.example.nutriority.R
import kotlin.math.pow
import kotlin.math.roundToInt

class RulerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs) {

    private val scroller: Scroller = Scroller(context)
    private val tickPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val indicatorPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var minValue: Int = 0
    private var maxValue: Int = 100
    private var defaultValue: Int = 50
    private var tickInterval: Float = 40f
    private var tickColor: Int = Color.GRAY
    private var textColor: Int = Color.BLACK
    private var textSize: Float = 40f
    private var indicatorColor: Int = Color.RED
    private var indicatorWidth: Float = 5f
    private var decimalPlaces: Int = 0
    private var majorTickFactor: Int = 5

    private val multiplier: Float
        get() = 10f.pow(decimalPlaces)

    private var currentValue: Int = 0
    private var lastX: Float = 0f

    var onValueChangedListener: ((Float) -> Unit)? = null
    var labelFormatter: ((Float) -> String)? = null

    init {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.RulerView, 0, 0)
            decimalPlaces = typedArray.getInt(R.styleable.RulerView_ruler_decimal_places, 0)
            majorTickFactor = typedArray.getInt(R.styleable.RulerView_ruler_majorTickFactor, 10)
            minValue = (typedArray.getFloat(R.styleable.RulerView_ruler_minValue, 0f) * multiplier).roundToInt()
            maxValue = (typedArray.getFloat(R.styleable.RulerView_ruler_maxValue, 100f) * multiplier).roundToInt()
            defaultValue = (typedArray.getFloat(R.styleable.RulerView_ruler_defaultValue, minValue.toFloat() / multiplier) * multiplier).roundToInt()
            tickInterval = typedArray.getDimension(R.styleable.RulerView_ruler_tickInterval, 40f)
            tickColor = typedArray.getColor(R.styleable.RulerView_ruler_tickColor, Color.GRAY)
            textColor = typedArray.getColor(R.styleable.RulerView_ruler_textColor, Color.BLACK)
            textSize = typedArray.getDimension(R.styleable.RulerView_ruler_textSize, 40f)
            indicatorColor = typedArray.getColor(R.styleable.RulerView_ruler_indicatorColor, Color.RED)
            indicatorWidth = typedArray.getDimension(R.styleable.RulerView_ruler_indicatorWidth, 5f)
            typedArray.recycle()
        }
        currentValue = defaultValue
        setupPaints()
    }

    private fun setupPaints() {
        tickPaint.color = tickColor
        tickPaint.style = Paint.Style.STROKE
        tickPaint.strokeWidth = 2f

        textPaint.color = textColor
        textPaint.textSize = textSize
        textPaint.textAlign = Paint.Align.CENTER

        indicatorPaint.color = indicatorColor
        indicatorPaint.style = Paint.Style.STROKE
        indicatorPaint.strokeWidth = indicatorWidth
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        scrollToValue(defaultValue, false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val rulerCenterY = height / 2f

        val shortTickHeight = 20f
        val longTickHeight = 40f

        val visibleStartValue = minValue + ((scrollX - tickInterval) / tickInterval).toInt()
        val visibleEndValue = minValue + ((scrollX + width + tickInterval) / tickInterval).toInt()

        for (i in visibleStartValue.coerceAtLeast(minValue)..visibleEndValue.coerceAtMost(maxValue)) {
            val x = (i - minValue) * tickInterval
            val isMajorTick = (i % majorTickFactor == 0)

            val tickHeight = if (isMajorTick) longTickHeight else shortTickHeight
            val tickYStart = rulerCenterY - tickHeight / 2
            val tickYEnd = rulerCenterY + tickHeight / 2

            canvas.drawLine(x, tickYStart, x, tickYEnd, tickPaint)

            if (isMajorTick) {
                val value = i.toFloat() / multiplier
                val label = labelFormatter?.invoke(value) ?: String.format("%.${decimalPlaces}f", value)
                canvas.drawText(label, x, tickYEnd + textSize + 10, textPaint)
            }
        }

        val indicatorX = scrollX + width / 2f
        val indicatorYStart = rulerCenterY - longTickHeight / 2 - 10
        val indicatorYEnd = rulerCenterY + longTickHeight / 2 + 10

        canvas.drawLine(indicatorX, indicatorYStart, indicatorX, indicatorYEnd, indicatorPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                if (!scroller.isFinished) {
                    scroller.abortAnimation()
                }
                parent.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (lastX - event.x).toInt()
                scrollBy(dx, 0)
                lastX = event.x
                updateValue()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent.requestDisallowInterceptTouchEvent(false)
                scrollToValue(currentValue, true)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun computeScroll() {
        super.computeScroll()
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.currX, scroller.currY)
            updateValue()
            invalidate()
        }
    }

    private fun updateValue() {
        val centerPosition = scrollX + width / 2f
        val preciseValue = (minValue + centerPosition / tickInterval)
        val coercedPreciseValue = preciseValue.coerceIn(minValue.toFloat(), maxValue.toFloat())

        onValueChangedListener?.invoke(coercedPreciseValue / multiplier)

        currentValue = coercedPreciseValue.roundToInt().coerceIn(minValue, maxValue)
    }

    private fun scrollToValue(value: Int, smooth: Boolean) {
        val targetScrollX = ((value - minValue) * tickInterval - width / 2f).roundToInt()
        val dx = targetScrollX - scrollX

        if (dx == 0) return

        if (smooth) {
            scroller.startScroll(scrollX, 0, dx, 0, 300)
        } else {
            scrollTo(targetScrollX, 0)
        }
        invalidate()
    }

    fun getValue(): Float {
        return currentValue.toFloat() / multiplier
    }

    fun setMinValue(value: Float) {
        minValue = (value * multiplier).roundToInt()
        invalidate()
    }

    fun setMaxValue(value: Float) {
        maxValue = (value * multiplier).roundToInt()
        invalidate()
    }

    fun setCurrentValue(value: Float) {
        val newValue = (value * multiplier).roundToInt().coerceIn(minValue, maxValue)
        if (newValue != currentValue) {
            currentValue = newValue
            scrollToValue(currentValue, false)
        }
    }

    fun setMajorTickFactor(factor: Int) {
        this.majorTickFactor = factor
        invalidate()
    }
}
