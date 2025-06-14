package com.veducation.app

import android.content.Intent
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

class CreateAccountActivity : AppCompatActivity() {
    
    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var ivTogglePassword: ImageView
    private lateinit var ivToggleConfirmPassword: ImageView
    private lateinit var btnSignUp: Button
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)
        
        // Initialize views
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        ivTogglePassword = findViewById(R.id.ivTogglePassword)
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword)
        btnSignUp = findViewById(R.id.btnSignUp)
        
        // Set up password visibility toggles
        ivTogglePassword.setOnClickListener {
            togglePasswordVisibility(etPassword, ivTogglePassword, isPasswordVisible)
            isPasswordVisible = !isPasswordVisible
        }
        
        ivToggleConfirmPassword.setOnClickListener {
            togglePasswordVisibility(etConfirmPassword, ivToggleConfirmPassword, isConfirmPasswordVisible)
            isConfirmPasswordVisible = !isConfirmPasswordVisible
        }
        
        // Set up button click listeners
        btnSignUp.setOnClickListener {
            signUpUser()
        }
        
        findViewById<TextView>(R.id.tvLogin).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
    
    private fun signUpUser() {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()
        
        // Validations
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showToast("Por favor, preencha todos os campos")
            return
        }
        
        if (password != confirmPassword) {
            showToast("As passwords não coincidem")
            return
        }
        
        if (password.length < 6) {
            showToast("A password deve ter pelo menos 6 caracteres")
            return
        }
        
        // Disable button during signup
        btnSignUp.isEnabled = false
        btnSignUp.text = "A criar conta..."
        
        lifecycleScope.launch {
            val result = SupabaseClient.signUp(email, password, name)
            
            result.onSuccess { message ->
                showToast(message)
                startActivity(Intent(this@CreateAccountActivity, LoginActivity::class.java))
                finish()
            }.onFailure { error ->
                showToast("Erro ao criar conta: ${error.message}")
            }
            
            btnSignUp.isEnabled = true
            btnSignUp.text = "Sign Up"
        }
    }
    
    private fun togglePasswordVisibility(editText: EditText, imageView: ImageView, isVisible: Boolean) {
        if (!isVisible) {
            editText.transformationMethod = HideReturnsTransformationMethod.getInstance()
            imageView.alpha = 1.0f
        } else {
            editText.transformationMethod = PasswordTransformationMethod.getInstance()
            imageView.alpha = 0.5f
        }
        editText.setSelection(editText.text.length)
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
