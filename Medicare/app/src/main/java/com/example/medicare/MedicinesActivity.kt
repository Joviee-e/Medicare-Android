package com.example.medicare

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MedicinesActivity : AppCompatActivity() {

    private val masterList = listOf(
        MedicineItem("Atorvastatin", "20mg • Tablet", "Next: 8:00 PM (Once Daily)", "Teal", "Next"),
        MedicineItem("Lisinopril", "10mg • Tablet", "Missed: 8:00 AM", "Red", "Missed"),
        MedicineItem("Flonase", "50mcg • Nasal Spray", "Taken today", "Grey", "Taken")
    )

    private val displayedList = mutableListOf<MedicineItem>()
    private lateinit var adapter: MedicineAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medicines)

        // Setup custom bottom navigation
        NavigationHelper.setupNavigation(this, R.id.tab_medicines)

        // Setup RecyclerView for Medicines
        val recyclerMedicines = findViewById<RecyclerView>(R.id.recycler_medicines)
        recyclerMedicines.layoutManager = LinearLayoutManager(this)

        displayedList.addAll(masterList)
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
            filterMedicines("All")
        }

        chipMorning?.setOnClickListener {
            selectChip(chipMorning, allChips)
            filterMedicines("Morning")
        }

        chipEvening?.setOnClickListener {
            selectChip(chipEvening, allChips)
            filterMedicines("Evening")
        }

        chipAsNeeded?.setOnClickListener {
            selectChip(chipAsNeeded, allChips)
            filterMedicines("As Needed")
        }

        // Wire up Floating Action Button to start AddMedicineActivity
        findViewById<FloatingActionButton>(R.id.fab_add_medicine)?.setOnClickListener {
            val intent = Intent(this, AddMedicineActivity::class.java)
            startActivity(intent)
        }
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

    private fun filterMedicines(filter: String) {
        displayedList.clear()
        when (filter) {
            "All" -> displayedList.addAll(masterList)
            "Morning" -> {
                displayedList.add(masterList[1]) // Lisinopril
            }
            "Evening" -> {
                displayedList.add(masterList[0]) // Atorvastatin
            }
            "As Needed" -> {
                displayedList.add(masterList[2]) // Flonase
            }
        }
        adapter.notifyDataSetChanged()
    }
}
