package com.example.medicare

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.api.RegisterRequest
import com.example.medicare.api.AuthResponse
import com.example.medicare.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val nameInput = findViewById<EditText>(R.id.input_register_name)
        val emailInput = findViewById<EditText>(R.id.input_register_email)
        val passwordInput = findViewById<EditText>(R.id.input_register_password)
        val submitBtn = findViewById<Button>(R.id.btn_register_submit)
        val backArrow = findViewById<ImageView>(R.id.btn_back_to_login)
        val loginLink = findViewById<TextView>(R.id.btn_goto_login)

        submitBtn.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            submitBtn.isEnabled = false
            submitBtn.text = "Registering..."

            val request = RegisterRequest(email, password, "patient", name)
            RetrofitClient.getApiService(this).register(request)
                .enqueue(object : Callback<AuthResponse> {
                    override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                        submitBtn.isEnabled = true
                        submitBtn.text = "Sign Up"

                        val body = response.body()
                        if (response.isSuccessful && body != null && body.success) {
                            Toast.makeText(this@RegisterActivity, "Registration successful! Please sign in.", Toast.LENGTH_LONG).show()
                            finish() // Return to LoginActivity
                        } else {
                            val errMsg = RetrofitClient.parseErrorMessage(response)
                            Toast.makeText(this@RegisterActivity, errMsg, Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                        submitBtn.isEnabled = true
                        submitBtn.text = "Sign Up"
                        Toast.makeText(this@RegisterActivity, "Network error during registration", Toast.LENGTH_SHORT).show()
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
}
