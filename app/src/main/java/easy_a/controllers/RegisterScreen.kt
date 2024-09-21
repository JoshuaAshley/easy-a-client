package com.example.easy_a.controllers

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.easy_a.R
import com.example.easy_a.R.id.confirmPasswordTextInputLayout
import com.example.easy_a.R.id.passwordTextInputLayout
import com.google.android.material.textfield.TextInputLayout

class RegisterScreen : AppCompatActivity() {
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var passwordTextInputLayout: TextInputLayout
    private lateinit var confirmPasswordTextInputLayout: TextInputLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.register_screen)

        // Find views
        emailEditText = findViewById(R.id.email)
        passwordEditText = findViewById(R.id.password)
        confirmPasswordEditText = findViewById(R.id.confirmPassword)
        passwordTextInputLayout = findViewById(R.id.passwordTextInputLayout)
        confirmPasswordTextInputLayout = findViewById(R.id.confirmPasswordTextInputLayout)
    }

    // Called when "Already have an account?" is clicked
    fun btnLoginClicked(view: View) {
        // Navigate to Login screen
        val intent = Intent(this, LoginScreen::class.java)
        startActivity(intent)
    }

    // Called when the Sign Up button is clicked
    fun btnRegisterClicked(view: View) {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()
        val confirmPassword = confirmPasswordEditText.text.toString()

        // Clear previous errors
        emailEditText.error = null
        passwordTextInputLayout.error = null
        confirmPasswordTextInputLayout.error = null

        // Validate email
        if (!isValidEmail(email)) {
            emailEditText.error = "Enter a valid email address"
            return
        }

        // Validate password
        if (password != confirmPassword) {
            confirmPasswordTextInputLayout.error = "Passwords do not match"
            return
        }

        if (!isValidPassword(password)) {
            passwordTextInputLayout.error = "Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
            return
        }

        // Proceed with registration logic (to be implemented)
        Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()

        // Navigate to the Login screen after successful registration
        val intent = Intent(this, LoginScreen::class.java)
        startActivity(intent)
        finish() // Close the current activity
    }

    // Email validation logic
    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches() && email.length <= 100
    }

    // Password validation logic
    private fun isValidPassword(password: String): Boolean {
        val passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+\$).{8,}\$"
        return password.matches(passwordRegex.toRegex())
    }
}
