package com.veducation.app

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class FirstPageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Force portrait orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        
        setContentView(R.layout.activity_first_page)
        
        // Set up button click listeners
        findViewById<Button>(R.id.btnCreateAccount).setOnClickListener {
            startActivity(Intent(this, CreateAccountActivity::class.java))
        }
        
        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        
        findViewById<Button>(R.id.btnContinueWithoutAccount).setOnClickListener {
            // Navigate to main content without login
            // For now, just show a toast message
            android.widget.Toast.makeText(
                this,
                "Continue without account clicked",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
}
