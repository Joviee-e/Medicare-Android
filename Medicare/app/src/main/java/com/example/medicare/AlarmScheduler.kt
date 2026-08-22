package com.example.medicare

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.medicare.api.ApiMedicine
import java.text.SimpleDateFormat
import java.util.*

object AlarmScheduler {

    private fun getRequestCode(medicineId: String, index: Int): Int {
        return (medicineId.hashCode() * 31 + index) and 0xfffffff
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: true
        } else {
            true
        }
    }

    fun scheduleAlarms(context: Context, medicine: ApiMedicine) {
        if (medicine.frequency == "as_needed") {
            Log.d("AlarmScheduler", "Skipping alarm scheduling for 'As Needed' medicine: ${medicine.name}")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val firstTimeStr = medicine.reminderTimes.firstOrNull() ?: "08:00 AM"
        val timeFormat = if (firstTimeStr.contains("AM", ignoreCase = true) || firstTimeStr.contains("PM", ignoreCase = true)) {
            SimpleDateFormat("hh:mm a", Locale.US)
        } else {
            SimpleDateFormat("HH:mm", Locale.US)
        }

        // Parse Start and End dates
        fun parseDate(dStr: String): Date? {
            for (fmt in listOf("dd-MM-yyyy", "yyyy-MM-dd")) {
                try {
                    return SimpleDateFormat(fmt, Locale.US).parse(dStr)
                } catch (e: Exception) {}
            }
            return null
        }

        val startDate = parseDate(medicine.startDate)
        val endDate = parseDate(medicine.endDate)

        val todayZero = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        // If today is past the end date, do not schedule
        if (endDate != null && todayZero.after(endDate)) {
            Log.d("AlarmScheduler", "Skipping alarm scheduling for ${medicine.name}: end date in past")
            return
        }

        // Cancel existing first to guarantee idempotency
        cancelAlarms(context, medicine.id, medicine.reminderTimes.size)

        for ((index, timeStr) in medicine.reminderTimes.withIndex()) {
            try {
                val parsedTime = timeFormat.parse(timeStr.trim()) ?: continue
                val timeCal = Calendar.getInstance().apply { time = parsedTime }

                val targetCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                // If target time has passed today, move to tomorrow (or next occurrence)
                if (targetCal.before(Calendar.getInstance())) {
                    if (medicine.frequency == "weekly") {
                        targetCal.add(Calendar.DATE, 7)
                    } else {
                        targetCal.add(Calendar.DATE, 1)
                    }
                }

                // If target time is before start date, adjust to start date time
                if (startDate != null && targetCal.time.before(startDate)) {
                    val startCal = Calendar.getInstance().apply { time = startDate }
                    targetCal.set(Calendar.YEAR, startCal.get(Calendar.YEAR))
                    targetCal.set(Calendar.MONTH, startCal.get(Calendar.MONTH))
                    targetCal.set(Calendar.DAY_OF_MONTH, startCal.get(Calendar.DAY_OF_MONTH))
                    
                    // Keep moving forward until it is in the future
                    while (targetCal.before(Calendar.getInstance())) {
                        if (medicine.frequency == "weekly") {
                            targetCal.add(Calendar.DATE, 7)
                        } else {
                            targetCal.add(Calendar.DATE, 1)
                        }
                    }
                }

                // Verify target date is not past end date
                if (endDate != null && targetCal.time.after(endDate)) {
                    Log.d("AlarmScheduler", "Skipping specific alarm for ${medicine.name} at ${targetCal.time}: past end date")
                    continue
                }

                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("med_id", medicine.id)
                    putExtra("med_name", medicine.name)
                    putExtra("med_dose", medicine.dosage)
                    putExtra("med_time", timeStr)
                    putExtra("med_frequency", medicine.frequency)
                    putExtra("med_end_date", medicine.endDate)
                    putExtra("med_index", index)
                }

                val requestCode = getRequestCode(medicine.id, index)
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }

                val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)

                if (canScheduleExactAlarms(context)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            targetCal.timeInMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            targetCal.timeInMillis,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        targetCal.timeInMillis,
                        pendingIntent
                    )
                }
                Log.d("AlarmScheduler", "Scheduled alarm for ${medicine.name} at ${targetCal.time}")
            } catch (e: Exception) {
                Log.e("AlarmScheduler", "Error scheduling alarm index $index for ${medicine.name}", e)
            }
        }
    }

    fun scheduleNextAlarm(
        context: Context,
        medicineId: String,
        name: String,
        dosage: String,
        timeStr: String,
        frequency: String,
        endDateStr: String,
        index: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        
        fun parseDate(dStr: String): Date? {
            for (fmt in listOf("dd-MM-yyyy", "yyyy-MM-dd")) {
                try {
                    return SimpleDateFormat(fmt, Locale.US).parse(dStr)
                } catch (e: Exception) {}
            }
            return null
        }

        val endDate = parseDate(endDateStr)
        val timeFormat = if (timeStr.contains("AM", ignoreCase = true) || timeStr.contains("PM", ignoreCase = true)) {
            SimpleDateFormat("hh:mm a", Locale.US)
        } else {
            SimpleDateFormat("HH:mm", Locale.US)
        }

        try {
            val parsedTime = timeFormat.parse(timeStr.trim()) ?: return
            val timeCal = Calendar.getInstance().apply { time = parsedTime }

            val targetCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Move forward
            if (frequency == "weekly") {
                targetCal.add(Calendar.DATE, 7)
            } else {
                targetCal.add(Calendar.DATE, 1)
            }

            // Verify not past end date
            if (endDate != null && targetCal.time.after(endDate)) {
                Log.d("AlarmScheduler", "Next weekly/daily alarm would exceed end date ($endDateStr) for $name")
                return
            }

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("med_id", medicineId)
                putExtra("med_name", name)
                putExtra("med_dose", dosage)
                putExtra("med_time", timeStr)
                putExtra("med_frequency", frequency)
                putExtra("med_end_date", endDateStr)
                putExtra("med_index", index)
            }

            val requestCode = getRequestCode(medicineId, index)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)

            if (canScheduleExactAlarms(context)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        targetCal.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        targetCal.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    targetCal.timeInMillis,
                    pendingIntent
                )
            }
            Log.d("AlarmScheduler", "Scheduled next alarm for $name at ${targetCal.time}")
        } catch (e: Exception) {
            Log.e("AlarmScheduler", "Error scheduling next alarm for $name", e)
        }
    }

    fun cancelAlarms(context: Context, medicineId: String, reminderTimesCount: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java)

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE
        }

        for (index in 0 until maxOf(reminderTimesCount + 5, 20)) {
            val requestCode = getRequestCode(medicineId, index)
            val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                Log.d("AlarmScheduler", "Cancelled pending alarm for request code: $requestCode")
            }
        }
    }
}
