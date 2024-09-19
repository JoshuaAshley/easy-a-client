package com.example.proactive_opsc7311_poe.controllers

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.proactive_opsc7311_poe.R
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.firestore

class RegisterScreen : AppCompatActivity()
{
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var passwordTextInputLayout: TextInputLayout
    private lateinit var confirmPasswordTextInputLayout: TextInputLayout

    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.register_screen)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Find views
        emailEditText = findViewById(R.id.email)
        passwordEditText = findViewById(R.id.password)
        confirmPasswordEditText = findViewById(R.id.confirmPassword)
        passwordTextInputLayout = findViewById(R.id.passwordTextInputLayout)
        confirmPasswordTextInputLayout = findViewById(R.id.confrimPasswordTextLayout)
    }

    fun btnLoginClicked(view: View)
    {
        // Define the destination activity class
        val destinationActivity = LoginScreen::class.java

        // Create an Intent to start the destination activity
        val intent = Intent(this, destinationActivity)

        // Start the destination activity
        startActivity(intent)
    }

    //perform more logic when backend is implemented
    fun btnRegisterClicked(view: View)
    {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()
        val confirmPassword = confirmPasswordEditText.text.toString()

        if (!isValidEmail(email))
        {
            emailEditText.error = "Enter a valid email address"
            return
        }

        if (password != confirmPassword)
        {
            confirmPasswordTextInputLayout.error = "Passwords do not match"
            return
        }

        if (!isValidPassword(password))
        {
            passwordTextInputLayout.error =
                "Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
            return
        }

        // Create user with email and password
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
                if (task.isSuccessful)
                {
                    // Registration success, create firestore document for user
                    val user = auth.currentUser
                    val userId = user?.uid ?: ""

                    // Create firestore document with user's UID
                    val userDocRef = db.collection("users").document()
                    val userData = hashMapOf(
                        "uid" to userId,
                        "firstname" to "",
                        "lastname" to "",
                        "gender" to "",
                        "pfp" to "",
                        "dob" to null,
                        "pfp" to "",
                    )

                    userDocRef.set(userData).addOnSuccessListener {

                            Toast.makeText(
                                baseContext, "Register successful.", Toast.LENGTH_SHORT
                            ).show()

                            // Document creation success
                            val intent = Intent(this, MainScreen::class.java)

                            startActivity(intent)
                            finish()
                        }.addOnFailureListener { e ->
                            // Document creation failed
                            Toast.makeText(
                                baseContext,
                                "Failed to create user document: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                } else
                {
                    // Registration failed, handle exceptions
                    if (task.exception is FirebaseAuthUserCollisionException)
                    {
                        // User with this email already exists
                        emailEditText.error = "Email already registered"
                    } else
                    {
                        Toast.makeText(
                            baseContext, "Registration failed.", Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
    }

    private fun isValidEmail(email: String): Boolean
    {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches() && email.length <= 100
    }

    private fun isValidPassword(password: String): Boolean
    {
        val passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+\$).{8,}\$"
        return password.matches(passwordRegex.toRegex())
    }
}