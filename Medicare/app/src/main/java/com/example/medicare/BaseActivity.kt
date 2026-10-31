package com.example.medicare

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

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
        configureSystemBars()
        val prefs = getSharedPreferences("medicare_session", Context.MODE_PRIVATE)
        appliedFontSize = prefs.getInt("font_size", 2)
        appliedContrastMode = prefs.getBoolean("contrast_mode", false)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        applyStatusBarInsets()
        AccessibilityHelper.applyHighContrast(this)
    }

    private fun configureSystemBars() {
        val isNightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isNightMode
            isAppearanceLightNavigationBars = !isNightMode
        }
    }

    private fun applyStatusBarInsets() {
        val spacer = findViewById<View>(R.id.status_bar_spacer) ?: return
        ViewCompat.setOnApplyWindowInsetsListener(spacer) { _, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            if (statusBarHeight > 0 && spacer.layoutParams.height != statusBarHeight) {
                spacer.layoutParams.height = statusBarHeight
                spacer.requestLayout()
            }
            insets
        }
        ViewCompat.requestApplyInsets(spacer)
    }

    override fun onResume() {
        super.onResume()
        configureSystemBars()
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
        applyStatusBarInsets()
        AccessibilityHelper.applyHighContrast(this)
    }
}
