package com.example.proactive_opsc7311_poe.controllers

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
import com.example.proactive_opsc7311_poe.R
import com.google.firebase.auth.FirebaseAuth

class LoginScreen : AppCompatActivity()
{
    private lateinit var auth: FirebaseAuth
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_screen)

        // Initialize Firebase Authentication
        auth = FirebaseAuth.getInstance()

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

        // Validate that email and password are not empty
        if (inputEmail.isNotEmpty() && inputPassword.isNotEmpty())
        {
            // Authenticate with Firebase using the email and password
            auth.signInWithEmailAndPassword(inputEmail, inputPassword)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful)
                    {
                        // Check if Remember Me is checked and save credentials if true
                        if (rememberMe)
                        {
                            val editor = sharedPreferences.edit()
                            editor.putString("email", inputEmail)
                            editor.putString("password", inputPassword)
                            editor.apply()
                        } else
                        {
                            // Clear stored credentials if Remember Me is not checked
                            sharedPreferences.edit().clear().apply()
                        }
                        Toast.makeText(baseContext, "Login successful.", Toast.LENGTH_SHORT).show()
                        // Navigate to MainScreen and finish this activity
                        val intent = Intent(this, MainScreen::class.java)
                        startActivity(intent)
                        finish()
                    } else
                    {
                        // Notify user if authentication fails
                        Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
        } else
        {
            // Prompt user to enter both email and password
            Toast.makeText(baseContext, "Please enter email and password.", Toast.LENGTH_SHORT)
                .show()
        }
    }

    fun onForgotPasswordClicked(view: View)
    {
        // Create an EditText programmatically to allow the user to input their email.
        val emailInput = EditText(this).apply {
            inputType =
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS  // Set the input type to accept email addresses.
            hint = "Enter your email"  // Set a hint to guide users on what to enter.
        }

        // Build and display an AlertDialog which contains the EditText for email input.
        AlertDialog.Builder(this).setTitle("Reset Password")  // Set the title of the dialog.
            .setMessage("Enter your email to receive reset instructions")  // Set a message explaining what the dialog is for.
            .setView(emailInput)  // Embed the EditText in the dialog.
            .setPositiveButton("Send") { dialog, which ->  // Define a 'Send' button and its behavior.
                val email = emailInput.text.toString()  // Retrieve the email entered by the user.
                if (email.isNotEmpty())
                {  // Check if the email field is not empty.
                    // Request Firebase to send a password reset email.
                    FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful)
                            {  // Check if the email was sent successfully.
                                Toast.makeText(
                                    this,
                                    "Reset instructions sent to your email",
                                    Toast.LENGTH_LONG
                                ).show()  // Show a success message.
                            } else
                            {  // Handle the case where sending the email failed.
                                Toast.makeText(
                                    this,
                                    "Failed to send reset email",
                                    Toast.LENGTH_LONG
                                ).show()  // Show an error message.
                            }
                        }
                } else
                {  // If the email field is empty, prompt the user to fill it.
                    Toast.makeText(this, "Email field cannot be empty", Toast.LENGTH_LONG).show()
                }
            }.setNegativeButton(
                "Cancel",
                null
            )  // Define a 'Cancel' button that simply dismisses the dialog.
            .show()  // Display the dialog.
    }
}
