package com.example.medicare

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.api.BaseResponse
import com.example.medicare.api.ForgotPasswordRequest
import com.example.medicare.api.ResetPasswordRequest
import com.example.medicare.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ForgotPasswordActivity : BaseActivity() {

    private lateinit var layoutStepRequest: LinearLayout
    private lateinit var layoutStepVerify: LinearLayout
    private lateinit var progressLoader: ProgressBar
    
    private lateinit var emailInput: EditText
    private lateinit var codeInput: EditText
    private lateinit var newPasswordInput: EditText
    private lateinit var confirmNewPasswordInput: EditText
    
    private lateinit var sendCodeBtn: Button
    private lateinit var resetSubmitBtn: Button

    private var cardCodeHint: View? = null
    private var txtCodeBanner: TextView? = null

    private var currentEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        layoutStepRequest = findViewById(R.id.layout_step_request)
        layoutStepVerify = findViewById(R.id.layout_step_verify)
        progressLoader = findViewById(R.id.progress_reset)
        
        emailInput = findViewById(R.id.input_reset_email)
        codeInput = findViewById(R.id.input_verification_code)
        newPasswordInput = findViewById(R.id.input_new_password)
        confirmNewPasswordInput = findViewById(R.id.input_confirm_new_password)
        
        sendCodeBtn = findViewById(R.id.btn_send_code)
        resetSubmitBtn = findViewById(R.id.btn_reset_submit)

        cardCodeHint = findViewById(R.id.card_code_hint)
        txtCodeBanner = findViewById(R.id.txt_code_banner)
        
        val backArrow = findViewById<ImageView>(R.id.btn_back_to_login)
        val gotoLogin = findViewById<TextView>(R.id.btn_goto_login)

        sendCodeBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
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

            setLoading(true)
            RetrofitClient.getApiService(this).forgotPassword(com.example.medicare.api.ForgotPasswordRequest(email))
                .enqueue(object : Callback<com.example.medicare.api.ForgotPasswordResponse> {
                    override fun onResponse(call: Call<com.example.medicare.api.ForgotPasswordResponse>, response: Response<com.example.medicare.api.ForgotPasswordResponse>) {
                        setLoading(false)
                        val body = response.body()
                        if (response.isSuccessful && body != null && body.success) {
                            currentEmail = email
                            val code = body.resetCode ?: "123456"

                            // Transition steps
                            layoutStepRequest.visibility = View.GONE
                            layoutStepVerify.visibility = View.VISIBLE

                            // Populate code banner and pre-fill code input
                            cardCodeHint?.visibility = View.VISIBLE
                            txtCodeBanner?.text = "Verification code: $code (Auto-filled below)"
                            codeInput.setText(code)

                            // Show alert dialog informing user of code
                            com.google.android.material.dialog.MaterialAlertDialogBuilder(this@ForgotPasswordActivity)
                                .setTitle("Reset Verification Code")
                                .setMessage("Your 6-digit verification code is: $code\n\nIt has been automatically entered into the verification field for you.")
                                .setPositiveButton("Continue") { dialog, _ ->
                                    dialog.dismiss()
                                    newPasswordInput.requestFocus()
                                }
                                .show()
                        } else {
                            val errMsg = RetrofitClient.parseErrorMessage(response)
                            Toast.makeText(this@ForgotPasswordActivity, errMsg, Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onFailure(call: Call<com.example.medicare.api.ForgotPasswordResponse>, t: Throwable) {
                        setLoading(false)
                        Toast.makeText(this@ForgotPasswordActivity, "Network error. Try again later.", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        resetSubmitBtn.setOnClickListener {
            val code = codeInput.text.toString().trim()
            val newPassword = newPasswordInput.text.toString().trim()
            val confirmPassword = confirmNewPasswordInput.text.toString().trim()

            if (code.length != 6) {
                codeInput.error = "Enter a valid 6-digit code"
                return@setOnClickListener
            }
            if (newPassword.length < 6) {
                newPasswordInput.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }
            if (newPassword != confirmPassword) {
                confirmNewPasswordInput.error = "Passwords do not match"
                return@setOnClickListener
            }

            setLoading(true)
            val request = ResetPasswordRequest(email = currentEmail, code = code, newPassword = newPassword)
            RetrofitClient.getApiService(this).resetPassword(request)
                .enqueue(object : Callback<BaseResponse> {
                    override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                        setLoading(false)
                        val body = response.body()
                        if (response.isSuccessful && body != null && body.success) {
                            Toast.makeText(this@ForgotPasswordActivity, "Password successfully reset! Please login.", Toast.LENGTH_LONG).show()
                            finish()
                        } else {
                            val errMsg = RetrofitClient.parseErrorMessage(response)
                            Toast.makeText(this@ForgotPasswordActivity, errMsg, Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                        setLoading(false)
                        Toast.makeText(this@ForgotPasswordActivity, "Reset failed due to connection error", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        backArrow.setOnClickListener {
            handleBackStep()
        }

        gotoLogin.setOnClickListener {
            finish()
        }
    }

    private fun handleBackStep() {
        if (layoutStepVerify.visibility == View.VISIBLE) {
            layoutStepVerify.visibility = View.GONE
            layoutStepRequest.visibility = View.VISIBLE
        } else {
            finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (layoutStepVerify.visibility == View.VISIBLE) {
            layoutStepVerify.visibility = View.GONE
            layoutStepRequest.visibility = View.VISIBLE
        } else {
            super.onBackPressed()
        }
    }

    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            sendCodeBtn.isEnabled = false
            resetSubmitBtn.isEnabled = false
            progressLoader.visibility = View.VISIBLE
        } else {
            sendCodeBtn.isEnabled = true
            resetSubmitBtn.isEnabled = true
            progressLoader.visibility = View.GONE
        }
    }
}
