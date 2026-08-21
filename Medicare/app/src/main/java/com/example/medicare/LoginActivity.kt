package com.example.medicare

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.api.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var submitBtn: Button
    private lateinit var progressLoader: ProgressBar

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    performGoogleLogin(idToken)
                } else {
                    Toast.makeText(this, "Google authentication failed: missing token", Toast.LENGTH_LONG).show()
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign-In failed: status code ${e.statusCode}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val emailInput = findViewById<EditText>(R.id.input_login_email)
        val passwordInput = findViewById<EditText>(R.id.input_login_password)
        submitBtn = findViewById(R.id.btn_login_submit)
        val registerLink = findViewById<TextView>(R.id.btn_goto_register)
        val googleBtn = findViewById<View>(R.id.btn_google_signin)
        val forgotPasswordBtn = findViewById<TextView>(R.id.btn_forgot_password)
        progressLoader = findViewById(R.id.progress_login)
        
        val sessionManager = SessionManager(this)

        // Setup Google Sign-In options
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(getString(R.string.default_web_client_id))
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        submitBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            // Form validations
            if (email.isEmpty()) {
                emailInput.error = "Email address is required"
                return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput.error = "Please enter a valid email address"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                passwordInput.error = "Password cannot be blank"
                return@setOnClickListener
            }

            setLoading(true)

            RetrofitClient.getApiService(this).login(LoginRequest(email, password))
                .enqueue(object : Callback<AuthResponse> {
                    override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                        setLoading(false)
                        val body = response.body()
                        if (response.isSuccessful && body != null && body.success) {
                            val status = body.onboardingStatus ?: "NOT_STARTED"
                            sessionManager.saveSession(
                                accessToken = body.accessToken ?: "",
                                refreshToken = body.refreshToken ?: "",
                                userId = body.userId ?: "",
                                role = body.role ?: "patient",
                                email = email,
                                name = body.name ?: "Patient",
                                onboardingStatus = status
                            )
                            Toast.makeText(this@LoginActivity, "Welcome back!", Toast.LENGTH_SHORT).show()
                            routeOnboarding(status)
                        } else {
                            val errMsg = RetrofitClient.parseErrorMessage(response)
                            Toast.makeText(this@LoginActivity, errMsg, Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                        setLoading(false)
                        Toast.makeText(this@LoginActivity, "Network connection error", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        googleBtn.setOnClickListener {
            // Log out from current client state first to force account selector popup
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        }

        forgotPasswordBtn.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

        registerLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun performGoogleLogin(idToken: String) {
        setLoading(true)
        val request = GoogleLoginRequest(idToken = idToken, role = "patient")
        RetrofitClient.getApiService(this).googleLogin(request)
            .enqueue(object : Callback<AuthResponse> {
                override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                    setLoading(false)
                    val body = response.body()
                    if (response.isSuccessful && body != null && body.success) {
                        val sessionManager = SessionManager(this@LoginActivity)
                        val status = body.onboardingStatus ?: "NOT_STARTED"
                        sessionManager.saveSession(
                            accessToken = body.accessToken ?: "",
                            refreshToken = body.refreshToken ?: "",
                            userId = body.userId ?: "",
                            role = body.role ?: "patient",
                            email = "", // Read from payload or omit if security is high
                            name = body.name ?: "Patient",
                            onboardingStatus = status
                        )
                        Toast.makeText(this@LoginActivity, "Google Sign-In Successful!", Toast.LENGTH_SHORT).show()
                        routeOnboarding(status)
                    } else {
                        val errMsg = RetrofitClient.parseErrorMessage(response)
                        Toast.makeText(this@LoginActivity, errMsg, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                    setLoading(false)
                    Toast.makeText(this@LoginActivity, "Google verification network error", Toast.LENGTH_SHORT).show()
                }
            })
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

    private fun routeOnboarding(status: String) {
        val intent = if (status == "NOT_STARTED" || status == "IN_PROGRESS") {
            Intent(this, OnboardingActivity::class.java)
        } else {
            Intent(this, HomeActivity::class.java)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
