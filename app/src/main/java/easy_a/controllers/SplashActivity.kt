package com.example.easy_a.controllers

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.os.Handler
import com.example.easy_a.R

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)  // Link to your splash screen XML layout

        // Simulate a delay for the splash screen (2 seconds)
        Handler().postDelayed({
            // Move directly to the MainScreen instead of the LoginScreen
            val intent = Intent(this, MainScreen::class.java)
            startActivity(intent)
            finish()  // Close the SplashActivity so it is not accessible again
        }, 2000)  // 2000 milliseconds = 2 seconds
    }
}
