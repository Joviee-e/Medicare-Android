package com.example.medicare

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Setup custom bottom navigation
        NavigationHelper.setupNavigation(this, R.id.tab_home)

        // Setup schedule RecyclerView
        val recyclerSchedule = findViewById<RecyclerView>(R.id.recycler_schedule)
        recyclerSchedule.layoutManager = LinearLayoutManager(this)

        val scheduleData = listOf(
            ScheduleItem("Lisinopril", "10mg • After breakfast", "08:00 AM", "Taken"),
            ScheduleItem("Insulin", "15 units • Before dinner", "06:00 PM", "Upcoming"),
            ScheduleItem("Atorvastatin", "20mg • Before bed", "10:00 PM", "Missed")
        )
        recyclerSchedule.adapter = ScheduleAdapter(scheduleData)

        // Take Now button -> ReminderAlarmActivity
        findViewById<Button>(R.id.btn_take_now)?.setOnClickListener {
            val intent = Intent(this, ReminderAlarmActivity::class.java)
            startActivity(intent)
        }

        // Upcoming Medicine Card -> ReminderAlarmActivity
        findViewById<MaterialCardView>(R.id.card_upcoming)?.setOnClickListener {
            val intent = Intent(this, ReminderAlarmActivity::class.java)
            startActivity(intent)
        }

        // Notification Bell -> "Notifications coming soon" Toast
        findViewById<ImageView>(R.id.btn_notification)?.setOnClickListener {
            Toast.makeText(this, "Notifications coming soon", Toast.LENGTH_SHORT).show()
        }

        // View All -> MedicinesActivity
        findViewById<TextView>(R.id.btn_view_all)?.setOnClickListener {
            val intent = Intent(this, MedicinesActivity::class.java)
            startActivity(intent)
        }
    }
}