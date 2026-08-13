package com.example.medicare

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Setup custom bottom navigation
        NavigationHelper.setupNavigation(this, R.id.tab_profile)

        // Menu drawer trigger
        findViewById<ImageView>(R.id.btn_menu)?.setOnClickListener {
            Toast.makeText(this, "Menu drawer coming soon", Toast.LENGTH_SHORT).show()
        }

        // Notification Bell trigger
        findViewById<ImageView>(R.id.btn_notification)?.setOnClickListener {
            Toast.makeText(this, "Notifications coming soon", Toast.LENGTH_SHORT).show()
        }

        // Edit Profile Pencil trigger
        findViewById<ImageView>(R.id.btn_edit_profile)?.setOnClickListener {
            Toast.makeText(this, "Edit profile coming soon", Toast.LENGTH_SHORT).show()
        }

        // Profile Picture container click trigger
        findViewById<View>(R.id.photo_container)?.setOnClickListener {
            Toast.makeText(this, "Change profile picture coming soon", Toast.LENGTH_SHORT).show()
        }

        // Emergency Call Now button trigger
        findViewById<Button>(R.id.btn_call_emergency)?.setOnClickListener {
            Toast.makeText(this, "Emergency calling feature coming soon", Toast.LENGTH_SHORT).show()
        }

        // Reminder Sounds click row
        findViewById<View>(R.id.row_reminder_sounds)?.setOnClickListener {
            Toast.makeText(this, "Reminder sound settings coming soon", Toast.LENGTH_SHORT).show()
        }

        // Sync to Cloud click row
        findViewById<View>(R.id.row_sync_cloud)?.setOnClickListener {
            Toast.makeText(this, "Cloud sync coming soon", Toast.LENGTH_SHORT).show()
        }

        // Export Health Data click row
        findViewById<View>(R.id.row_export_data)?.setOnClickListener {
            Toast.makeText(this, "Export feature coming soon", Toast.LENGTH_SHORT).show()
        }

        // Sign Out button trigger -> confirmation dialog
        findViewById<Button>(R.id.btn_sign_out)?.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Sign Out") { _, _ ->
                    val intent = Intent(this, HomeActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                    finish()
                }
                .show()
        }
    }
}
