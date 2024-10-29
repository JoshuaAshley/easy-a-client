package easy_a.controllers

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import easy_a.application.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.GoogleAuthProvider
import easy_a.controllers.RetrofitClient
import easy_a.models.UserResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.HttpURLConnection

class RegisterScreen : AppCompatActivity() {
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var passwordTextInputLayout: TextInputLayout
    private lateinit var confirmPasswordTextInputLayout: TextInputLayout
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.register_screen)

        // Initialize FirebaseAuth
        firebaseAuth = FirebaseAuth.getInstance()

        // Initialize SharedPreferences to store and retrieve login credentials
        sharedPreferences = getSharedPreferences("com.example.easy_a", Context.MODE_PRIVATE)
        val languageCode = sharedPreferences.getString("language", "en") ?: "en"
        // Set the language before super.onCreate()
        LanguageHelper.setLocale(this, languageCode)

        // Find views
        emailEditText = findViewById(R.id.email)
        passwordEditText = findViewById(R.id.password)
        confirmPasswordEditText = findViewById(R.id.confirmPassword)
        passwordTextInputLayout = findViewById(R.id.passwordTextInputLayout)
        confirmPasswordTextInputLayout = findViewById(R.id.confirmPasswordTextInputLayout)

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(getString(R.string.default_web_client_id)) // Ensure you have this string resource defined
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        sessionManager = SessionManager(this)

        if (!sessionManager.isDarkMode()) {
            // Set all text elements to black for light mode
            findViewById<NestedScrollView>(R.id.mainLayout).setBackgroundColor(resources.getColor(R.color.white))
            findViewById<ImageView>(R.id.logoImage).setImageDrawable(resources.getDrawable(R.drawable.easy_a_logo_dark))
            findViewById<TextView>(R.id.titleTextView).setTextColor(resources.getColor(R.color.black))
            findViewById<TextView>(R.id.emailTextView).setTextColor(resources.getColor(R.color.black))
            findViewById<TextView>(R.id.passwordTextView).setTextColor(resources.getColor(R.color.black))
            findViewById<TextView>(R.id.confirmPasswordTextView).setTextColor(resources.getColor(R.color.black))
            findViewById<TextView>(R.id.dividerText).setTextColor(resources.getColor(R.color.black))
            findViewById<TextView>(R.id.signUpText).setTextColor(resources.getColor(R.color.black))
            findViewById<EditText>(R.id.email).background = resources.getDrawable(R.drawable.textfield_light)
            findViewById<EditText>(R.id.password).background = resources.getDrawable(R.drawable.textfield_light)
            findViewById<EditText>(R.id.confirmPassword).background = resources.getDrawable(R.drawable.textfield_light)
        }
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

        if (email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
            // Call register API using Retrofit
            RetrofitClient.apiService.registerUser(email, password)
                .enqueue(object : Callback<UserResponse> {
                    override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                        if (response.isSuccessful && response.body() != null) {
                            // Registration successful
                            val user = response.body()

                            val editor = sharedPreferences.edit()

                            editor.putString("token", user?.token)
                            editor.putString("uid", user?.uid)
                            editor.putString("email", user?.email)
                            editor.putString("profilePictureUrl", user?.profilePicture)
                            editor.putString("firstname", "")
                            editor.putString("lastname", "")
                            editor.putString("dateOfBirth", "")
                            editor.putString("gender", "Other")

                            editor.apply()

                            Toast.makeText(this@RegisterScreen, "Welcome, ${user?.email}", Toast.LENGTH_SHORT).show()

                            // Optionally: Navigate to the main screen
                            val intent = Intent(this@RegisterScreen, MainScreen::class.java)
                            startActivity(intent)
                        } else {
                            handleRegistrationError(response)
                        }
                    }

                    override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                        // Network or other unexpected errors
                        Toast.makeText(this@RegisterScreen, "Registration failed: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        } else {
            // Show error if email or password is missing
            Toast.makeText(this, "Please fill all the required fields", Toast.LENGTH_SHORT).show()
        }
    }

    // Google Sign-In button clicked
    fun btnGoogleSignUpClicked(view: View) {
        googleSignInClient.signOut().addOnCompleteListener {
            // After signing out, proceed with the sign-in
            startSignIn()
        }
    }

    private fun startSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                handleGoogleSignIn(account) // Call your handling method
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleGoogleSignIn(account: GoogleSignInAccount) {

        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    val user = firebaseAuth.currentUser
                    // Get the ID token and other details from the account
                    val token = account.idToken ?: "" // Use id instead of idToken
                    val uid = user?.uid ?: "" // Use id instead of idToken
                    val email = account.email ?: ""
                    val firstName = account.givenName ?: ""
                    val lastName = account.familyName ?: ""
                    val profilePicture = account.photoUrl?.toString() // Convert to string if it's not null

                    // Call your backend to register/sign in the user with Google token
                    registerUserWithGoogle(token, uid, email, firstName, lastName, profilePicture)
                } else {
                    // Sign in failed
                    Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun registerUserWithGoogle(token: String, uid: String, email: String, firstName: String, lastName: String, profilePicture: String?) {
        Log.d("RegisterScreen", "Registering user with Google uid: $uid, email: $email, firstName: $firstName, lastName: $lastName, pfp: $profilePicture") // Log uid and email

        // Call your API to create a new user record in Firestore
        RetrofitClient.apiService.registerUserWithGoogle(uid, email, firstName, lastName, profilePicture).enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    // Google Sign-In successful
                    val user = response.body()

                    val editor = sharedPreferences.edit()
                    editor.putString("token", token)
                    editor.putString("uid", uid)
                    editor.putString("email", email)
                    editor.putString("firstname", firstName)
                    editor.putString("lastname", lastName)
                    editor.putString("profilePictureUrl", profilePicture)

                    editor.apply()

                    Toast.makeText(this@RegisterScreen, "Welcome ${user?.email}", Toast.LENGTH_SHORT).show()

                    // Navigate to the main screen
                    val intent = Intent(this@RegisterScreen, MainScreen::class.java)
                    startActivity(intent)
                } else {
                    // Handle errors from backend
                    Log.e("RegisterScreen", "Google Sign-In backend response: ${response.errorBody()?.string()}")
                    Toast.makeText(this@RegisterScreen, "Google Sign-In failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                // Network or other unexpected errors
                Toast.makeText(this@RegisterScreen, "Google Sign-In failed: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
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

    private fun handleRegistrationError(response: Response<UserResponse>) {
        when (response.code()) {
            HttpURLConnection.HTTP_BAD_REQUEST -> {
                val errorMessage = "Email already exists."
                Toast.makeText(this@RegisterScreen, "Registration failed: $errorMessage", Toast.LENGTH_LONG).show()
            }
            HttpURLConnection.HTTP_INTERNAL_ERROR -> {
                Toast.makeText(this@RegisterScreen, "Server error. Please try again later.", Toast.LENGTH_LONG).show()
            }
            else -> {
                val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                Toast.makeText(this@RegisterScreen, "Registration failed: $errorMessage", Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        private const val RC_SIGN_IN = 9001
    }
}