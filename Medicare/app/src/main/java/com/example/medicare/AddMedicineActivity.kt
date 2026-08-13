package com.example.medicare

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import java.util.Calendar

class AddMedicineActivity : AppCompatActivity() {

    private val remindersList = mutableListOf<String>()
    private lateinit var reminderAdapter: ReminderAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_medicine)

        // Setup bottom navigation tab active showcase representation
        NavigationHelper.setupNavigation(this, R.id.tab_medicines)

        val inputName = findViewById<EditText>(R.id.input_med_name)
        val inputDosage = findViewById<EditText>(R.id.input_dosage_value)

        // Setup Reminders RecyclerView
        val recyclerReminders = findViewById<RecyclerView>(R.id.recycler_reminders)
        recyclerReminders.layoutManager = LinearLayoutManager(this)
        remindersList.addAll(listOf("08:00 AM", "08:00 PM"))
        reminderAdapter = ReminderAdapter(remindersList)
        recyclerReminders.adapter = reminderAdapter

        // Pre-fill fields if Extras are passed (from editing logic)
        val editName = intent.getStringExtra("med_name")
        val editDose = intent.getStringExtra("med_dose")
        if (editName != null) {
            inputName.setText(editName)
            findViewById<TextView>(R.id.txt_title)?.text = "Edit Medicine"
        }
        if (editDose != null) {
            // Strip out non-numeric characters for dosage value input
            val numericDose = editDose.takeWhile { it.isDigit() }
            inputDosage.setText(numericDose.ifEmpty { editDose })
        }

        // Setup Medicine Type Card Single-Selects
        findViewById<MaterialCardView>(R.id.card_type_tablet)?.setOnClickListener {
            updateTypeSelection(R.id.card_type_tablet)
        }
        findViewById<MaterialCardView>(R.id.card_type_capsule)?.setOnClickListener {
            updateTypeSelection(R.id.card_type_capsule)
        }
        findViewById<MaterialCardView>(R.id.card_type_syrup)?.setOnClickListener {
            updateTypeSelection(R.id.card_type_syrup)
        }
        findViewById<MaterialCardView>(R.id.card_type_injection)?.setOnClickListener {
            updateTypeSelection(R.id.card_type_injection)
        }

        // Setup Frequency Chip Single-Selects
        findViewById<TextView>(R.id.chip_freq_daily)?.setOnClickListener {
            updateFrequencySelection(R.id.chip_freq_daily)
        }
        findViewById<TextView>(R.id.chip_freq_weekly)?.setOnClickListener {
            updateFrequencySelection(R.id.chip_freq_weekly)
        }
        findViewById<TextView>(R.id.chip_freq_as_needed)?.setOnClickListener {
            updateFrequencySelection(R.id.chip_freq_as_needed)
        }

        // Setup Start/End Date Picker Dialogs
        val layoutStartDate = findViewById<LinearLayout>(R.id.layout_start_date)
        val txtStartDate = findViewById<TextView>(R.id.txt_start_date)
        layoutStartDate?.setOnClickListener {
            showDatePicker(txtStartDate)
        }

        val layoutEndDate = findViewById<LinearLayout>(R.id.layout_end_date)
        val txtEndDate = findViewById<TextView>(R.id.txt_end_date)
        layoutEndDate?.setOnClickListener {
            showDatePicker(txtEndDate)
        }

        // Setup Add Reminder Time
        findViewById<TextView>(R.id.btn_add_time)?.setOnClickListener {
            showTimePicker()
        }

        // Save Button Validation and Toast
        findViewById<Button>(R.id.btn_save_medicine)?.setOnClickListener {
            if (inputName.text.toString().trim().isEmpty()) {
                Toast.makeText(this, "Please enter a medicine name", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Medicine saved successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        // Back Button
        findViewById<ImageView>(R.id.btn_back)?.setOnClickListener {
            finish()
        }
    }

    private fun updateTypeSelection(selectedCardId: Int) {
        val cards = listOf(
            R.id.card_type_tablet,
            R.id.card_type_capsule,
            R.id.card_type_syrup,
            R.id.card_type_injection
        )

        for (id in cards) {
            val card = findViewById<MaterialCardView>(id) ?: continue
            val container = card.getChildAt(0) as? LinearLayout ?: continue
            val iconView = container.getChildAt(0) as? ImageView ?: continue
            val textView = container.getChildAt(1) as? TextView ?: continue

            if (id == selectedCardId) {
                card.setCardBackgroundColor(resources.getColor(R.color.primary, null))
                card.strokeWidth = 0
                iconView.setColorFilter(resources.getColor(R.color.white, null))
                textView.setTextColor(resources.getColor(R.color.white, null))
                textView.setTypeface(null, Typeface.BOLD)
            } else {
                card.setCardBackgroundColor(resources.getColor(R.color.background, null))
                card.strokeWidth = (1.5 * resources.displayMetrics.density).toInt()
                card.strokeColor = Color.parseColor("#BFC8CD")
                iconView.setColorFilter(resources.getColor(R.color.text_secondary, null))
                textView.setTextColor(resources.getColor(R.color.text_secondary, null))
                textView.setTypeface(null, Typeface.NORMAL)
            }
        }
    }

    private fun updateFrequencySelection(selectedChipId: Int) {
        val chips = listOf(R.id.chip_freq_daily, R.id.chip_freq_weekly, R.id.chip_freq_as_needed)
        for (id in chips) {
            val chip = findViewById<TextView>(id) ?: continue
            if (id == selectedChipId) {
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

    private fun showDatePicker(targetTextView: TextView) {
        val calendar = Calendar.getInstance()
        val picker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val formattedDate = String.format("%02d-%02d-%d", dayOfMonth, month + 1, year)
                targetTextView.text = formattedDate
                targetTextView.setTextColor(resources.getColor(R.color.text_primary, null))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        picker.show()
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val picker = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val amPm = if (hourOfDay < 12) "AM" else "PM"
                val hour = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
                val formattedTime = String.format("%02d:%02d %s", hour, minute, amPm)
                remindersList.add(formattedTime)
                reminderAdapter.notifyItemInserted(remindersList.size - 1)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        )
        picker.show()
    }
}
