
package com.dk26.football

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import kotlin.math.sqrt

class VirtualJoystickView(context: Context) : View(context) {

    var onMove: ((Float, Float) -> Unit)? = null

    private val density = resources.displayMetrics.density
    private val radius =  sixty(68f)
    private val knobRadius = sixty(27f)
    private val margin = sixty(95f)

    private var centerX = 0f
    private var centerY = 0f
    private var knobX = 0f
    private var knobY = 0f
    private var active = false

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(105, 220, 230, 240)
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(190, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = sixty(3f)
    }

    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(210, 35, 110, 245)
        style = Paint.Style.FILL
    }

    private val knobBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = sixty(2f)
    }

    private fun sixty(value: Float): Float = value * density

    private fun updateCenter() {
        centerX = margin + radius
        centerY = height - margin - radius
        if (!active) {
            knobX = centerX
            knobY = centerY
        }
    }

    override fun onSizeChanged(
        width: Int,
        height: Int,
        oldWidth: Int,
        oldHeight: Int
    ) {
        updateCenter()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (width == 0 || height == 0) return

        updateCenter()

        canvas.drawCircle(centerX, centerY, radius, basePaint)
        canvas.drawCircle(centerX, centerY, radius, borderPaint)
        canvas.drawCircle(knobX, knobY, knobRadius, knobPaint)
        canvas.drawCircle(knobX, knobY, knobRadius, knobBorderPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {

            MotionEvent.ACTION_DOWN -> {
                updateCenter()

                val dx = event.x - centerX
                val dy = event.y - centerY
                val distance = sqrt(dx * dx + dy * dy)

                if (distance > radius * 1.65f) {
                    return false
                }

                active = true
                updateJoystick(event.x, event.y)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!active) return false

                updateJoystick(event.x, event.y)
                return true
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                if (!active) return false

                active = false
                knobX = centerX
                knobY = centerY
                onMove?.invoke(0f, 0f)
                invalidate()
                return true
            }
        }

        return active
    }

    private fun updateJoystick(x: Float, y: Float) {
        var dx = x - centerX
        var dy = y - centerY

        val distance = sqrt(dx * dx + dy * dy)

        if (distance > radius) {
            val scale = radius / distance
            dx *= scale
            dy *= scale
        }

        knobX = centerX + dx
        knobY = centerY + dy

        val horizontal = (dx / radius).coerceIn(-1f, 1f)
        val vertical = (-dy / radius).coerceIn(-1f, 1f)

        onMove?.invoke(horizontal, vertical)
        invalidate()
    }
}
