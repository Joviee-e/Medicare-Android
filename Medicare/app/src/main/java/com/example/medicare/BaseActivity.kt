package com.example.medicare

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AccessibilityHelper.applyFontScale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        AccessibilityHelper.applyHighContrast(this)
    }

    override fun onResume() {
        super.onResume()
        AccessibilityHelper.applyHighContrast(this)
    }
}
