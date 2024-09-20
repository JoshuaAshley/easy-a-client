package com.example.easy_a.controllers

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.easy_a.R

class LoginScreen : AppCompatActivity() {
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_screen)

        // Initialize SharedPreferences to store and retrieve login credentials
        sharedPreferences = getSharedPreferences("com.example.easy_a", Context.MODE_PRIVATE)

        // Get references to EditText fields
        email = findViewById(R.id.email)
        password = findViewById(R.id.password)

        // Check if credentials are stored in SharedPreferences and set them in the input fields
        val savedEmail = sharedPreferences.getString("email", "") ?: ""
        val savedPassword = sharedPreferences.getString("password", "") ?: ""
        if (savedEmail.isNotEmpty() && savedPassword.isNotEmpty()) {
            email.setText(savedEmail)
            password.setText(savedPassword)
            findViewById<CheckBox>(R.id.rememberMeCheckBox).isChecked = true // Set the checkbox to checked state
        }
    }

    // Called when the Sign Up button is clicked
    fun btnSignUpClicked(view: View) {
        val intent = Intent(this, RegisterScreen::class.java)
        startActivity(intent)
    }

    // Called when the Login button is clicked
    fun btnLoginClicked(view: View) {
        val inputEmail = email.text.toString()
        val inputPassword = password.text.toString()
        val rememberMe = findViewById<CheckBox>(R.id.rememberMeCheckBox).isChecked

        // Check if email and password are not empty
        if (inputEmail.isNotEmpty() && inputPassword.isNotEmpty()) {
            // If Remember Me is checked, save credentials to SharedPreferences
            if (rememberMe) {
                val editor = sharedPreferences.edit()
                editor.putString("email", inputEmail)
                editor.putString("password", inputPassword)
                editor.apply()
            }
            // Display toast message for successful login
            Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()

            // Navigate to MainScreen after successful login
            val intent = Intent(this, MainScreen::class.java)
            startActivity(intent)
            finish()  // Optionally finish the current activity
        } else {
            Toast.makeText(this, "Please enter your email and password", Toast.LENGTH_SHORT).show()
        }
    }

    // Called when Forgot Password is clicked
    fun onForgotPasswordClicked(view: View) {
        val emailInput = EditText(this).apply {
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            hint = "Enter your email"
        }

        AlertDialog.Builder(this)
            .setTitle("Forgot Password")
            .setMessage("Enter your registered email")
            .setView(emailInput)
            .setPositiveButton("Submit") { dialog, _ ->
                val enteredEmail = emailInput.text.toString()
                // Logic to handle password reset (e.g., sending a reset email)
                Toast.makeText(this, "Reset link sent to $enteredEmail", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }
}
