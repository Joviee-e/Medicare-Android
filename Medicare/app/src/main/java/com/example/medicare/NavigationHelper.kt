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

        // Setup transitions
        tabHome?.setOnClickListener {
            if (activeTabResId != R.id.tab_home) {
                val intent = Intent(activity, HomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                activity.startActivity(intent)
                if (activity !is HomeActivity) activity.finish()
            }
        }
        tabMedicines?.setOnClickListener {
            if (activeTabResId != R.id.tab_medicines) {
                val intent = Intent(activity, MedicinesActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                activity.startActivity(intent)
                if (activity !is HomeActivity) activity.finish()
            }
        }
        tabPharmacy?.setOnClickListener {
            if (activeTabResId != R.id.tab_pharmacy) {
                val intent = Intent(activity, PharmacyActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                activity.startActivity(intent)
                if (activity !is HomeActivity) activity.finish()
            }
        }
        tabAI?.setOnClickListener {
            if (activeTabResId != R.id.tab_ai) {
                val intent = Intent(activity, AIAssistantActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                activity.startActivity(intent)
                if (activity !is HomeActivity) activity.finish()
            }
        }
        tabProfile?.setOnClickListener {
            if (activeTabResId != R.id.tab_profile) {
                val intent = Intent(activity, ProfileActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                activity.startActivity(intent)
                if (activity !is HomeActivity) activity.finish()
            }
        }
    }
}
