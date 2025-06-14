package com.veducation.app

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class StartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Force portrait orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        
        setContentView(R.layout.activity_start)
        
        // Delay for 5 seconds and then navigate to FirstPageActivity
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, FirstPageActivity::class.java))
            finish()
        }, 5000)
    }
}
