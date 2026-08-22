package com.example.medicare

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medicare.api.BaseResponse
import com.example.medicare.api.MedicineRequest
import com.example.medicare.api.MedicineResponse
import com.example.medicare.api.GetMedicinesResponse
import com.example.medicare.api.ApiMedicine
import com.example.medicare.api.RetrofitClient
import com.google.android.material.card.MaterialCardView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddMedicineActivity : BaseActivity() {

    private val remindersList = mutableListOf<String>()
    private lateinit var reminderAdapter: ReminderAdapter
    
    private var selectedType = "tablet"
    private var selectedFrequency = "daily"
    private var medId: String? = null // Null if creating, set if editing

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_medicine)

        sessionManager = SessionManager(this)

        val inputName = findViewById<EditText>(R.id.input_med_name)
        val inputDosage = findViewById<EditText>(R.id.input_dosage_value)
        val txtStartDate = findViewById<TextView>(R.id.txt_start_date)
        val txtEndDate = findViewById<TextView>(R.id.txt_end_date)

        // Setup Reminders RecyclerView
        val recyclerReminders = findViewById<RecyclerView>(R.id.recycler_reminders)
        recyclerReminders.layoutManager = LinearLayoutManager(this)
        reminderAdapter = ReminderAdapter(remindersList) { position ->
            showTimePicker(position)
        }
        recyclerReminders.adapter = reminderAdapter

        // Pre-set default dates (today)
        val todayStr = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        txtStartDate.text = todayStr
        txtEndDate.text = todayStr

        // Extract extras if editing
        medId = intent.getStringExtra("med_id")
        val editName = intent.getStringExtra("med_name")
        val editDose = intent.getStringExtra("med_dose")
        
        if (medId != null) {
            findViewById<TextView>(R.id.txt_title)?.text = "Edit Medicine"
            if (editName != null) {
                inputName.setText(editName)
            }
            if (editDose != null) {
                // Strip out non-numeric characters for dosage input
                val numericDose = editDose.takeWhile { it.isDigit() }
                inputDosage.setText(numericDose.ifEmpty { editDose })
            }
            fetchMedicineDetails(medId!!)
        }

        // Setup Medicine Type Card Single-Selects
        findViewById<MaterialCardView>(R.id.card_type_tablet)?.setOnClickListener {
            selectedType = "tablet"
            updateTypeSelection(R.id.card_type_tablet)
        }
        findViewById<MaterialCardView>(R.id.card_type_capsule)?.setOnClickListener {
            selectedType = "capsule"
            updateTypeSelection(R.id.card_type_capsule)
        }
        findViewById<MaterialCardView>(R.id.card_type_syrup)?.setOnClickListener {
            selectedType = "syrup"
            updateTypeSelection(R.id.card_type_syrup)
        }
        findViewById<MaterialCardView>(R.id.card_type_injection)?.setOnClickListener {
            selectedType = "injection"
            updateTypeSelection(R.id.card_type_injection)
        }

        // Setup Frequency Chip Single-Selects
        findViewById<TextView>(R.id.chip_freq_daily)?.setOnClickListener {
            selectedFrequency = "daily"
            updateFrequencySelection(R.id.chip_freq_daily)
        }
        findViewById<TextView>(R.id.chip_freq_weekly)?.setOnClickListener {
            selectedFrequency = "weekly"
            updateFrequencySelection(R.id.chip_freq_weekly)
        }
        findViewById<TextView>(R.id.chip_freq_as_needed)?.setOnClickListener {
            selectedFrequency = "as_needed"
            updateFrequencySelection(R.id.chip_freq_as_needed)
        }

        // Setup Start/End Date Picker Dialogs
        val layoutStartDate = findViewById<LinearLayout>(R.id.layout_start_date)
        layoutStartDate?.setOnClickListener {
            showDatePicker(txtStartDate)
        }

        val layoutEndDate = findViewById<LinearLayout>(R.id.layout_end_date)
        layoutEndDate?.setOnClickListener {
            showDatePicker(txtEndDate)
        }

        // Setup Add Reminder Time
        findViewById<TextView>(R.id.btn_add_time)?.setOnClickListener {
            showTimePicker()
        }

        // Default initial reminder times if empty
        if (remindersList.isEmpty()) {
            remindersList.addAll(listOf("08:00 AM", "08:00 PM"))
            reminderAdapter.notifyDataSetChanged()
        }

        // Save Button Action
        findViewById<Button>(R.id.btn_save_medicine)?.setOnClickListener {
            saveMedicineData(inputName.text.toString().trim(), inputDosage.text.toString().trim(), txtStartDate.text.toString(), txtEndDate.text.toString())
        }

        // Back Button
        findViewById<ImageView>(R.id.btn_back)?.setOnClickListener {
            finish()
        }
    }

    private lateinit var sessionManager: SessionManager

    private fun saveMedicineData(name: String, dosageVal: String, startD: String, endD: String) {
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a medicine name", Toast.LENGTH_SHORT).show()
            return
        }

        val finalDosage = if (dosageVal.isNotEmpty()) "$dosageVal mg" else "1 unit"

        val request = MedicineRequest(
            name = name,
            type = selectedType,
            dosage = finalDosage,
            frequency = selectedFrequency,
            startDate = startD,
            endDate = endD,
            reminderTimes = remindersList
        )

        val apiService = RetrofitClient.getApiService(this)
        
        if (medId != null) {
            // Edit mode
            apiService.updateMedicine(medId!!, request)
                .enqueue(object : Callback<BaseResponse> {
                    override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            val apiMed = ApiMedicine(
                                id = medId!!,
                                patientId = "",
                                name = request.name,
                                type = request.type,
                                dosage = request.dosage,
                                frequency = request.frequency,
                                startDate = request.startDate,
                                endDate = request.endDate,
                                reminderTimes = request.reminderTimes,
                                logs = emptyList()
                            )
                            AlarmScheduler.scheduleAlarms(this@AddMedicineActivity, apiMed)

                            Toast.makeText(this@AddMedicineActivity, "Medicine updated successfully", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            val errMsg = RetrofitClient.parseErrorMessage(response)
                            Toast.makeText(this@AddMedicineActivity, errMsg, Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                        Toast.makeText(this@AddMedicineActivity, "Network error updating medicine", Toast.LENGTH_SHORT).show()
                    }
                })
        } else {
            // Create mode
            apiService.createMedicine(request)
                .enqueue(object : Callback<MedicineResponse> {
                    override fun onResponse(call: Call<MedicineResponse>, response: Response<MedicineResponse>) {
                        val body = response.body()
                        if (response.isSuccessful && body != null && body.success) {
                            val createdMedId = body.medicineId ?: ""
                            if (createdMedId.isNotEmpty()) {
                                val apiMed = ApiMedicine(
                                    id = createdMedId,
                                    patientId = "",
                                    name = request.name,
                                    type = request.type,
                                    dosage = request.dosage,
                                    frequency = request.frequency,
                                    startDate = request.startDate,
                                    endDate = request.endDate,
                                    reminderTimes = request.reminderTimes,
                                    logs = emptyList()
                                )
                                AlarmScheduler.scheduleAlarms(this@AddMedicineActivity, apiMed)
                            }
                            Toast.makeText(this@AddMedicineActivity, "Medicine saved successfully", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            val errMsg = RetrofitClient.parseErrorMessage(response)
                            Toast.makeText(this@AddMedicineActivity, errMsg, Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<MedicineResponse>, t: Throwable) {
                        Toast.makeText(this@AddMedicineActivity, "Network error saving medicine", Toast.LENGTH_SHORT).show()
                    }
                })
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

    private fun showTimePicker(editPosition: Int? = null) {
        val calendar = Calendar.getInstance()
        var initialHour = calendar.get(Calendar.HOUR_OF_DAY)
        var initialMinute = calendar.get(Calendar.MINUTE)

        if (editPosition != null && editPosition in remindersList.indices) {
            val currentTimeStr = remindersList[editPosition]
            try {
                val format = SimpleDateFormat("hh:mm a", Locale.US)
                val date = format.parse(currentTimeStr)
                if (date != null) {
                    val tempCal = Calendar.getInstance().apply { time = date }
                    initialHour = tempCal.get(Calendar.HOUR_OF_DAY)
                    initialMinute = tempCal.get(Calendar.MINUTE)
                }
            } catch (e: Exception) {
                // Fallback to current time
            }
        }

        val picker = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val amPm = if (hourOfDay < 12) "AM" else "PM"
                val hour = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
                val formattedTime = String.format(Locale.US, "%02d:%02d %s", hour, minute, amPm)
                if (editPosition != null && editPosition in remindersList.indices) {
                    remindersList[editPosition] = formattedTime
                    reminderAdapter.notifyItemChanged(editPosition)
                } else {
                    remindersList.add(formattedTime)
                    reminderAdapter.notifyItemInserted(remindersList.size - 1)
                }
            },
            initialHour,
            initialMinute,
            false
        )
        picker.show()
    }

    private fun fetchMedicineDetails(medicineId: String) {
        val cached = MedicineCache.getMedicine(medicineId)
        if (cached != null) {
            populateFields(cached)
            return
        }

        val apiService = RetrofitClient.getApiService(this)
        apiService.getMedicines().enqueue(object : Callback<GetMedicinesResponse> {
            override fun onResponse(call: Call<GetMedicinesResponse>, response: Response<GetMedicinesResponse>) {
                val body = response.body()
                if (response.isSuccessful && body != null && body.success) {
                    val apiMed = body.medicines.find { it.id == medicineId }
                    if (apiMed != null) {
                        MedicineCache.updateCache(this@AddMedicineActivity, body.medicines)
                        populateFields(apiMed)
                    }
                }
            }

            override fun onFailure(call: Call<GetMedicinesResponse>, t: Throwable) {
                Toast.makeText(this@AddMedicineActivity, "Error loading medicine details", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun populateFields(apiMed: ApiMedicine) {
        findViewById<EditText>(R.id.input_med_name).setText(apiMed.name)
        
        // Strip out " mg" or other units for numeric value
        val numericDose = apiMed.dosage.takeWhile { it.isDigit() }
        findViewById<EditText>(R.id.input_dosage_value).setText(numericDose.ifEmpty { apiMed.dosage })
        
        // Set Type card selection
        selectedType = apiMed.type
        val typeCardId = when (selectedType) {
            "tablet" -> R.id.card_type_tablet
            "capsule" -> R.id.card_type_capsule
            "syrup" -> R.id.card_type_syrup
            "injection" -> R.id.card_type_injection
            else -> R.id.card_type_tablet
        }
        updateTypeSelection(typeCardId)

        // Set Frequency chip selection
        selectedFrequency = apiMed.frequency
        val freqChipId = when (selectedFrequency) {
            "daily" -> R.id.chip_freq_daily
            "weekly" -> R.id.chip_freq_weekly
            "as_needed" -> R.id.chip_freq_as_needed
            else -> R.id.chip_freq_daily
        }
        updateFrequencySelection(freqChipId)

        // Set Dates
        findViewById<TextView>(R.id.txt_start_date).text = apiMed.startDate
        findViewById<TextView>(R.id.txt_end_date).text = apiMed.endDate

        // Set Reminder Times
        remindersList.clear()
        remindersList.addAll(apiMed.reminderTimes)
        reminderAdapter.notifyDataSetChanged()
    }
}
