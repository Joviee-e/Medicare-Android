package com.example.medicare

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.api.AuthResponse
import com.example.medicare.api.RegisterRequest
import com.example.medicare.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : BaseActivity() {

    private lateinit var submitBtn: Button
    private lateinit var progressLoader: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val nameInput = findViewById<EditText>(R.id.input_register_name)
        val emailInput = findViewById<EditText>(R.id.input_register_email)
        val passwordInput = findViewById<EditText>(R.id.input_register_password)
        val confirmPasswordInput = findViewById<EditText>(R.id.input_register_confirm_password)
        val termsCheckbox = findViewById<CheckBox>(R.id.checkbox_terms)
        submitBtn = findViewById(R.id.btn_register_submit)
        val backArrow = findViewById<ImageView>(R.id.btn_back_to_login)
        val loginLink = findViewById<TextView>(R.id.btn_goto_login)
        progressLoader = findViewById(R.id.progress_register)
        
        val sessionManager = SessionManager(this)

        emailInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val input = emailInput.text.toString().trim()
                if (input.isNotEmpty() && !EmailValidator.isValid(input)) {
                    emailInput.error = "Please enter a valid email address (e.g. name@example.com)"
                }
            }
        }

        submitBtn.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            // Form validations
            if (name.isEmpty()) {
                nameInput.error = "Full name is required"
                nameInput.requestFocus()
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                emailInput.error = "Email address is required"
                emailInput.requestFocus()
                Toast.makeText(this, "Email address is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!EmailValidator.isValid(email)) {
                emailInput.error = "Please enter a valid email address (e.g. name@example.com)"
                emailInput.requestFocus()
                Toast.makeText(this, "Please enter a valid email address (e.g. name@example.com)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                passwordInput.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                confirmPasswordInput.error = "Passwords do not match"
                return@setOnClickListener
            }
            if (!termsCheckbox.isChecked) {
                Toast.makeText(this, "Please accept the Terms of Service & Privacy Policy", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            setLoading(true)

            val request = RegisterRequest(email, password, "patient", name)
            RetrofitClient.getApiService(this).register(request)
                .enqueue(object : Callback<AuthResponse> {
                    override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                        setLoading(false)
                        val body = response.body()
                        if (response.isSuccessful && body != null && body.success) {
                            val status = body.onboardingStatus ?: "NOT_STARTED"
                            
                            // Automatically save session and sign-in directly
                            sessionManager.saveSession(
                                accessToken = body.accessToken ?: "",
                                refreshToken = body.refreshToken ?: "",
                                userId = body.userId ?: "",
                                role = body.role ?: "patient",
                                email = email,
                                name = name,
                                onboardingStatus = status
                            )
                            Toast.makeText(this@RegisterActivity, "Registration Successful!", Toast.LENGTH_SHORT).show()
                            
                            // Redirect directly to OnboardingActivity
                            val intent = Intent(this@RegisterActivity, OnboardingActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                            finish()
                        } else {
                            val errMsg = RetrofitClient.parseErrorMessage(response)
                            Toast.makeText(this@RegisterActivity, errMsg, Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                        setLoading(false)
                        Toast.makeText(this@RegisterActivity, "Network connection error", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        backArrow.setOnClickListener {
            finish()
        }

        loginLink.setOnClickListener {
            finish()
        }
    }

    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            submitBtn.isEnabled = false
            progressLoader.visibility = View.VISIBLE
        } else {
            submitBtn.isEnabled = true
            progressLoader.visibility = View.GONE
        }
    }
}
