package com.example.easy_a.controllers

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.easy_a.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_screen)

        // Load PomodoroScreen fragment by default
        navigateToFragment(PomodoroScreen())

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.home -> {
                    // Load home fragment if needed
                    true
                }
                R.id.add -> {
                    // Navigate to the PomodoroScreen
                    navigateToFragment(PomodoroScreen())
                    true
                }
                else -> false
            }
        }
    }

    private fun navigateToFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
