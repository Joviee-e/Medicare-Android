package com.example.medicare

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.ImageViewCompat
import com.google.android.material.card.MaterialCardView

object AccessibilityHelper {
    
    fun applyFontScale(context: Context): Context {
        val prefs = context.getSharedPreferences("medicare_session", Context.MODE_PRIVATE)
        val progress = prefs.getInt("font_size", 2)
        val scale = when (progress) {
            0 -> 0.85f
            1 -> 0.92f
            2 -> 1.0f
            3 -> 1.15f
            4 -> 1.30f
            else -> 1.0f
        }
        val config = Configuration(context.resources.configuration)
        config.fontScale = scale
        return context.createConfigurationContext(config)
    }

    fun applyHighContrast(activity: Activity) {
        val prefs = activity.getSharedPreferences("medicare_session", Context.MODE_PRIVATE)
        val isHighContrast = prefs.getBoolean("contrast_mode", false)
        if (isHighContrast) {
            val root = activity.findViewById<View>(android.R.id.content)
            if (root != null) {
                applyHighContrastToView(root)
            }
        }
    }

    private fun applyHighContrastToView(view: View) {
        when (view) {
            is MaterialCardView -> {
                // Ensure crisp, distinct card boundaries in high-contrast mode
                val strokeWidthPx = (1.5f * view.context.resources.displayMetrics.density).toInt()
                view.strokeWidth = strokeWidthPx
                view.strokeColor = Color.parseColor("#002D38")
            }
            is EditText -> {
                view.setTextColor(Color.BLACK)
                view.setHintTextColor(Color.parseColor("#334155"))
            }
            is TextView -> {
                val currentTextColor = view.currentTextColor
                val hexColor = String.format("#%06X", 0xFFFFFF and currentTextColor).lowercase()
                
                // Map specific text colors to high contrast counterparts while keeping semantic red intact
                if (hexColor == "#3f484c" || hexColor == "#8e9192" || hexColor == "#50686d" || hexColor == "#757575") {
                    view.setTextColor(Color.BLACK)
                } else if (hexColor == "#004d60" || hexColor == "#00677f" || hexColor == "#003745") {
                    view.setTextColor(Color.parseColor("#001e26"))
                    view.setTypeface(view.typeface, Typeface.BOLD)
                } else if (hexColor == "#ffffff") {
                    // White text on dark buttons/chips: ensure bold for maximum legibility
                    view.setTypeface(view.typeface, Typeface.BOLD)
                }
            }
            is ImageView -> {
                // Enhance icon visibility if tinted with subdued gray
                val tint = ImageViewCompat.getImageTintList(view)
                if (tint != null) {
                    val defaultColor = tint.defaultColor
                    val hexColor = String.format("#%06X", 0xFFFFFF and defaultColor).lowercase()
                    if (hexColor == "#8e9192" || hexColor == "#50686d" || hexColor == "#757575") {
                        ImageViewCompat.setImageTintList(view, ColorStateList.valueOf(Color.parseColor("#1A202C")))
                    }
                }
            }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyHighContrastToView(view.getChildAt(i))
            }
        }
    }
}
