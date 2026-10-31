package com.example.medicare

import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.example.medicare.api.BaseResponse
import com.example.medicare.api.LogRequest
import com.example.medicare.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderAlarmActivity : BaseActivity() {

    private lateinit var sessionManager: SessionManager
    private var tts: TextToSpeech? = null
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder_alarm)

        sessionManager = SessionManager(this)

        // Wake screen up, turn screen on, and display over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val txtName = findViewById<TextView>(R.id.txt_alarm_med_name)
        val txtDose = findViewById<TextView>(R.id.txt_alarm_med_dose)

        val btnTaken = findViewById<Button>(R.id.btn_mark_taken)
        val btnSnooze = findViewById<Button>(R.id.btn_snooze)
        val btnSkip = findViewById<Button>(R.id.btn_skip_dose)

        // Extract parameters passed from click listener
        val medId = intent.getStringExtra("med_id") ?: ""
        val medName = intent.getStringExtra("med_name") ?: "Medication"
        val medDose = intent.getStringExtra("med_dose") ?: "Dose"
        val medTime = intent.getStringExtra("med_time") ?: "08:00 AM"

        // Cancel notification for this reminder if present
        val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
        val notifIndex = intent.getIntExtra("med_index", 0)
        notifManager?.cancel(medId.hashCode() + notifIndex)
        notifManager?.cancel(medId.hashCode() + 999)

        txtName.text = medName
        txtDose.text = medDose

        // Bind medicine icon based on selected type
        val imgIcon = findViewById<android.widget.ImageView>(R.id.img_alarm_med_icon)
        var medType = intent.getStringExtra("med_type")
        if (medType.isNullOrEmpty() && medId.isNotEmpty()) {
            MedicineCache.loadFromPrefs(this)
            medType = MedicineCache.getMedicines().find { it.id == medId }?.type
        }

        val iconRes = when (medType?.lowercase()) {
            "capsule" -> R.drawable.capsules_solid_full
            "syrup" -> R.drawable.droplet_solid_full
            "injection" -> R.drawable.syringe_solid_full
            else -> R.drawable.tablets_solid_full
        }
        imgIcon?.setImageResource(iconRes)

        // 1. Voice Reminders (Text to Speech)
        val isVoiceEnabled = sessionManager.isVoiceRemindersEnabled()
        if (isVoiceEnabled) {
            tts = TextToSpeech(this) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.getDefault()
                    val textToSpeak = "Time to take your medication: $medName, $medDose"
                    tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "MedicationReminderTTS")
                }
            }
        }

        // 2. Reminder Sounds (RingtoneManager play alarm sound)
        val isSoundEnabled = sessionManager.isReminderSoundsEnabled()
        if (isSoundEnabled) {
            try {
                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ringtone = RingtoneManager.getRingtone(applicationContext, alarmUri)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ringtone?.audioAttributes = android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                }
                ringtone?.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. Vibration (Haptic Feedback)
        val isHapticEnabled = sessionManager.isHapticFeedbackEnabled()
        if (isHapticEnabled) {
            try {
                vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vibratorManager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }
                
                val pattern = longArrayOf(0, 500, 1000) // vibrate 500ms, pause 1000ms
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, 0)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        btnTaken.setOnClickListener {
            stopAlerts()
            logCompliance(medId, todayStr, medTime, "taken", "Medication logged as Taken!")
        }

        btnSnooze.setOnClickListener {
            stopAlerts()
            AlarmScheduler.scheduleSnoozeAlarm(
                context = this,
                medicineId = medId,
                name = medName,
                dosage = medDose,
                timeStr = medTime,
                type = medType ?: "tablet",
                snoozeMinutes = 15
            )
            logCompliance(medId, todayStr, medTime, "snoozed", "Snoozed reminder for 15 minutes")
        }

        btnSkip.setOnClickListener {
            stopAlerts()
            logCompliance(medId, todayStr, medTime, "skipped", "Medication logged as Skipped")
        }
    }

    private fun stopAlerts() {
        try {
            ringtone?.stop()
            vibrator?.cancel()
            tts?.stop()
        } catch (e: Exception) {}
    }

    private fun logCompliance(medId: String, date: String, time: String, status: String, toastMessage: String) {
        if (medId.isEmpty()) {
            Toast.makeText(this, "Error: Invalid medication ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val cleanTime = time.substringBefore(" ")

        val request = LogRequest(date = date, time = cleanTime, status = status)
        RetrofitClient.getApiService(this).logCompliance(medId, request)
            .enqueue(object : Callback<BaseResponse> {
                override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@ReminderAlarmActivity, toastMessage, Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        val errMsg = RetrofitClient.parseErrorMessage(response)
                        Toast.makeText(this@ReminderAlarmActivity, errMsg, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }

                override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                    Toast.makeText(this@ReminderAlarmActivity, "Network error logging compliance", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        ringtone?.stop()
        vibrator?.cancel()
        super.onDestroy()
    }
}
