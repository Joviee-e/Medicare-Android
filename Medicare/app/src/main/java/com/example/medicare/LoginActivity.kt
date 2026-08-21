package com.example.medicare

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.api.LoginRequest
import com.example.medicare.api.AuthResponse
import com.example.medicare.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val emailInput = findViewById<EditText>(R.id.input_login_email)
        val passwordInput = findViewById<EditText>(R.id.input_login_password)
        val submitBtn = findViewById<Button>(R.id.btn_login_submit)
        val registerLink = findViewById<TextView>(R.id.btn_goto_register)
        
        val sessionManager = SessionManager(this)

        submitBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            submitBtn.isEnabled = false
            submitBtn.text = "Signing In..."

            RetrofitClient.getApiService(this).login(LoginRequest(email, password))
                .enqueue(object : Callback<AuthResponse> {
                    override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                        submitBtn.isEnabled = true
                        submitBtn.text = "Sign In"
                        
                        val body = response.body()
                        if (response.isSuccessful && body != null && body.success) {
                            sessionManager.saveSession(
                                accessToken = body.accessToken ?: "",
                                refreshToken = body.refreshToken ?: "",
                                userId = body.userId ?: "",
                                role = body.role ?: "patient",
                                email = email,
                                name = body.name ?: "Patient"
                            )
                            Toast.makeText(this@LoginActivity, "Login Successful!", Toast.LENGTH_SHORT).show()
                            
                            val intent = Intent(this@LoginActivity, HomeActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                            finish()
                        } else {
                            val errMsg = RetrofitClient.parseErrorMessage(response)
                            Toast.makeText(this@LoginActivity, errMsg, Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                        submitBtn.isEnabled = true
                        submitBtn.text = "Sign In"
                        Toast.makeText(this@LoginActivity, "Network connection error", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        registerLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}
