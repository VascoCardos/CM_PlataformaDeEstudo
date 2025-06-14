package com.veducation.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class PreStartActivity : AppCompatActivity() {
    
    private lateinit var sessionManager: SessionManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pre_start)
        
        sessionManager = SessionManager(this)
        
        // Check if user is already logged in
        Handler(Looper.getMainLooper()).postDelayed({
            if (sessionManager.isLoggedIn()) {
                // User is logged in, restore session and go to MainActivity
                val accessToken = sessionManager.getAccessToken()
                if (accessToken != null) {
                    SupabaseClient.setAccessToken(accessToken)
                }
                
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                // User not logged in, go to StartActivity
                startActivity(Intent(this, StartActivity::class.java))
                finish()
            }
        }, 500)
    }
}
