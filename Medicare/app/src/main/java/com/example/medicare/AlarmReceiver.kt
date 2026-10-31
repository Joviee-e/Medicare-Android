package com.example.medicare

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
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
        val medType = intent.getStringExtra("med_type") ?: "tablet"

        Log.d("AlarmReceiver", "Medication alarm triggered: $medName ($medDose)")

        // 1. Acquire WakeLock to turn screen on and keep CPU awake even if app is off/device sleeping
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val wakeLock = powerManager?.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP or
                PowerManager.ON_AFTER_RELEASE,
                "Medicare:AlarmWakeLock"
            )
            wakeLock?.acquire(15000L) // Keep awake for 15s to guarantee activity launch
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Could not acquire WakeLock: ${e.message}")
        }

        // 2. Setup intent to launch ReminderAlarmActivity
        val alarmIntent = Intent(context, ReminderAlarmActivity::class.java).apply {
            putExtra("med_id", medId)
            putExtra("med_name", medName)
            putExtra("med_dose", medDose)
            putExtra("med_time", medTime)
            putExtra("med_type", medType)
            putExtra("med_index", index)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
        }

        // 3. Attempt direct activity launch (allowed when triggered by setAlarmClock / full screen intent)
        try {
            context.startActivity(alarmIntent)
            Log.d("AlarmReceiver", "Direct activity start succeeded for $medName")
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Direct activity start failed (will rely on full-screen intent): ${e.message}")
        }

        val sessionManager = SessionManager(context)
        val isSoundEnabled = sessionManager.isReminderSoundsEnabled()

        // 4. Post high-priority notification with full-screen intent
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "medication_alarms_channel_v2"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            val alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val channel = NotificationChannel(
                channelId,
                "Medication Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent alerts and alarms for medication doses"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 500)
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                if (isSoundEnabled) {
                    setSound(alarmSoundUri, audioAttributes)
                } else {
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
            .setContentTitle("MediCare+ Alarm: $medName")
            .setContentText("Time to take: $medName ($medDose)")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pendingIntent, true) // Pop up over lockscreen / heads-up
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(true)

        if (!isSoundEnabled) {
            notificationBuilder.setSilent(true)
        }

        notificationManager.notify(medId.hashCode() + index, notificationBuilder.build())

        // 5. Schedule next recurrence if not snoozed
        if (index != 999) {
            AlarmScheduler.scheduleNextAlarm(
                context = context,
                medicineId = medId,
                name = medName,
                dosage = medDose,
                timeStr = medTime,
                frequency = frequency,
                endDateStr = endDateStr,
                index = index,
                type = medType
            )
        }
    }
}
