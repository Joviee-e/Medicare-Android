package com.example.medicare

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

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
        if (view is TextView) {
            val currentTextColor = view.currentTextColor
            val hexColor = String.format("#%06X", 0xFFFFFF and currentTextColor).lowercase()
            
            // Map specific text colors to high contrast counterparts while keeping semantic red intact
            if (hexColor == "#3f484c" || hexColor == "#8e9192" || hexColor == "#50686d") {
                // Secondary / Neutral text -> make pure black
                view.setTextColor(Color.BLACK)
            } else if (hexColor == "#004d60" || hexColor == "#00677f" || hexColor == "#003745") {
                // Primary brand teal -> make extremely dark high-contrast teal
                view.setTextColor(Color.parseColor("#001e26"))
                view.setTypeface(view.typeface, Typeface.BOLD)
            }
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyHighContrastToView(view.getChildAt(i))
            }
        }
    }
}
