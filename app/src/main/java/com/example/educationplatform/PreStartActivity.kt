package com.veducation.app

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class PreStartActivity : AppCompatActivity() {
    
    private lateinit var sessionManager: SessionManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Force portrait orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        
        setContentView(R.layout.activity_pre_start)
        
        sessionManager = SessionManager(this)
        
        // Check if user is already logged in
        Handler(Looper.getMainLooper()).postDelayed({
            if (sessionManager.isLoggedIn()) {
                Log.d("PreStartActivity", "User is logged in, checking tokens...")
                
                // Check if we have valid tokens
                val accessToken = sessionManager.getAccessToken()
                val refreshToken = sessionManager.getRefreshToken()
                val expiresAt = sessionManager.getTokenExpiresAt()
                
                if (accessToken != null && refreshToken != null && expiresAt > 0) {
                    val expiresIn = ((expiresAt - System.currentTimeMillis()) / 1000).toInt()
                    
                    if (expiresIn > 0) {
                        // Tokens are valid, restore them and go to MainActivity
                        SupabaseClient.setTokens(accessToken, refreshToken, expiresIn)
                        Log.d("PreStartActivity", "✅ Tokens restored, going to MainActivity")
                        
                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        // Tokens are expired, clear session and go to login
                        Log.w("PreStartActivity", "⚠️ Tokens expired, clearing session")
                        sessionManager.logout()
                        goToStartActivity()
                    }
                } else {
                    // No valid tokens, go to login
                    Log.w("PreStartActivity", "⚠️ No valid tokens found")
                    sessionManager.logout()
                    goToStartActivity()
                }
            } else {
                // User not logged in, go to StartActivity
                Log.d("PreStartActivity", "User not logged in, going to StartActivity")
                goToStartActivity()
            }
        }, 500)
    }
    
    private fun goToStartActivity() {
        startActivity(Intent(this, StartActivity::class.java))
        finish()
    }
}
