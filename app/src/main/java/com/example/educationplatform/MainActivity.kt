package com.veducation.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    private lateinit var btnLogout: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize views
        btnLogout = findViewById(R.id.btnLogout)
        
        // Set up logout button
        btnLogout.setOnClickListener {
            logout()
        }
    }
    
    private fun logout() {
        // Clear any stored session data here if needed
        // For now, just navigate back to login
        
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
        
        android.widget.Toast.makeText(this, "Logout realizado com sucesso", android.widget.Toast.LENGTH_SHORT).show()
    }
}
