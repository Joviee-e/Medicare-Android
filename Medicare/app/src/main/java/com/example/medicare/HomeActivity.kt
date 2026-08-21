package com.example.medicare

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medicare.api.ApiMedicine
import com.example.medicare.api.GetMedicinesResponse
import com.example.medicare.api.RetrofitClient
import com.google.android.material.card.MaterialCardView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var circularProgress: CircularProgressView
    private lateinit var txtProgressFraction: TextView
    private lateinit var txtGreeting: TextView
    private lateinit var recyclerSchedule: RecyclerView

    private lateinit var cardUpcoming: MaterialCardView
    private lateinit var txtUpcomingName: TextView
    private lateinit var txtUpcomingDesc: TextView
    private lateinit var txtUpcomingTime: TextView
    private lateinit var txtUpcomingCountdown: TextView
    private lateinit var btnTakeNow: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        sessionManager = SessionManager(this)

        // Bind layout views
        circularProgress = findViewById(R.id.circular_progress)
        txtProgressFraction = findViewById(R.id.txt_progress_fraction)
        txtGreeting = findViewById(R.id.txt_greeting)
        recyclerSchedule = findViewById(R.id.recycler_schedule)
        recyclerSchedule.layoutManager = LinearLayoutManager(this)

        cardUpcoming = findViewById(R.id.card_upcoming)
        txtUpcomingName = findViewById(R.id.txt_upcoming_name)
        txtUpcomingDesc = findViewById(R.id.txt_upcoming_desc)
        txtUpcomingTime = findViewById(R.id.txt_upcoming_time)
        txtUpcomingCountdown = findViewById(R.id.txt_upcoming_countdown)
        btnTakeNow = findViewById(R.id.btn_take_now)

        // Setup custom bottom navigation
        NavigationHelper.setupNavigation(this, R.id.tab_home)

        // Set greeting text
        val name = sessionManager.getUserName() ?: "User"
        txtGreeting.text = "Good Morning, $name"

        // Notification Bell Toast trigger
        findViewById<ImageView>(R.id.btn_notification)?.setOnClickListener {
            Toast.makeText(this, "Notifications coming soon", Toast.LENGTH_SHORT).show()
        }

        // View All click -> starts MedicinesActivity
        findViewById<TextView>(R.id.btn_view_all)?.setOnClickListener {
            val intent = Intent(this, MedicinesActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadDailySchedule()
    }

    private fun loadDailySchedule() {
        RetrofitClient.getApiService(this).getMedicines()
            .enqueue(object : Callback<GetMedicinesResponse> {
                override fun onResponse(call: Call<GetMedicinesResponse>, response: Response<GetMedicinesResponse>) {
                    val body = response.body()
                    if (response.isSuccessful && body != null && body.success) {
                        populateSchedule(body.medicines)
                    } else {
                        val errMsg = RetrofitClient.parseErrorMessage(response)
                        Toast.makeText(this@HomeActivity, errMsg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<GetMedicinesResponse>, t: Throwable) {
                    Toast.makeText(this@HomeActivity, "Failed to load schedule from cloud", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun populateSchedule(medicines: List<ApiMedicine>) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val scheduleList = mutableListOf<ScheduleItem>()
        
        var totalDosesToday = 0
        var takenDosesToday = 0

        for (med in medicines) {
            if (isTodayActive(med.startDate, med.endDate)) {
                for (time in med.reminderTimes) {
                    totalDosesToday++
                    
                    // Check if logged as taken for today and specific time
                    val log = med.logs.find { it.date == todayStr && it.time.contains(time.substringBefore(" ")) }
                    val status = if (log != null) {
                        if (log.status == "taken") takenDosesToday++
                        log.status.replaceFirstChar { it.uppercase() }
                    } else {
                        // Evaluate if missed or upcoming based on current time
                        if (isTimePast(time)) "Missed" else "Upcoming"
                    }

                    scheduleList.add(
                        ScheduleItem(
                            id = med.id,
                            name = med.name,
                            dose = med.dosage,
                            time = time,
                            status = status
                        )
                    )
                }
            }
        }

        // Sort schedule by time
        scheduleList.sortBy { parseTimeToMinutes(it.time) }
        recyclerSchedule.adapter = ScheduleAdapter(scheduleList)

        // Update Daily Progress Card
        txtProgressFraction.text = "$takenDosesToday / $totalDosesToday"
        val progressPercent = if (totalDosesToday > 0) takenDosesToday.toFloat() / totalDosesToday.toFloat() else 0f
        circularProgress.setProgress(progressPercent)

        // Find Next Upcoming Dose
        val upcomingDose = scheduleList.find { it.status == "Upcoming" }
        if (upcomingDose != null) {
            cardUpcoming.visibility = View.VISIBLE
            txtUpcomingName.text = upcomingDose.name
            txtUpcomingDesc.text = upcomingDose.dose
            txtUpcomingTime.text = upcomingDose.time
            txtUpcomingCountdown.text = "Upcoming"
            
            val openAlarmAction = View.OnClickListener {
                val intent = Intent(this, ReminderAlarmActivity::class.java).apply {
                    putExtra("med_id", upcomingDose.id)
                    putExtra("med_name", upcomingDose.name)
                    putExtra("med_dose", upcomingDose.dose)
                    putExtra("med_time", upcomingDose.time)
                }
                startActivity(intent)
            }
            
            cardUpcoming.setOnClickListener(openAlarmAction)
            btnTakeNow.setOnClickListener(openAlarmAction)
            btnTakeNow.isEnabled = true
        } else {
            // Hide or set placeholder
            txtUpcomingName.text = "No upcoming doses"
            txtUpcomingDesc.text = "All scheduled doses completed"
            txtUpcomingTime.text = "--:--"
            txtUpcomingCountdown.text = "Completed"
            btnTakeNow.isEnabled = false
        }
    }

    private fun isTodayActive(startDateStr: String, endDateStr: String): Boolean {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        fun parseDate(dStr: String): Date? {
            for (fmt in listOf("dd-MM-yyyy", "yyyy-MM-dd")) {
                try {
                    return SimpleDateFormat(fmt, Locale.getDefault()).parse(dStr)
                } catch (e: Exception) {}
            }
            return null
        }

        val start = parseDate(startDateStr) ?: return false
        val end = parseDate(endDateStr) ?: return false

        return !today.before(start) && !today.after(end)
    }

    private fun isTimePast(timeStr: String): Boolean {
        val now = Calendar.getInstance()
        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val doseMinutes = parseTimeToMinutes(timeStr)
        return nowMinutes > doseMinutes
    }

    private fun parseTimeToMinutes(timeStr: String): Int {
        return try {
            val format = if (timeStr.contains("AM", ignoreCase = true) || timeStr.contains("PM", ignoreCase = true)) {
                SimpleDateFormat("hh:mm a", Locale.getDefault())
            } else {
                SimpleDateFormat("HH:mm", Locale.getDefault())
            }
            val date = format.parse(timeStr.trim()) ?: return 0
            val cal = Calendar.getInstance().apply { time = date }
            cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        } catch (e: Exception) {
            0
        }
    }
}