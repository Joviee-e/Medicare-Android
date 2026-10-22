package com.example.medicare

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medId = intent.getStringExtra("med_id") ?: return
        val medName = intent.getStringExtra("med_name") ?: "Medication"
        val medDose = intent.getStringExtra("med_dose") ?: "Dose"
        val medTime = intent.getStringExtra("med_time") ?: "08:00 AM"
        val frequency = intent.getStringExtra("med_frequency") ?: "daily"
        val endDateStr = intent.getStringExtra("med_end_date") ?: ""
        val index = intent.getIntExtra("med_index", 0)

        Log.d("AlarmReceiver", "Medication alarm triggered: $medName ($medDose)")

        // 1. Setup intent to launch ReminderAlarmActivity
        val alarmIntent = Intent(context, ReminderAlarmActivity::class.java).apply {
            putExtra("med_id", medId)
            putExtra("med_name", medName)
            putExtra("med_dose", medDose)
            putExtra("med_time", medTime)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val sessionManager = SessionManager(context)
        val isSoundEnabled = sessionManager.isReminderSoundsEnabled()

        // 2. Post high-priority notification with full-screen intent
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "medication_reminders_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Medication Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent alerts for medication reminders"
                enableVibration(true)
                if (!isSoundEnabled) {
                    setSound(null, null)
                }
            }
            notificationManager.createNotificationChannel(channel)
        }

        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            medId.hashCode() + index,
            alarmIntent,
            pendingFlags
        )

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_clock)
            .setContentTitle("Medicare Reminder")
            .setContentText("Time to take: $medName ($medDose)")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true) // Launches ReminderAlarmActivity if permitted
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (!isSoundEnabled) {
            notificationBuilder.setSilent(true)
        }

        notificationManager.notify(medId.hashCode() + index, notificationBuilder.build())

        // 3. Schedule next alarm daily/weekly
        AlarmScheduler.scheduleNextAlarm(
            context = context,
            medicineId = medId,
            name = medName,
            dosage = medDose,
            timeStr = medTime,
            frequency = frequency,
            endDateStr = endDateStr,
            index = index
        )
    }
}
