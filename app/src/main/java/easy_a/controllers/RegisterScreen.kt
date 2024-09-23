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
import com.google.android.material.textfield.TextInputLayout
import easy_a.controllers.RetrofitClient
import easy_a.models.UserResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterScreen : AppCompatActivity() {
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var firstNameEditText: EditText
    private lateinit var lastNameEditText: EditText
    private lateinit var genderEditText: EditText // Optional
    private lateinit var dobEditText: EditText // Optional
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
        firstNameEditText = findViewById(R.id.firstName)
        lastNameEditText = findViewById(R.id.lastName)
        genderEditText = findViewById(R.id.gender) // Optional
        dobEditText = findViewById(R.id.dob) // Optional
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
        val firstName = firstNameEditText.text.toString()
        val lastName = lastNameEditText.text.toString()
        val gender = if (genderEditText.text.toString().trim().isNotEmpty()) genderEditText.text.toString().trim() else null // Optional
        val dob = if (dobEditText.text.toString().trim().isNotEmpty()) dobEditText.text.toString().trim() else null // Optional

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

        if (email.isNotEmpty() && password.isNotEmpty() && firstName.isNotEmpty() && lastName.isNotEmpty()) {
            // Call register API using Retrofit, adding optional fields like Gender and DateOfBirth
            RetrofitClient.apiService.registerUser(email, password, firstName, lastName, gender, dob)
                .enqueue(object : Callback<UserResponse> {
                    override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                        if (response.isSuccessful) {
                            // Registration successful
                            val user = response.body()
                            Toast.makeText(this@RegisterScreen, "Welcome ${user?.firstName}", Toast.LENGTH_SHORT).show()

                            // Optionally: Navigate to the login screen
                            val intent = Intent(this@RegisterScreen, LoginScreen::class.java)
                            startActivity(intent)
                        } else {
                            // Registration failed
                            val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                            Toast.makeText(this@RegisterScreen, "Registration failed: $errorMessage", Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                        Toast.makeText(this@RegisterScreen, "Registration failed: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        } else {
            Toast.makeText(this, "Please fill all the required fields", Toast.LENGTH_SHORT).show()
        }
    }

    // Email validation logic
    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches() && email.length <= 100
    }

    // Password validation logic
    private fun isValidPassword(password: String): Boolean {
        val passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$"
        return password.matches(passwordRegex.toRegex())
    }

    // Method to handle login link click
    fun navigateToLogin(view: View) {
        val intent = Intent(this, LoginScreen::class.java)
        startActivity(intent)
    }
}
