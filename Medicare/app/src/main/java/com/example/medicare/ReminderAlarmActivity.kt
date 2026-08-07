package com.example.medicare

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ReminderAlarmActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder_alarm)

        // Wire "Mark as Taken" button
        findViewById<Button>(R.id.btn_mark_taken)?.setOnClickListener {
            Toast.makeText(this, "Medicine marked as taken", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Wire "Snooze" button
        findViewById<Button>(R.id.btn_snooze)?.setOnClickListener {
            Toast.makeText(this, "Reminder snoozed", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Wire "Skip Dose" button
        findViewById<Button>(R.id.btn_skip_dose)?.setOnClickListener {
            Toast.makeText(this, "Dose skipped", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
