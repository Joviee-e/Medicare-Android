package com.example.medicare

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import com.example.medicare.api.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class ProfileActivity : BaseActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var txtName: TextView
    private lateinit var txtEmail: TextView
    private lateinit var txtPhone: TextView
    private lateinit var txtBlood: TextView
    private lateinit var txtEmergName: TextView
    private lateinit var txtEmergPhone: TextView

    // New profile completion views
    private lateinit var txtCompletionPercent: TextView
    private lateinit var progressCompletion: ProgressBar

    private lateinit var switchContrast: SwitchCompat
    private lateinit var switchVoice: SwitchCompat
    private lateinit var switchHaptic: SwitchCompat
    private lateinit var seekbarFont: SeekBar

    // Keep memory cache of fetched patient profile details
    private var cachedProfile: PatientProfile? = null
    private var isBindingData = false

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val imgProfile: ImageView = findViewById(R.id.img_profile_picture) ?: return@registerForActivityResult
            val success = ProfileImageManager.saveProfilePicture(this, uri)
            if (success) {
                ProfileImageManager.displayProfilePicture(this, imgProfile)
            } else {
                Toast.makeText(this, "Failed to save profile picture", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        sessionManager = SessionManager(this)

        // Bind layout views
        txtName = findViewById(R.id.txt_user_name)
        txtEmail = findViewById(R.id.txt_user_email)
        txtPhone = findViewById(R.id.txt_user_phone)
        txtBlood = findViewById(R.id.txt_blood_group)
        txtEmergName = findViewById(R.id.txt_emergency_name)
        txtEmergPhone = findViewById(R.id.txt_emergency_phone)

        txtCompletionPercent = findViewById(R.id.txt_completion_percentage)
        progressCompletion = findViewById(R.id.progress_completion)

        switchContrast = findViewById(R.id.switch_contrast)
        switchVoice = findViewById(R.id.switch_voice)
        switchHaptic = findViewById(R.id.switch_haptic)
        seekbarFont = findViewById(R.id.seekbar_font_size)

        // Setup custom bottom navigation
        NavigationHelper.setupNavigation(this, R.id.tab_profile)

        // Fetch profile from backend
        loadProfileData()

        // Edit Profile Trigger -> shows Edit Profile Dialog
        findViewById<ImageView>(R.id.btn_edit_profile)?.setOnClickListener {
            showEditProfileDialog()
        }

        // Setup dynamic listeners for accessibility controls to sync automatically to backend
        setupAccessibilitySync()

        // Display profile picture on startup
        val imgProfile = findViewById<ImageView>(R.id.img_profile_picture)
        if (imgProfile != null) {
            ProfileImageManager.displayProfilePicture(this, imgProfile)
        }

        // Emergency call button with normalized dialing
        findViewById<Button>(R.id.btn_call_emergency)?.setOnClickListener {
            val phoneNum = txtEmergPhone.text.toString().trim()
            if (phoneNum.isNotEmpty() && phoneNum != "Not Specified") {
                val normalizedNum = PhoneNumberUtils.normalizeIndianPhoneNumber(phoneNum)
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$normalizedNum"))
                startActivity(intent)
            } else {
                Toast.makeText(this, "No emergency number specified", Toast.LENGTH_SHORT).show()
            }
        }

        // Profile pic overlay triggers PickVisualMedia
        findViewById<View>(R.id.photo_container)?.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // Reminder sounds switch UI and row click
        val switchSounds = findViewById<SwitchCompat>(R.id.switch_sounds)
        switchSounds?.isChecked = sessionManager.isReminderSoundsEnabled()
        switchSounds?.setOnCheckedChangeListener { _, isChecked ->
            sessionManager.setReminderSoundsEnabled(isChecked)
        }
        findViewById<View>(R.id.row_reminder_sounds)?.setOnClickListener {
            switchSounds?.toggle()
        }

        // Sign Out trigger
        findViewById<Button>(R.id.btn_sign_out)?.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Sign Out") { _, _ ->
                    logoutUser()
                }
                .show()
        }

        findViewById<View>(R.id.row_export_data)?.setOnClickListener {
            exportUserData()
        }
    }

    private fun loadProfileData() {
        RetrofitClient.getApiService(this).getProfile()
            .enqueue(object : Callback<ProfileResponse> {
                override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                    val body = response.body()
                    if (response.isSuccessful && body != null && body.success && body.profile != null) {
                        val profile = body.profile
                        cachedProfile = profile
                        
                        txtName.text = profile.name
                        txtEmail.text = sessionManager.getUserEmail() ?: "No email"
                        
                        // Map primary emergency contact phone or phone field
                        txtPhone.text = if (!profile.phone.isNullOrEmpty()) profile.phone else "(Not Specified)"
                        txtBlood.text = profile.bloodGroup ?: "O+"
                        
                        val primaryContact = profile.emergencyContacts?.firstOrNull()
                        txtEmergName.text = primaryContact?.name ?: profile.emergencyContactName ?: "Not Specified"
                        txtEmergPhone.text = primaryContact?.phone ?: profile.emergencyContactPhone ?: "Not Specified"

                        // Render profile completion percentage from backend source of truth
                        val completion = profile.completionPercentage ?: 0
                        txtCompletionPercent.text = "$completion%"
                        progressCompletion.progress = completion

                        // Sync sessionManager
                        sessionManager.saveUserName(profile.name)
                        sessionManager.saveOnboardingStatus(profile.onboardingStatus ?: "COMPLETED")

                        // Accessibility Bindings (prioritize backend values, fallback to local settings)
                        var needsRecreate = false
                        profile.accessibilitySettings?.let { settings ->
                            if (settings.contrastMode != sessionManager.isContrastMode()) {
                                sessionManager.setContrastMode(settings.contrastMode)
                                needsRecreate = true
                            }
                            if (settings.fontSize != sessionManager.getFontSize()) {
                                sessionManager.setFontSize(settings.fontSize)
                                needsRecreate = true
                            }
                            if (settings.voiceInput != sessionManager.isVoiceRemindersEnabled()) {
                                sessionManager.setVoiceRemindersEnabled(settings.voiceInput)
                            }
                            if (settings.hapticFeedback != sessionManager.isHapticFeedbackEnabled()) {
                                sessionManager.setHapticFeedbackEnabled(settings.hapticFeedback)
                            }
                        }

                        isBindingData = true
                        switchContrast.isChecked = sessionManager.isContrastMode()
                        switchVoice.isChecked = sessionManager.isVoiceRemindersEnabled()
                        switchHaptic.isChecked = sessionManager.isHapticFeedbackEnabled()
                        seekbarFont.progress = sessionManager.getFontSize()
                        isBindingData = false

                        if (needsRecreate) {
                            recreate()
                        }
                    }
                }

                override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                    Toast.makeText(this@ProfileActivity, "Failed to load profile details", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showEditProfileDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null)
        val editName = dialogView.findViewById<EditText>(R.id.edit_name)
        val editPhone = dialogView.findViewById<EditText>(R.id.edit_phone)
        val editDob = dialogView.findViewById<EditText>(R.id.edit_dob)
        val editAge = dialogView.findViewById<EditText>(R.id.edit_age)
        val editGender = dialogView.findViewById<EditText>(R.id.edit_gender)
        val editBlood = dialogView.findViewById<EditText>(R.id.edit_blood)
        val editAddress = dialogView.findViewById<EditText>(R.id.edit_address)
        val editAllergies = dialogView.findViewById<EditText>(R.id.edit_allergies)
        val editConditions = dialogView.findViewById<EditText>(R.id.edit_conditions)
        val editMedications = dialogView.findViewById<EditText>(R.id.edit_medications)
        
        val editEmergName = dialogView.findViewById<EditText>(R.id.edit_emerg_name)
        val editEmergRel = dialogView.findViewById<EditText>(R.id.edit_emerg_relationship)
        val editEmergPhone = dialogView.findViewById<EditText>(R.id.edit_emerg_phone)

        val btnEditPhoneCountry = dialogView.findViewById<TextView>(R.id.btn_edit_phone_country)
        val btnEditEmergPhoneCountry = dialogView.findViewById<TextView>(R.id.btn_edit_emerg_phone_country)

        var selectedUserCountry = PhoneNumberHelper.supportedCountries[0]
        var selectedEmergCountry = PhoneNumberHelper.supportedCountries[0]

        fun getCountryLabel(country: PhoneNumberHelper.Country): String {
            val flag = country.name.split(" ")[0]
            return "$flag ${country.callingCode}"
        }

        btnEditPhoneCountry.text = getCountryLabel(selectedUserCountry)
        btnEditEmergPhoneCountry.text = getCountryLabel(selectedEmergCountry)

        btnEditPhoneCountry.setOnClickListener {
            PhoneNumberHelper.showCountryPickerDialog(this) { country ->
                selectedUserCountry = country
                btnEditPhoneCountry.text = getCountryLabel(country)
                val phone = editPhone.text.toString().trim()
                if (phone.isNotEmpty() && PhoneNumberHelper.isValidNumber(phone, selectedUserCountry.code)) {
                    editPhone.setText(PhoneNumberHelper.formatNationalNumber(phone, selectedUserCountry.code))
                    editPhone.error = null
                }
            }
        }

        btnEditEmergPhoneCountry.setOnClickListener {
            PhoneNumberHelper.showCountryPickerDialog(this) { country ->
                selectedEmergCountry = country
                btnEditEmergPhoneCountry.text = getCountryLabel(country)
                val phone = editEmergPhone.text.toString().trim()
                if (phone.isNotEmpty() && PhoneNumberHelper.isValidNumber(phone, selectedEmergCountry.code)) {
                    editEmergPhone.setText(PhoneNumberHelper.formatNationalNumber(phone, selectedEmergCountry.code))
                    editEmergPhone.error = null
                }
            }
        }

        editPhone.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val phone = editPhone.text.toString().trim()
                if (phone.isNotEmpty()) {
                    if (PhoneNumberHelper.isValidNumber(phone, selectedUserCountry.code)) {
                        editPhone.setText(PhoneNumberHelper.formatNationalNumber(phone, selectedUserCountry.code))
                        editPhone.error = null
                    } else {
                        editPhone.error = "Enter a valid phone number for selected country"
                    }
                }
            }
        }

        editEmergPhone.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val phone = editEmergPhone.text.toString().trim()
                if (phone.isNotEmpty()) {
                    if (PhoneNumberHelper.isValidNumber(phone, selectedEmergCountry.code)) {
                        editEmergPhone.setText(PhoneNumberHelper.formatNationalNumber(phone, selectedEmergCountry.code))
                        editEmergPhone.error = null
                    } else {
                        editEmergPhone.error = "Enter a valid phone number for selected country"
                    }
                }
            }
        }

        // Pre-fill values from cache
        cachedProfile?.let { profile ->
            editName.setText(profile.name)
            editDob.setText(profile.dateOfBirth ?: "")
            editAge.setText(profile.age ?: "")
            editGender.setText(profile.gender ?: "")
            editBlood.setText(profile.bloodGroup ?: "")
            editAddress.setText(profile.address ?: "")
            editAllergies.setText(profile.medicalInformation?.allergies ?: "")
            editConditions.setText(profile.medicalInformation?.conditions ?: "")
            editMedications.setText(profile.medicalInformation?.medications ?: "")

            // Parse User Phone
            editPhone.setText(profile.phoneNational ?: profile.phone ?: "")
            profile.phoneCountryCode?.let { code ->
                selectedUserCountry = PhoneNumberHelper.getCountryByCode(code)
                btnEditPhoneCountry.text = getCountryLabel(selectedUserCountry)
            } ?: run {
                profile.phone?.let { full ->
                    PhoneNumberHelper.parseInternationalNumber(full)?.let { pair ->
                        selectedUserCountry = pair.first
                        btnEditPhoneCountry.text = getCountryLabel(selectedUserCountry)
                        editPhone.setText(pair.second)
                    }
                }
            }

            // Parse Emergency Phone
            val contact = profile.emergencyContacts?.firstOrNull()
            editEmergName.setText(contact?.name ?: profile.emergencyContactName ?: "")
            editEmergRel.setText(contact?.relationship ?: "Family")
            
            val emergPhoneVal = contact?.phone ?: profile.emergencyContactPhone ?: ""
            val emergCountryVal = contact?.countryCode ?: ""
            val emergNationalVal = contact?.phoneNational ?: ""

            editEmergPhone.setText(emergNationalVal.ifEmpty { emergPhoneVal })
            if (emergCountryVal.isNotEmpty()) {
                selectedEmergCountry = PhoneNumberHelper.getCountryByCode(emergCountryVal)
                btnEditEmergPhoneCountry.text = getCountryLabel(selectedEmergCountry)
            } else if (emergPhoneVal.isNotEmpty()) {
                PhoneNumberHelper.parseInternationalNumber(emergPhoneVal)?.let { pair ->
                    selectedEmergCountry = pair.first
                    btnEditEmergPhoneCountry.text = getCountryLabel(selectedEmergCountry)
                    editEmergPhone.setText(pair.second)
                }
            }
        }

        // Set DatePicker for DOB in dialog
        editDob.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val dobStr = String.format("%02d-%02d-%d", selectedDay, selectedMonth + 1, selectedYear)
                editDob.setText(dobStr)
            }, year, month, day).show()
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Edit Profile")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val newName = editName.text.toString().trim()
            val newPhone = editPhone.text.toString().trim()
            val newDob = editDob.text.toString().trim()
            val newAge = editAge.text.toString().trim()
            val newGender = editGender.text.toString().trim()
            val newBlood = editBlood.text.toString().trim()
            val newAddress = editAddress.text.toString().trim()
            val newAllergies = editAllergies.text.toString().trim()
            val newConditions = editConditions.text.toString().trim()
            val newMedications = editMedications.text.toString().trim()
            
            val newEmergName = editEmergName.text.toString().trim()
            val newEmergRel = editEmergRel.text.toString().trim()
            val newEmergPhone = editEmergPhone.text.toString().trim()

            if (newName.isEmpty()) {
                editName.error = "Name cannot be empty"
                return@setOnClickListener
            }

            // User phone validation
            if (newPhone.isNotEmpty() && !PhoneNumberHelper.isValidNumber(newPhone, selectedUserCountry.code)) {
                editPhone.error = "Enter a valid phone number for selected country"
                return@setOnClickListener
            }

            // Emergency contact validation
            if (newEmergPhone.isNotEmpty() && !PhoneNumberHelper.isValidNumber(newEmergPhone, selectedEmergCountry.code)) {
                editEmergPhone.error = "Enter a valid phone number for selected country"
                return@setOnClickListener
            }

            val settings = AccessibilitySettings(
                contrastMode = switchContrast.isChecked,
                voiceInput = switchVoice.isChecked,
                hapticFeedback = switchHaptic.isChecked,
                fontSize = seekbarFont.progress
            )

            val normalizedPhone = if (newPhone.isNotEmpty()) {
                PhoneNumberHelper.getNormalizedNumber(newPhone, selectedUserCountry.code) ?: newPhone
            } else ""
            val phoneNational = newPhone
            val phoneCountryCode = if (newPhone.isNotEmpty()) selectedUserCountry.code else null

            val contacts = mutableListOf<EmergencyContact>()
            if (newEmergName.isNotEmpty() || newEmergPhone.isNotEmpty()) {
                val normalizedEmerg = if (newEmergPhone.isNotEmpty()) {
                    PhoneNumberHelper.getNormalizedNumber(newEmergPhone, selectedEmergCountry.code) ?: newEmergPhone
                } else ""
                contacts.add(EmergencyContact(
                    name = newEmergName,
                    relationship = newEmergRel,
                    phone = normalizedEmerg,
                    countryCode = if (newEmergPhone.isNotEmpty()) selectedEmergCountry.code else null,
                    phoneNational = newEmergPhone
                ))
            }

            val existingStatus = cachedProfile?.onboardingStatus ?: "COMPLETED"

            val request = UpdateProfileRequest(
                name = newName,
                bloodGroup = newBlood,
                emergencyContacts = contacts,
                dateOfBirth = newDob,
                age = newAge,
                gender = newGender,
                phone = normalizedPhone,
                address = newAddress,
                medicalInformation = MedicalInformation(newAllergies, newConditions, newMedications),
                onboardingStatus = existingStatus,
                accessibilitySettings = settings,
                phoneCountryCode = phoneCountryCode,
                phoneNational = phoneNational
            )

            RetrofitClient.getApiService(this@ProfileActivity).updateProfile(request)
                .enqueue(object : Callback<BaseResponse> {
                    override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            Toast.makeText(this@ProfileActivity, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                            loadProfileData()
                            dialog.dismiss()
                        } else {
                            val errMsg = RetrofitClient.parseErrorMessage(response)
                            Toast.makeText(this@ProfileActivity, errMsg, Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                        Toast.makeText(this@ProfileActivity, "Network error updating profile", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    private fun setupAccessibilitySync() {
        switchContrast.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != sessionManager.isContrastMode()) {
                sessionManager.setContrastMode(isChecked)
                syncAccessibilitySettingsToBackend()
                recreate()
            }
        }
        
        switchVoice.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != sessionManager.isVoiceRemindersEnabled()) {
                sessionManager.setVoiceRemindersEnabled(isChecked)
                syncAccessibilitySettingsToBackend()
            }
        }
        
        switchHaptic.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != sessionManager.isHapticFeedbackEnabled()) {
                sessionManager.setHapticFeedbackEnabled(isChecked)
                syncAccessibilitySettingsToBackend()
            }
        }

        seekbarFont.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val progress = seekBar?.progress ?: 2
                if (progress != sessionManager.getFontSize()) {
                    sessionManager.setFontSize(progress)
                    syncAccessibilitySettingsToBackend()
                    recreate()
                }
            }
        })
    }

    private fun syncAccessibilitySettingsToBackend() {
        val profile = cachedProfile ?: return
        val settings = AccessibilitySettings(
            contrastMode = sessionManager.isContrastMode(),
            voiceInput = sessionManager.isVoiceRemindersEnabled(),
            hapticFeedback = sessionManager.isHapticFeedbackEnabled(),
            fontSize = sessionManager.getFontSize()
        )
        val request = UpdateProfileRequest(
            name = profile.name,
            bloodGroup = profile.bloodGroup ?: "O+",
            emergencyContacts = profile.emergencyContacts ?: emptyList(),
            dateOfBirth = profile.dateOfBirth ?: "",
            age = profile.age ?: "",
            gender = profile.gender ?: "",
            phone = profile.phone ?: "",
            address = profile.address ?: "",
            medicalInformation = MedicalInformation(
                allergies = profile.medicalInformation?.allergies ?: "",
                conditions = profile.medicalInformation?.conditions ?: "",
                medications = profile.medicalInformation?.medications ?: ""
            ),
            onboardingStatus = profile.onboardingStatus ?: "COMPLETED",
            accessibilitySettings = settings,
            phoneCountryCode = profile.phoneCountryCode,
            phoneNational = profile.phoneNational
        )
        RetrofitClient.getApiService(this).updateProfile(request)
            .enqueue(object : Callback<BaseResponse> {
                override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        cachedProfile = profile.copy(accessibilitySettings = settings)
                    }
                }
                override fun onFailure(call: Call<BaseResponse>, t: Throwable) {}
            })
    }

    private fun exportUserData() {
        val profile = cachedProfile
        if (profile == null) {
            Toast.makeText(this, "Profile data not loaded yet. Please wait.", Toast.LENGTH_SHORT).show()
            return
        }

        val progressDialog = AlertDialog.Builder(this)
            .setMessage("Preparing exportable health data...")
            .setCancelable(false)
            .show()

        RetrofitClient.getApiService(this).getMedicines()
            .enqueue(object : Callback<GetMedicinesResponse> {
                override fun onResponse(call: Call<GetMedicinesResponse>, response: Response<GetMedicinesResponse>) {
                    progressDialog.dismiss()
                    val body = response.body()
                    val medicines = if (response.isSuccessful && body != null) body.medicines else emptyList()
                    try {
                        generateAndShareFile(profile, medicines)
                    } catch (e: Exception) {
                        Toast.makeText(this@ProfileActivity, "Failed to generate report: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<GetMedicinesResponse>, t: Throwable) {
                    progressDialog.dismiss()
                    try {
                        generateAndShareFile(profile, emptyList())
                    } catch (e: Exception) {
                        Toast.makeText(this@ProfileActivity, "Failed to generate report: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            })
    }

    private fun generateAndShareFile(profile: PatientProfile, medicines: List<ApiMedicine>) {
        val dateStr = java.text.SimpleDateFormat("dd-MM-yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        val builder = java.lang.StringBuilder()
        builder.append("=========================================\n")
        builder.append("      MEDICARE+ PATIENT REPORT\n")
        builder.append("=========================================\n")
        builder.append("Generated on: $dateStr\n\n")

        builder.append("PATIENT DETAILS:\n")
        builder.append("----------------\n")
        builder.append("Name: ${profile.name}\n")
        builder.append("Age: ${profile.age ?: "Not Specified"}\n")
        builder.append("Gender: ${profile.gender ?: "Not Specified"}\n")
        builder.append("Blood Group: ${profile.bloodGroup ?: "Not Specified"}\n")
        builder.append("Phone: ${profile.phone ?: "Not Specified"}\n")
        builder.append("Address: ${profile.address ?: "Not Specified"}\n\n")

        builder.append("EMERGENCY CONTACT:\n")
        builder.append("------------------\n")
        val contact = profile.emergencyContacts?.firstOrNull()
        if (contact != null) {
            builder.append("Name: ${contact.name}\n")
            builder.append("Relationship: ${contact.relationship}\n")
            builder.append("Phone: ${contact.phone}\n\n")
        } else {
            builder.append("Name: ${profile.emergencyContactName ?: "Not Specified"}\n")
            builder.append("Phone: ${profile.emergencyContactPhone ?: "Not Specified"}\n\n")
        }

        builder.append("MEDICAL INFORMATION:\n")
        builder.append("--------------------\n")
        builder.append("Allergies: ${profile.medicalInformation?.allergies?.ifEmpty { "None Specified" } ?: "None Specified"}\n")
        builder.append("Chronic Conditions: ${profile.medicalInformation?.conditions?.ifEmpty { "None Specified" } ?: "None Specified"}\n")
        builder.append("Current Medications: ${profile.medicalInformation?.medications?.ifEmpty { "None Specified" } ?: "None Specified"}\n\n")

        builder.append("MEDICATION SCHEDULE:\n")
        builder.append("--------------------\n")
        if (medicines.isNotEmpty()) {
            for ((idx, med) in medicines.withIndex()) {
                builder.append("${idx + 1}. ${med.name} (${med.dosage})\n")
                builder.append("   - Type: ${med.type.replaceFirstChar { it.uppercase() }}\n")
                builder.append("   - Frequency: ${med.frequency.replaceFirstChar { it.uppercase() }}\n")
                builder.append("   - Start Date: ${med.startDate}\n")
                builder.append("   - End Date: ${med.endDate}\n")
                builder.append("   - Scheduled Times: ${med.reminderTimes.joinToString(", ")}\n\n")
            }
        } else {
            builder.append("No active medications found in schedule.\n")
        }
        builder.append("=========================================\n")

        val filename = "medicare_health_report.txt"
        val cacheFile = java.io.File(cacheDir, filename)
        java.io.FileOutputStream(cacheFile).use { fos ->
            fos.write(builder.toString().toByteArray())
        }

        val authority = "${packageName}.fileprovider"
        val contentUri = androidx.core.content.FileProvider.getUriForFile(this, authority, cacheFile)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "MediCare+ Patient Health Report")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val chooser = Intent.createChooser(shareIntent, "Share Health Report")
        val resInfoList = packageManager.queryIntentActivities(chooser, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
        for (resolveInfo in resInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            grantUriPermission(packageName, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(chooser)
        Toast.makeText(this, "Health report generated successfully!", Toast.LENGTH_SHORT).show()
    }

    private fun logoutUser() {
        RetrofitClient.getApiService(this).logout()
            .enqueue(object : Callback<BaseResponse> {
                override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {}
                override fun onFailure(call: Call<BaseResponse>, t: Throwable) {}
            })

        sessionManager.logout()
        MedicineCache.clearMemoryCache()
        Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
