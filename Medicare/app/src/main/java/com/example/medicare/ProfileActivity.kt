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

                        // Accessibility Bindings
                        isBindingData = true
                        profile.accessibilitySettings?.let { settings ->
                            switchContrast.isChecked = settings.contrastMode
                            switchVoice.isChecked = settings.voiceInput
                            switchHaptic.isChecked = settings.hapticFeedback
                            seekbarFont.progress = settings.fontSize
                        }
                        isBindingData = false
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

        // Pre-fill values from cache
        cachedProfile?.let { profile ->
            editName.setText(profile.name)
            editPhone.setText(profile.phone ?: "")
            editDob.setText(profile.dateOfBirth ?: "")
            editAge.setText(profile.age ?: "")
            editGender.setText(profile.gender ?: "")
            editBlood.setText(profile.bloodGroup ?: "")
            editAddress.setText(profile.address ?: "")
            editAllergies.setText(profile.medicalInformation?.allergies ?: "")
            editConditions.setText(profile.medicalInformation?.conditions ?: "")
            editMedications.setText(profile.medicalInformation?.medications ?: "")

            val contact = profile.emergencyContacts?.firstOrNull()
            editEmergName.setText(contact?.name ?: profile.emergencyContactName ?: "")
            editEmergRel.setText(contact?.relationship ?: "Family")
            editEmergPhone.setText(contact?.phone ?: profile.emergencyContactPhone ?: "")
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

        AlertDialog.Builder(this)
            .setTitle("Edit Profile")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
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
                    Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val settings = AccessibilitySettings(
                    contrastMode = switchContrast.isChecked,
                    voiceInput = switchVoice.isChecked,
                    hapticFeedback = switchHaptic.isChecked,
                    fontSize = seekbarFont.progress
                )

                val contacts = mutableListOf<EmergencyContact>()
                if (newEmergName.isNotEmpty() || newEmergPhone.isNotEmpty()) {
                    contacts.add(EmergencyContact(newEmergName, newEmergRel, newEmergPhone))
                }

                // If user updates skipped sections, the onboarding state updates to COMPLETED if fully filled,
                // or remains COMPLETED/SKIPPED depending on current settings. If onboarding status was SKIPPED,
                // keep it or promote to COMPLETED if completion percentage becomes 100%. To be safe, keep existing.
                val existingStatus = cachedProfile?.onboardingStatus ?: "COMPLETED"

                val request = UpdateProfileRequest(
                    name = newName,
                    bloodGroup = newBlood,
                    emergencyContacts = contacts,
                    dateOfBirth = newDob,
                    age = newAge,
                    gender = newGender,
                    phone = newPhone,
                    address = newAddress,
                    medicalInformation = MedicalInformation(newAllergies, newConditions, newMedications),
                    onboardingStatus = existingStatus,
                    accessibilitySettings = settings
                )

                RetrofitClient.getApiService(this).updateProfile(request)
                    .enqueue(object : Callback<BaseResponse> {
                        override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                            if (response.isSuccessful && response.body()?.success == true) {
                                Toast.makeText(this@ProfileActivity, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                                loadProfileData()
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
            .show()
    }

    private fun setupAccessibilitySync() {
        switchContrast.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != sessionManager.isContrastMode()) {
                sessionManager.setContrastMode(isChecked)
                syncAccessibility()
                recreate()
            }
        }
        
        switchVoice.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != sessionManager.isVoiceRemindersEnabled()) {
                sessionManager.setVoiceRemindersEnabled(isChecked)
                syncAccessibility()
            }
        }
        
        switchHaptic.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != sessionManager.isHapticFeedbackEnabled()) {
                sessionManager.setHapticFeedbackEnabled(isChecked)
                syncAccessibility()
            }
        }

        seekbarFont.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val progress = seekBar?.progress ?: 2
                if (progress != sessionManager.getFontSize()) {
                    sessionManager.setFontSize(progress)
                    syncAccessibility()
                    recreate()
                }
            }
        })
    }

    private fun syncAccessibility() {
        val profile = cachedProfile ?: return
        val settings = AccessibilitySettings(
            contrastMode = switchContrast.isChecked,
            voiceInput = switchVoice.isChecked,
            hapticFeedback = switchHaptic.isChecked,
            fontSize = seekbarFont.progress
        )

        // Make sure local preferences are saved to handle loaded data from backend on first fetch
        sessionManager.setContrastMode(settings.contrastMode)
        sessionManager.setVoiceRemindersEnabled(settings.voiceInput)
        sessionManager.setHapticFeedbackEnabled(settings.hapticFeedback)
        sessionManager.setFontSize(settings.fontSize)

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
                profile.medicalInformation?.allergies ?: "",
                profile.medicalInformation?.conditions ?: "",
                profile.medicalInformation?.medications ?: ""
            ),
            onboardingStatus = profile.onboardingStatus ?: "COMPLETED",
            accessibilitySettings = settings
        )

        RetrofitClient.getApiService(this).updateProfile(request)
            .enqueue(object : Callback<BaseResponse> {
                override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {}
                override fun onFailure(call: Call<BaseResponse>, t: Throwable) {}
            })
    }

    private fun logoutUser() {
        RetrofitClient.getApiService(this).logout()
            .enqueue(object : Callback<BaseResponse> {
                override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {}
                override fun onFailure(call: Call<BaseResponse>, t: Throwable) {}
            })

        sessionManager.logout()
        Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
