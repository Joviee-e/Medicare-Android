package com.example.medicare

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.api.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class OnboardingActivity : BaseActivity() {

    private var currentStep = 1
    private var existingProfile: PatientProfile? = null
    private lateinit var sessionManager: SessionManager

    // Step views
    private lateinit var stepBasic: LinearLayout
    private lateinit var stepMedical: LinearLayout
    private lateinit var stepEmergency: LinearLayout
    private lateinit var stepAddress: LinearLayout

    // Header views
    private lateinit var progressSteps: ProgressBar
    private lateinit var txtStepIndicator: TextView
    private lateinit var txtStepTitle: TextView
    private lateinit var progressLoader: ProgressBar

    // Step 1 Fields
    private lateinit var inputPhone: EditText
    private lateinit var btnObPhoneCountry: TextView
    private var selectedUserCountry = PhoneNumberHelper.supportedCountries[0]
    private lateinit var inputDob: EditText
    private lateinit var inputAge: EditText
    private lateinit var rgGender: RadioGroup

    // Step 2 Fields
    private var selectedBloodGroup: String = "O+"
    private val bloodChipIds = listOf(
        R.id.chip_blood_o_pos, R.id.chip_blood_o_neg,
        R.id.chip_blood_a_pos, R.id.chip_blood_a_neg,
        R.id.chip_blood_b_pos, R.id.chip_blood_b_neg,
        R.id.chip_blood_ab_pos, R.id.chip_blood_ab_neg
    )
    private lateinit var inputAllergies: EditText
    private lateinit var inputConditions: EditText
    private lateinit var inputMedications: EditText

    // Step 3 Fields
    private lateinit var inputEmergName: EditText
    private lateinit var spinnerEmergRel: Spinner
    private lateinit var inputEmergPhone: EditText
    private lateinit var btnObEmergPhoneCountry: TextView
    private var selectedEmergCountry = PhoneNumberHelper.supportedCountries[0]

    // Step 4 Fields
    private lateinit var inputAddress: EditText

    // Buttons
    private lateinit var btnSkip: Button
    private lateinit var btnNext: Button
    private lateinit var btnSkipAll: TextView

    private val bloodGroups = arrayOf("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-")
    private val relationships = arrayOf("Family", "Spouse", "Parent", "Child", "Friend", "Other")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        sessionManager = SessionManager(this)

        // Bind layouts
        stepBasic = findViewById(R.id.step_basic_info)
        stepMedical = findViewById(R.id.step_medical_details)
        stepEmergency = findViewById(R.id.step_emergency_contact)
        stepAddress = findViewById(R.id.step_address_details)

        progressSteps = findViewById(R.id.progress_steps)
        txtStepIndicator = findViewById(R.id.txt_step_indicator)
        txtStepTitle = findViewById(R.id.txt_step_title)
        progressLoader = findViewById(R.id.progress_onboarding)

        inputPhone = findViewById(R.id.input_ob_phone)
        btnObPhoneCountry = findViewById(R.id.btn_ob_phone_country)
        btnObPhoneCountry.text = getCountryLabel(selectedUserCountry)
        btnObPhoneCountry.setOnClickListener {
            PhoneNumberHelper.showCountryPickerDialog(this) { country ->
                selectedUserCountry = country
                btnObPhoneCountry.text = getCountryLabel(country)
                validateUserPhone(showError = false)
            }
        }
        inputPhone.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateUserPhone(showError = true)
        }

        inputDob = findViewById(R.id.input_ob_dob)
        inputAge = findViewById(R.id.input_ob_age)
        rgGender = findViewById(R.id.rg_ob_gender)

        inputAllergies = findViewById(R.id.input_ob_allergies)
        inputConditions = findViewById(R.id.input_ob_conditions)
        inputMedications = findViewById(R.id.input_ob_medications)

        inputEmergName = findViewById(R.id.input_ob_emerg_name)
        spinnerEmergRel = findViewById(R.id.spinner_ob_emerg_rel)
        inputEmergPhone = findViewById(R.id.input_ob_emerg_phone)
        btnObEmergPhoneCountry = findViewById(R.id.btn_ob_emerg_phone_country)
        btnObEmergPhoneCountry.text = getCountryLabel(selectedEmergCountry)
        btnObEmergPhoneCountry.setOnClickListener {
            PhoneNumberHelper.showCountryPickerDialog(this) { country ->
                selectedEmergCountry = country
                btnObEmergPhoneCountry.text = getCountryLabel(country)
                validateEmergPhone(showError = false)
            }
        }
        inputEmergPhone.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateEmergPhone(showError = true)
        }

        inputAddress = findViewById(R.id.input_ob_address)

        btnSkip = findViewById(R.id.btn_ob_skip)
        btnNext = findViewById(R.id.btn_ob_next)
        btnSkipAll = findViewById(R.id.btn_skip_onboarding)

        // Setup Spinners and Chips
        setupBloodGroupChips()
        spinnerEmergRel.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, relationships)

        // Setup DOB Picker dialog trigger
        inputDob.setOnClickListener {
            showDatePicker()
        }

        // Fetch existing profile to populate current data
        loadExistingProfile()

        // Handle navigation triggers
        btnNext.setOnClickListener {
            saveCurrentStepData(isSkippingStep = false)
        }

        btnSkip.setOnClickListener {
            saveCurrentStepData(isSkippingStep = true)
        }

        btnSkipAll.setOnClickListener {
            // Save state as SKIPPED and route to Home immediately
            saveOnboardingStateAndExit("SKIPPED")
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val picker = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val dobStr = String.format("%02d-%02d-%d", selectedDay, selectedMonth + 1, selectedYear)
            inputDob.setText(dobStr)
            
            // Auto calculate age
            val birthCal = Calendar.getInstance().apply { set(selectedYear, selectedMonth, selectedDay) }
            var calculatedAge = calendar.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)
            if (calendar.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) {
                calculatedAge--
            }
            if (calculatedAge >= 0) {
                inputAge.setText(calculatedAge.toString())
            }
        }, year, month, day)
        picker.show()
    }

    private fun loadExistingProfile() {
        setLoading(true)
        RetrofitClient.getApiService(this).getProfile()
            .enqueue(object : Callback<ProfileResponse> {
                override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                    setLoading(false)
                    if (response.isSuccessful && response.body()?.success == true) {
                        existingProfile = response.body()?.profile
                        prepopulateFields()
                    }
                }

                override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                    setLoading(false)
                    Toast.makeText(this@OnboardingActivity, "Failed to connect to profile cloud service", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun prepopulateFields() {
        existingProfile?.let { profile ->
            inputPhone.setText(profile.phoneNational ?: profile.phone ?: "")
            profile.phoneCountryCode?.let { code ->
                selectedUserCountry = PhoneNumberHelper.getCountryByCode(code)
                btnObPhoneCountry.text = getCountryLabel(selectedUserCountry)
            } ?: run {
                profile.phone?.let { full ->
                    PhoneNumberHelper.parseInternationalNumber(full)?.let { pair ->
                        selectedUserCountry = pair.first
                        btnObPhoneCountry.text = getCountryLabel(selectedUserCountry)
                        inputPhone.setText(pair.second)
                    }
                }
            }
            inputDob.setText(profile.dateOfBirth ?: "")
            inputAge.setText(profile.age ?: "")
            
            when (profile.gender?.lowercase()) {
                "male" -> rgGender.check(R.id.rb_gender_male)
                "female" -> rgGender.check(R.id.rb_gender_female)
                "other" -> rgGender.check(R.id.rb_gender_other)
            }

            selectedBloodGroup = profile.bloodGroup ?: "O+"
            updateBloodGroupSelection()

            inputAllergies.setText(profile.medicalInformation?.allergies ?: "")
            inputConditions.setText(profile.medicalInformation?.conditions ?: "")
            inputMedications.setText(profile.medicalInformation?.medications ?: "")

            val primaryContact = profile.emergencyContacts?.firstOrNull()
            inputEmergName.setText(primaryContact?.name ?: profile.emergencyContactName ?: "")
            val primaryEmergPhone = primaryContact?.phone ?: profile.emergencyContactPhone ?: ""
            val primaryEmergCountry = primaryContact?.countryCode ?: ""
            val primaryEmergNational = primaryContact?.phoneNational ?: ""

            inputEmergPhone.setText(primaryEmergNational.ifEmpty { primaryEmergPhone })
            if (primaryEmergCountry.isNotEmpty()) {
                selectedEmergCountry = PhoneNumberHelper.getCountryByCode(primaryEmergCountry)
                btnObEmergPhoneCountry.text = getCountryLabel(selectedEmergCountry)
            } else if (primaryEmergPhone.isNotEmpty()) {
                PhoneNumberHelper.parseInternationalNumber(primaryEmergPhone)?.let { pair ->
                    selectedEmergCountry = pair.first
                    btnObEmergPhoneCountry.text = getCountryLabel(selectedEmergCountry)
                    inputEmergPhone.setText(pair.second)
                }
            }
            
            val relIdx = relationships.indexOf(primaryContact?.relationship ?: "Family")
            if (relIdx >= 0) spinnerEmergRel.setSelection(relIdx)

            inputAddress.setText(profile.address ?: "")
        }
    }

    private fun saveCurrentStepData(isSkippingStep: Boolean) {
        // Collect local layout parameters
        val name = existingProfile?.name ?: sessionManager.getUserName() ?: "Patient"
        val contrastMode = existingProfile?.accessibilitySettings?.contrastMode ?: false
        val voiceInput = existingProfile?.accessibilitySettings?.voiceInput ?: false
        val hapticFeedback = existingProfile?.accessibilitySettings?.hapticFeedback ?: false
        val fontSize = existingProfile?.accessibilitySettings?.fontSize ?: 2
        
        // Step 1 fields
        var phone = inputPhone.text.toString().trim()
        var dob = inputDob.text.toString().trim()
        var age = inputAge.text.toString().trim()
        var gender = ""
        val checkedGenderId = rgGender.checkedRadioButtonId
        if (checkedGenderId != -1) {
            gender = findViewById<RadioButton>(checkedGenderId).text.toString()
        }

        // Step 2 fields
        var bloodGroup = selectedBloodGroup
        var allergies = inputAllergies.text.toString().trim()
        var conditions = inputConditions.text.toString().trim()
        var medications = inputMedications.text.toString().trim()

        // Step 3 fields
        var emergName = inputEmergName.text.toString().trim()
        var emergRel = spinnerEmergRel.selectedItem.toString()
        var emergPhone = inputEmergPhone.text.toString().trim()

        // Step 4 fields
        var address = inputAddress.text.toString().trim()

        if (!isSkippingStep) {
            // Apply current validation rule triggers
            when (currentStep) {
                1 -> {
                    if (!validateUserPhone(showError = true)) {
                        return
                    }
                    if (dob.isEmpty()) {
                        inputDob.error = "Birth date is required"
                        return
                    }
                    if (age.isEmpty()) {
                        inputAge.error = "Age is required"
                        return
                    }
                }
                3 -> {
                    if (emergName.isEmpty()) {
                        inputEmergName.error = "Contact name is required"
                        return
                    }
                    if (!validateEmergPhone(showError = true)) {
                        return
                    }
                }
                4 -> {
                    if (address.isEmpty()) {
                        inputAddress.error = "Residential address is required"
                        return
                    }
                }
            }
        } else {
            // If skipping, we preserve fields empty or keep whatever was typed
        }

        // Determine onboarding status: if we finish step 4, status becomes COMPLETED
        val nextStatus = if (currentStep == 4 && !isSkippingStep) "COMPLETED" else "IN_PROGRESS"

        // Build list of emergency contacts
        val contactsList = mutableListOf<EmergencyContact>()
        if (emergName.isNotEmpty() || emergPhone.isNotEmpty()) {
            val normalizedEmerg = PhoneNumberHelper.getNormalizedNumber(emergPhone, selectedEmergCountry.code) ?: emergPhone
            contactsList.add(EmergencyContact(
                name = emergName,
                relationship = emergRel,
                phone = normalizedEmerg,
                countryCode = selectedEmergCountry.code,
                phoneNational = emergPhone
            ))
        } else {
            // Copy existing contacts if empty
            existingProfile?.emergencyContacts?.let { contactsList.addAll(it) }
        }

        val settings = AccessibilitySettings(contrastMode, voiceInput, hapticFeedback, fontSize)
        val medical = MedicalInformation(allergies, conditions, medications)

        val normalizedPhone = PhoneNumberHelper.getNormalizedNumber(phone, selectedUserCountry.code) ?: phone
        val phoneNational = phone
        val phoneCountryCode = selectedUserCountry.code

        val request = UpdateProfileRequest(
            name = name,
            bloodGroup = bloodGroup,
            emergencyContacts = contactsList,
            dateOfBirth = dob,
            age = age,
            gender = gender,
            phone = normalizedPhone,
            address = address,
            medicalInformation = medical,
            onboardingStatus = nextStatus,
            accessibilitySettings = settings,
            phoneCountryCode = phoneCountryCode,
            phoneNational = phoneNational
        )

        setLoading(true)
        RetrofitClient.getApiService(this).updateProfile(request)
            .enqueue(object : Callback<BaseResponse> {
                override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                    setLoading(false)
                    if (response.isSuccessful && response.body()?.success == true) {
                        sessionManager.saveOnboardingStatus(nextStatus)
                        
                        if (currentStep == 4) {
                            // Onboarding fully complete!
                            Toast.makeText(this@OnboardingActivity, "Profile onboarding completed successfully!", Toast.LENGTH_SHORT).show()
                            navigateToHome()
                        } else {
                            // Transition steps locally
                            goToNextStep()
                        }
                    } else {
                        val errMsg = RetrofitClient.parseErrorMessage(response)
                        Toast.makeText(this@OnboardingActivity, errMsg, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                    setLoading(false)
                    Toast.makeText(this@OnboardingActivity, "Connection error saving onboarding progress", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun saveOnboardingStateAndExit(status: String) {
        // Fetches profile name and metadata to update onboarding status only
        val name = existingProfile?.name ?: sessionManager.getUserName() ?: "Patient"
        val bloodGroup = existingProfile?.bloodGroup ?: selectedBloodGroup
        val dob = existingProfile?.dateOfBirth ?: ""
        val age = existingProfile?.age ?: ""
        val gender = existingProfile?.gender ?: ""
        val phone = existingProfile?.phone ?: ""
        val address = existingProfile?.address ?: ""
        val contacts = existingProfile?.emergencyContacts ?: emptyList()
        
        val contrastMode = existingProfile?.accessibilitySettings?.contrastMode ?: false
        val voiceInput = existingProfile?.accessibilitySettings?.voiceInput ?: false
        val hapticFeedback = existingProfile?.accessibilitySettings?.hapticFeedback ?: false
        val fontSize = existingProfile?.accessibilitySettings?.fontSize ?: 2
        val settings = AccessibilitySettings(contrastMode, voiceInput, hapticFeedback, fontSize)
        
        val allergies = existingProfile?.medicalInformation?.allergies ?: ""
        val conditions = existingProfile?.medicalInformation?.conditions ?: ""
        val medications = existingProfile?.medicalInformation?.medications ?: ""
        val medical = MedicalInformation(allergies, conditions, medications)

        val request = UpdateProfileRequest(
            name = name,
            bloodGroup = bloodGroup,
            emergencyContacts = contacts,
            dateOfBirth = dob,
            age = age,
            gender = gender,
            phone = phone,
            address = address,
            medicalInformation = medical,
            onboardingStatus = status,
            accessibilitySettings = settings,
            phoneCountryCode = existingProfile?.phoneCountryCode,
            phoneNational = existingProfile?.phoneNational
        )

        setLoading(true)
        RetrofitClient.getApiService(this).updateProfile(request)
            .enqueue(object : Callback<BaseResponse> {
                override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                    setLoading(false)
                    if (response.isSuccessful && response.body()?.success == true) {
                        sessionManager.saveOnboardingStatus(status)
                        navigateToHome()
                    } else {
                        navigateToHome() // Exit even if update fails to keep user moving
                    }
                }

                override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                    setLoading(false)
                    navigateToHome()
                }
            })
    }

    private fun goToNextStep() {
        currentStep++
        
        // Set all GONE first
        stepBasic.visibility = View.GONE
        stepMedical.visibility = View.GONE
        stepEmergency.visibility = View.GONE
        stepAddress.visibility = View.GONE

        when (currentStep) {
            2 -> {
                stepMedical.visibility = View.VISIBLE
                progressSteps.progress = 50
                txtStepIndicator.text = "STEP 2 OF 4"
                txtStepTitle.text = "Medical Details"
                btnSkip.text = "Skip Step"
            }
            3 -> {
                stepEmergency.visibility = View.VISIBLE
                progressSteps.progress = 75
                txtStepIndicator.text = "STEP 3 OF 4"
                txtStepTitle.text = "Emergency Contact"
                btnSkip.text = "Skip Step"
            }
            4 -> {
                stepAddress.visibility = View.VISIBLE
                progressSteps.progress = 100
                txtStepIndicator.text = "STEP 4 OF 4"
                txtStepTitle.text = "Home Address"
                btnSkip.text = "Complete Later"
                btnNext.text = "Finish"
            }
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            btnNext.isEnabled = false
            btnSkip.isEnabled = false
            progressLoader.visibility = View.VISIBLE
        } else {
            btnNext.isEnabled = true
            btnSkip.isEnabled = true
            progressLoader.visibility = View.GONE
        }
    }

    private fun setupBloodGroupChips() {
        for (id in bloodChipIds) {
            val chip = findViewById<TextView>(id) ?: continue
            chip.setOnClickListener {
                selectedBloodGroup = chip.text.toString()
                updateBloodGroupSelection()
            }
        }
        updateBloodGroupSelection()
    }

    private fun updateBloodGroupSelection() {
        for (id in bloodChipIds) {
            val chip = findViewById<TextView>(id) ?: continue
            if (chip.text.toString() == selectedBloodGroup) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected)
                chip.setTextColor(resources.getColor(R.color.white, null))
                chip.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_unselected)
                chip.setTextColor(resources.getColor(R.color.text_primary, null))
                chip.setTypeface(null, android.graphics.Typeface.NORMAL)
            }
        }
    }

    private fun getCountryLabel(country: PhoneNumberHelper.Country): String {
        val flag = country.name.split(" ")[0]
        return "$flag ${country.callingCode}"
    }

    private fun validateUserPhone(showError: Boolean): Boolean {
        val phone = inputPhone.text.toString().trim()
        if (phone.isEmpty()) {
            if (showError) inputPhone.error = "Phone number is required"
            return false
        }
        val isValid = PhoneNumberHelper.isValidNumber(phone, selectedUserCountry.code)
        if (!isValid) {
            if (showError) inputPhone.error = "Enter a valid phone number for selected country"
            return false
        }
        val formatted = PhoneNumberHelper.formatNationalNumber(phone, selectedUserCountry.code)
        if (formatted != phone) {
            inputPhone.setText(formatted)
            inputPhone.setSelection(formatted.length)
        }
        inputPhone.error = null
        return true
    }

    private fun validateEmergPhone(showError: Boolean): Boolean {
        val phone = inputEmergPhone.text.toString().trim()
        if (phone.isEmpty()) {
            if (showError) inputEmergPhone.error = "Emergency phone is required"
            return false
        }
        val isValid = PhoneNumberHelper.isValidNumber(phone, selectedEmergCountry.code)
        if (!isValid) {
            if (showError) inputEmergPhone.error = "Enter a valid phone number for selected country"
            return false
        }
        val formatted = PhoneNumberHelper.formatNationalNumber(phone, selectedEmergCountry.code)
        if (formatted != phone) {
            inputEmergPhone.setText(formatted)
            inputEmergPhone.setSelection(formatted.length)
        }
        inputEmergPhone.error = null
        return true
    }
}
