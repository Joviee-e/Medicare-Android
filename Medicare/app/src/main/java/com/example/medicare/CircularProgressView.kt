package com.example.medicare

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class CircularProgressView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE0E0E0.toInt() // light grey
        style = Paint.Style.STROKE
        strokeWidth = 20f
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF3CB371.toInt() // taken green (#3CB371)
        style = Paint.Style.STROKE
        strokeWidth = 20f
        strokeCap = Paint.Cap.ROUND
    }

    private val rect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = Math.min(width, height).toFloat()
        val stroke = 20f
        val padding = stroke / 2 + 10f
        
        rect.set(padding, padding, size - padding, size - padding)
        
        // Draw background arc
        canvas.drawArc(rect, 0f, 360f, false, bgPaint)
        
        // Draw progress arc (75% for 3/4 Taken)
        // Starts at top (-90 degrees) and goes clockwise 270 degrees
        canvas.drawArc(rect, -90f, 270f, false, progressPaint)
    }
}
