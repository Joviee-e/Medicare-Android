package com.example.medicare

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.example.medicare.api.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var txtName: TextView
    private lateinit var txtEmail: TextView
    private lateinit var txtPhone: TextView
    private lateinit var txtBlood: TextView
    private lateinit var txtEmergName: TextView
    private lateinit var txtEmergPhone: TextView

    private lateinit var switchContrast: SwitchCompat
    private lateinit var switchVoice: SwitchCompat
    private lateinit var switchHaptic: SwitchCompat
    private lateinit var seekbarFont: SeekBar

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

        // Emergency call button
        findViewById<Button>(R.id.btn_call_emergency)?.setOnClickListener {
            val phoneNum = txtEmergPhone.text.toString().trim()
            if (phoneNum.isNotEmpty() && phoneNum != "Not Specified") {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNum"))
                startActivity(intent)
            } else {
                Toast.makeText(this, "No emergency number specified", Toast.LENGTH_SHORT).show()
            }
        }

        // Profile pic overlay
        findViewById<View>(R.id.photo_container)?.setOnClickListener {
            Toast.makeText(this, "Photo uploads coming soon", Toast.LENGTH_SHORT).show()
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
                        txtName.text = profile.name
                        txtEmail.text = sessionManager.getUserEmail() ?: "No email"
                        txtPhone.text = profile.emergencyContactPhone ?: "(Not Specified)" // Maps emergency phone or custom phone
                        txtBlood.text = profile.bloodGroup ?: "O+"
                        txtEmergName.text = profile.emergencyContactName ?: "Not Specified"
                        txtEmergPhone.text = profile.emergencyContactPhone ?: "Not Specified"

                        // Sync sessionManager name
                        sessionManager.saveUserName(profile.name)

                        // Accessibility Bindings
                        profile.accessibilitySettings?.let { settings ->
                            switchContrast.isChecked = settings.contrastMode
                            switchVoice.isChecked = settings.voiceInput
                            switchHaptic.isChecked = settings.hapticFeedback
                            seekbarFont.progress = settings.fontSize
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
        val editBlood = dialogView.findViewById<EditText>(R.id.edit_blood)
        val editEmergName = dialogView.findViewById<EditText>(R.id.edit_emerg_name)
        val editEmergPhone = dialogView.findViewById<EditText>(R.id.edit_emerg_phone)

        // Pre-fill values
        editName.setText(txtName.text)
        editBlood.setText(txtBlood.text)
        editEmergName.setText(txtEmergName.text)
        editEmergPhone.setText(txtEmergPhone.text)

        AlertDialog.Builder(this)
            .setTitle("Edit Profile")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val newName = editName.text.toString().trim()
                val newBlood = editBlood.text.toString().trim()
                val newEmergName = editEmergName.text.toString().trim()
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

                val request = UpdateProfileRequest(
                    name = newName,
                    bloodGroup = newBlood,
                    emergencyContactName = newEmergName,
                    emergencyContactPhone = newEmergPhone,
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
        val syncListener = CompoundButton.OnCheckedChangeListener { _, _ -> syncAccessibility() }
        switchContrast.setOnCheckedChangeListener(syncListener)
        switchVoice.setOnCheckedChangeListener(syncListener)
        switchHaptic.setOnCheckedChangeListener(syncListener)

        seekbarFont.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                syncAccessibility()
            }
        })
    }

    private fun syncAccessibility() {
        val settings = AccessibilitySettings(
            contrastMode = switchContrast.isChecked,
            voiceInput = switchVoice.isChecked,
            hapticFeedback = switchHaptic.isChecked,
            fontSize = seekbarFont.progress
        )

        val request = UpdateProfileRequest(
            name = txtName.text.toString(),
            bloodGroup = txtBlood.text.toString(),
            emergencyContactName = txtEmergName.text.toString(),
            emergencyContactPhone = txtEmergPhone.text.toString(),
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
