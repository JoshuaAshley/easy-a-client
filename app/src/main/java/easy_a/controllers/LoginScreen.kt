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

class LoginScreen : AppCompatActivity()
{
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_screen)


        // Initialize SharedPreferences to store and retrieve login credentials
        sharedPreferences =
            getSharedPreferences("com.example.proactive_opsc7311_poe", Context.MODE_PRIVATE)

        // Get references to EditText fields
        email = findViewById(R.id.email)
        password = findViewById(R.id.password)

        // Check if credentials are stored in SharedPreferences and set them in the input fields
        val savedEmail = sharedPreferences.getString("email", "") ?: ""
        val savedPassword = sharedPreferences.getString("password", "") ?: ""
        if (savedEmail.isNotEmpty() && savedPassword.isNotEmpty())
        {
            email.setText(savedEmail)
            password.setText(savedPassword)
            findViewById<CheckBox>(R.id.rememberMeCheckBox)?.isChecked =
                true // Set the checkbox to checked state
        }
    }

    fun btnSignUpClicked(view: View)
    {
        // Intent to navigate to the RegisterScreen
        val destinationActivity = RegisterScreen::class.java
        val intent = Intent(this, destinationActivity)
        startActivity(intent)
    }

    fun btnLoginClicked(view: View)
    {
        // Get the email and password from EditTexts
        val inputEmail = email.text.toString()
        val inputPassword = password.text.toString()
        // Check the state of the Remember Me checkbox
        val rememberMe = findViewById<CheckBox>(R.id.rememberMeCheckBox).isChecked
    }

    fun onForgotPasswordClicked(view: View)
    {
        // Create an EditText programmatically to allow the user to input their email.
        val emailInput = EditText(this).apply {
            inputType =
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS  // Set the input type to accept email addresses.
            hint = "Enter your email"  // Set a hint to guide users on what to enter.
        }
    }
}
