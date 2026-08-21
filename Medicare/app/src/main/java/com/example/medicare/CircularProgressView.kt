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
    private var progressValue = 0.0f

    fun setProgress(progress: Float) {
        progressValue = progress.coerceIn(0.0f, 1.0f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = Math.min(width, height).toFloat()
        val stroke = 20f
        val padding = stroke / 2 + 10f
        
        rect.set(padding, padding, size - padding, size - padding)
        
        // Draw background arc
        canvas.drawArc(rect, 0f, 360f, false, bgPaint)
        
        // Draw progress arc
        val sweepAngle = progressValue * 360f
        canvas.drawArc(rect, -90f, sweepAngle, false, progressPaint)
    }
}
