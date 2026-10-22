package com.example.medicare

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {

    private var appliedFontSize: Int = -1
    private var appliedContrastMode: Boolean? = null

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("medicare_session", Context.MODE_PRIVATE)
        appliedFontSize = prefs.getInt("font_size", 2)
        super.attachBaseContext(AccessibilityHelper.applyFontScale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("medicare_session", Context.MODE_PRIVATE)
        appliedFontSize = prefs.getInt("font_size", 2)
        appliedContrastMode = prefs.getBoolean("contrast_mode", false)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        AccessibilityHelper.applyHighContrast(this)
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("medicare_session", Context.MODE_PRIVATE)
        val currentFontSize = prefs.getInt("font_size", 2)
        val currentContrastMode = prefs.getBoolean("contrast_mode", false)

        if ((appliedFontSize != -1 && appliedFontSize != currentFontSize) ||
            (appliedContrastMode != null && appliedContrastMode != currentContrastMode)) {
            appliedFontSize = currentFontSize
            appliedContrastMode = currentContrastMode
            recreate()
            return
        }
        AccessibilityHelper.applyHighContrast(this)
    }
}
