package com.example.easy_a.controllers

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.easy_a.R
import easy_a.controllers.RetrofitClient
import easy_a.models.UserResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
            // Call login API using Retrofit
            RetrofitClient.apiService.loginUser(inputEmail, inputPassword)
                .enqueue(object : Callback<UserResponse> {
                    override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                        if (response.isSuccessful) {
                            // Login successful
                            val user = response.body()
                            Toast.makeText(this@LoginScreen, "Welcome ${user?.firstName}", Toast.LENGTH_SHORT).show()

                            // If Remember Me is checked, save credentials to SharedPreferences
                            if (rememberMe) {
                                val editor = sharedPreferences.edit()
                                editor.putString("email", inputEmail)
                                editor.putString("password", inputPassword)
                                editor.apply()
                            }

                            // Navigate to MainScreen after successful login
                            val intent = Intent(this@LoginScreen, MainScreen::class.java)
                            startActivity(intent)
                            finish()  // Optionally finish the current activity
                        } else {
                            // Login failed
                            Toast.makeText(this@LoginScreen, "Invalid login credentials", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                        Toast.makeText(this@LoginScreen, "Login failed: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        } else {
            Toast.makeText(this, "Please enter your email and password", Toast.LENGTH_SHORT).show()
        }
    }

    // Called when Forgot Password is clicked
    fun onForgotPasswordClicked(view: View) {
        val emailInput = EditText(this).apply {
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            hint = "Enter your email"
        }

        android.app.AlertDialog.Builder(this)
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
