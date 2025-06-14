package com.veducation.app

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var ivTogglePassword: ImageView
    private lateinit var btnLogin: Button
    private lateinit var sessionManager: SessionManager
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force portrait orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        setContentView(R.layout.activity_login)

        sessionManager = SessionManager(this)

        // Initialize views
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        ivTogglePassword = findViewById(R.id.ivTogglePassword)
        btnLogin = findViewById(R.id.btnLogin)

        // Set up password visibility toggle
        ivTogglePassword.setOnClickListener {
            togglePasswordVisibility()
        }

        // Set up button click listeners
        btnLogin.setOnClickListener {
            loginUser()
        }

        findViewById<TextView>(R.id.tvCreateAccount).setOnClickListener {
            startActivity(Intent(this, CreateAccountActivity::class.java))
        }

        findViewById<TextView>(R.id.tvContinueWithoutAccount).setOnClickListener {
            showToast("Continuar sem conta - funcionalidade a implementar")
        }

        findViewById<TextView>(R.id.tvForgotPassword).setOnClickListener {
            showToast("Recuperar password - funcionalidade a implementar")
        }
    }

    private fun loginUser() {
        val email = etUsername.text.toString().trim()
        val password = etPassword.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            showToast("Por favor, preencha todos os campos")
            return
        }

        // Disable button during login
        btnLogin.isEnabled = false
        btnLogin.text = "A fazer login..."

        lifecycleScope.launch {
            val result = SupabaseClient.signIn(email, password)

            result.onSuccess { signInResponse ->
                // Save login session with both tokens
                sessionManager.createLoginSession(
                    signInResponse.email,
                    signInResponse.name,
                    signInResponse.accessToken,
                    signInResponse.refreshToken,
                    3600 // 1 hour
                )

                showToast("Login successful!")

                // Navigate to MainActivity after successful login
                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }.onFailure { error ->
                showToast("Erro no login: ${error.message}")
            }

            btnLogin.isEnabled = true
            btnLogin.text = "Login"
        }
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        if (isPasswordVisible) {
            etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            ivTogglePassword.alpha = 1.0f
        } else {
            etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            ivTogglePassword.alpha = 0.5f
        }
        etPassword.setSelection(etPassword.text.length)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
