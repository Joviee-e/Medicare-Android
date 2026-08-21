package com.example.medicare

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.api.BaseResponse
import com.example.medicare.api.LogRequest
import com.example.medicare.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderAlarmActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder_alarm)

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

        txtName.text = medName
        txtDose.text = medDose

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        btnTaken.setOnClickListener {
            logCompliance(medId, todayStr, medTime, "taken", "Medication logged as Taken!")
        }

        btnSnooze.setOnClickListener {
            logCompliance(medId, todayStr, medTime, "snoozed", "Snoozed reminder for 15 minutes")
        }

        btnSkip.setOnClickListener {
            logCompliance(medId, todayStr, medTime, "skipped", "Medication logged as Skipped")
        }
    }

    private fun logCompliance(medId: String, date: String, time: String, status: String, toastMessage: String) {
        if (medId.isEmpty()) {
            Toast.makeText(this, "Error: Invalid medication ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Clean time parameter for the backend logger
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
}
