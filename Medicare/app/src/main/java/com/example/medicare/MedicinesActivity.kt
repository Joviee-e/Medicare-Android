package com.example.medicare

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medicare.api.ApiMedicine
import com.example.medicare.api.GetMedicinesResponse
import com.example.medicare.api.RetrofitClient
import com.google.android.material.floatingactionbutton.FloatingActionButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MedicinesActivity : BaseActivity() {

    private val masterList = mutableListOf<MedicineItem>()
    private val displayedList = mutableListOf<MedicineItem>()
    private lateinit var adapter: MedicineAdapter
    private var currentFilter = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medicines)

        // Setup custom bottom navigation
        NavigationHelper.setupNavigation(this, R.id.tab_medicines)

        // Setup RecyclerView
        val recyclerMedicines = findViewById<RecyclerView>(R.id.recycler_medicines)
        recyclerMedicines.layoutManager = LinearLayoutManager(this)

        adapter = MedicineAdapter(displayedList)
        recyclerMedicines.adapter = adapter

        // Setup chips
        val chipAll = findViewById<TextView>(R.id.chip_all)
        val chipMorning = findViewById<TextView>(R.id.chip_morning)
        val chipEvening = findViewById<TextView>(R.id.chip_evening)
        val chipAsNeeded = findViewById<TextView>(R.id.chip_as_needed)

        val allChips = listOf(chipAll, chipMorning, chipEvening, chipAsNeeded)

        chipAll?.setOnClickListener {
            selectChip(chipAll, allChips)
            currentFilter = "All"
            applyFilter()
        }

        chipMorning?.setOnClickListener {
            selectChip(chipMorning, allChips)
            currentFilter = "Morning"
            applyFilter()
        }

        chipEvening?.setOnClickListener {
            selectChip(chipEvening, allChips)
            currentFilter = "Evening"
            applyFilter()
        }

        chipAsNeeded?.setOnClickListener {
            selectChip(chipAsNeeded, allChips)
            currentFilter = "As Needed"
            applyFilter()
        }

        // Floating Action Button
        findViewById<FloatingActionButton>(R.id.fab_add_medicine)?.setOnClickListener {
            val intent = Intent(this, AddMedicineActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadMedicines()
    }

    private fun loadMedicines() {
        RetrofitClient.getApiService(this).getMedicines()
            .enqueue(object : Callback<GetMedicinesResponse> {
                override fun onResponse(call: Call<GetMedicinesResponse>, response: Response<GetMedicinesResponse>) {
                    val body = response.body()
                    if (response.isSuccessful && body != null && body.success) {
                        masterList.clear()
                        for (apiMed in body.medicines) {
                            masterList.add(mapToMedicineItem(apiMed))
                        }
                        applyFilter()
                    } else {
                        val errMsg = RetrofitClient.parseErrorMessage(response)
                        Toast.makeText(this@MedicinesActivity, errMsg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<GetMedicinesResponse>, t: Throwable) {
                    Toast.makeText(this@MedicinesActivity, "Failed to load medicines from cloud", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun mapToMedicineItem(apiMed: ApiMedicine): MedicineItem {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        // Find if there is a log for today
        val todayLog = apiMed.logs.find { it.date == todayStr }
        
        val statusType: String
        val statusColor: String
        val infoText: String
        
        if (todayLog != null) {
            statusType = todayLog.status.replaceFirstChar { it.uppercase() }
            statusColor = when (todayLog.status) {
                "taken" -> "Grey"
                "skipped", "snoozed" -> "Teal"
                "missed" -> "Red"
                else -> "Teal"
            }
            infoText = "${statusType} today"
        } else {
            statusType = "Next"
            statusColor = "Teal"
            val times = apiMed.reminderTimes
            val timeStr = if (times.isNotEmpty()) times[0] else "8:00 AM"
            infoText = "Next: $timeStr (${apiMed.frequency.replaceFirstChar { it.uppercase() }})"
        }

        val typeText = apiMed.type.replaceFirstChar { it.uppercase() }
        val doseText = "${apiMed.dosage} • $typeText"

        return MedicineItem(
            id = apiMed.id,
            name = apiMed.name,
            dose = doseText,
            info = infoText,
            statusColor = statusColor,
            statusType = statusType
        )
    }

    private fun selectChip(selectedChip: TextView, allChips: List<TextView>) {
        for (chip in allChips) {
            if (chip == selectedChip) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected)
                chip.setTextColor(resources.getColor(R.color.white, null))
                chip.setTypeface(null, Typeface.BOLD)
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_unselected)
                chip.setTextColor(resources.getColor(R.color.text_primary, null))
                chip.setTypeface(null, Typeface.NORMAL)
            }
        }
    }

    private fun applyFilter() {
        displayedList.clear()
        
        val filtered = when (currentFilter) {
            "Morning" -> masterList.filter { it.info.contains("AM", ignoreCase = true) || it.dose.lowercase().contains("morning") }
            "Evening" -> masterList.filter { it.info.contains("PM", ignoreCase = true) || it.dose.lowercase().contains("evening") }
            "As Needed" -> masterList.filter { it.info.lowercase().contains("as needed") }
            else -> masterList
        }
        
        displayedList.addAll(filtered)
        adapter.notifyDataSetChanged()
    }
}
