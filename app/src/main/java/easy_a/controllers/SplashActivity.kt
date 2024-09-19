package com.example.easy_a.controllers

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.os.Handler
import com.example.easy_a.R
import com.example.easy_a.controllers.MainScreen

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)  // Link to your XML layout

        // Simulate a delay for the splash screen (3 seconds)
        Handler().postDelayed({
            // Move to the MainActivity after the delay
            val intent = Intent(this, MainScreen::class.java)
            startActivity(intent)
            finish()  // Close the SplashActivity so it is not accessible again
        }, 3000)  // 3000 milliseconds = 3 seconds
    }
}
