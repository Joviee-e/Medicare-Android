package com.example.medicare

import android.app.Activity
import android.content.Intent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout

object NavigationHelper {
    fun setupNavigation(activity: Activity, activeTabResId: Int) {
        val tabHome = activity.findViewById<FrameLayout>(R.id.tab_home)
        val tabMedicines = activity.findViewById<FrameLayout>(R.id.tab_medicines)
        val tabPharmacy = activity.findViewById<FrameLayout>(R.id.tab_pharmacy)
        val tabAI = activity.findViewById<FrameLayout>(R.id.tab_ai)
        val tabProfile = activity.findViewById<FrameLayout>(R.id.tab_profile)

        // Handle navigation bar & gesture insets safely
        val bottomNav = activity.findViewById<View>(R.id.bottom_navigation)
        if (bottomNav != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { view, insets ->
                val navInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
                if (navInsets.bottom > 0) {
                    val baseHeight = (72 * activity.resources.displayMetrics.density).toInt()
                    view.layoutParams.height = baseHeight + navInsets.bottom
                    view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, navInsets.bottom)
                    view.requestLayout()
                }
                insets
            }
        }

        val tabHomeActive = activity.findViewById<LinearLayout>(R.id.tab_home_active)
        val tabHomeInactive = activity.findViewById<LinearLayout>(R.id.tab_home_inactive)
        
        val tabMedicinesActive = activity.findViewById<LinearLayout>(R.id.tab_medicines_active)
        val tabMedicinesInactive = activity.findViewById<LinearLayout>(R.id.tab_medicines_inactive)
        
        val tabPharmacyActive = activity.findViewById<LinearLayout>(R.id.tab_pharmacy_active)
        val tabPharmacyInactive = activity.findViewById<LinearLayout>(R.id.tab_pharmacy_inactive)
        
        val tabAIActive = activity.findViewById<LinearLayout>(R.id.tab_ai_active)
        val tabAIInactive = activity.findViewById<LinearLayout>(R.id.tab_ai_inactive)
        
        val tabProfileActive = activity.findViewById<LinearLayout>(R.id.tab_profile_active)
        val tabProfileInactive = activity.findViewById<LinearLayout>(R.id.tab_profile_inactive)

        // Set all to inactive by default
        tabHomeActive?.visibility = View.GONE
        tabHomeInactive?.visibility = View.VISIBLE
        
        tabMedicinesActive?.visibility = View.GONE
        tabMedicinesInactive?.visibility = View.VISIBLE
        
        tabPharmacyActive?.visibility = View.GONE
        tabPharmacyInactive?.visibility = View.VISIBLE
        
        tabAIActive?.visibility = View.GONE
        tabAIInactive?.visibility = View.VISIBLE
        
        tabProfileActive?.visibility = View.GONE
        tabProfileInactive?.visibility = View.VISIBLE

        // Highlight the active tab
        when (activeTabResId) {
            R.id.tab_home -> {
                tabHomeActive?.visibility = View.VISIBLE
                tabHomeInactive?.visibility = View.GONE
            }
            R.id.tab_medicines -> {
                tabMedicinesActive?.visibility = View.VISIBLE
                tabMedicinesInactive?.visibility = View.GONE
            }
            R.id.tab_pharmacy -> {
                tabPharmacyActive?.visibility = View.VISIBLE
                tabPharmacyInactive?.visibility = View.GONE
            }
            R.id.tab_ai -> {
                tabAIActive?.visibility = View.VISIBLE
                tabAIInactive?.visibility = View.GONE
            }
            R.id.tab_profile -> {
                tabProfileActive?.visibility = View.VISIBLE
                tabProfileInactive?.visibility = View.GONE
            }
        }

        // Setup smooth tab switching transitions
        tabHome?.setOnClickListener {
            if (activeTabResId != R.id.tab_home) {
                navigateToTab(activity, HomeActivity::class.java)
            }
        }
        tabMedicines?.setOnClickListener {
            if (activeTabResId != R.id.tab_medicines) {
                navigateToTab(activity, MedicinesActivity::class.java)
            }
        }
        tabPharmacy?.setOnClickListener {
            if (activeTabResId != R.id.tab_pharmacy) {
                navigateToTab(activity, PharmacyActivity::class.java)
            }
        }
        tabAI?.setOnClickListener {
            if (activeTabResId != R.id.tab_ai) {
                navigateToTab(activity, AIAssistantActivity::class.java)
            }
        }
        tabProfile?.setOnClickListener {
            if (activeTabResId != R.id.tab_profile) {
                navigateToTab(activity, ProfileActivity::class.java)
            }
        }
    }

    private fun navigateToTab(activity: Activity, targetClass: Class<*>) {
        val intent = Intent(activity, targetClass)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        activity.startActivity(intent)

        if (android.os.Build.VERSION.SDK_INT >= 34) {
            activity.overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_OPEN,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
        } else {
            @Suppress("DEPRECATION")
            activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // Finish sub-screens so user doesn't get stuck in nested forms
        val isRootTab = activity is HomeActivity ||
                activity is MedicinesActivity ||
                activity is PharmacyActivity ||
                activity is AIAssistantActivity ||
                activity is ProfileActivity

        if (!isRootTab) {
            activity.finish()
        }
    }
}
