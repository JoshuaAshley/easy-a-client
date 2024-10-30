package easy_a.controllers

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import easy_a.application.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import easy_a.controllers.RetrofitClient
import easy_a.models.UserResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView

class LoginScreen : AppCompatActivity() {

    // Declare variables for Email/Password fields
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var sharedPreferences: SharedPreferences

    // Google Sign-In related variables
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var sessionManager: SessionManager

    // Constant for Google Sign-In Intent
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_screen)

        // Initialize SharedPreferences to store and retrieve login credentials
        sharedPreferences = getSharedPreferences("com.example.easy_a", Context.MODE_PRIVATE)
        val languageCode = sharedPreferences.getString("language", "en") ?: "en"
        // Set the language before super.onCreate()
        LanguageHelper.setLocale(this, languageCode)

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Configure Google Sign-In to require the user to select an account
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Use the client ID from Firebase
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Get references to EditText fields
        email = findViewById(R.id.email)
        password = findViewById(R.id.password)

        // Check if credentials are stored in SharedPreferences and set them in the input fields
        val savedEmail = sharedPreferences.getString("savedEmail", "") ?: ""
        val savedPassword = sharedPreferences.getString("savedPassword", "") ?: ""
        // Set the saved email and password in the respective EditText fields
        email.setText(savedEmail)
        password.setText(savedPassword)

        sessionManager = SessionManager(this)

        if (!sessionManager.isDarkMode()) {
            // Set all text elements to black for light mode
            findViewById<NestedScrollView>(R.id.mainLayout).setBackgroundColor(resources.getColor(R.color.white))
            findViewById<ImageView>(R.id.logoImage).setImageDrawable(resources.getDrawable(R.drawable.easy_a_logo_dark))
            findViewById<TextView>(R.id.titleTextView).setTextColor(resources.getColor(R.color.black))
            findViewById<TextView>(R.id.emailTextView).setTextColor(resources.getColor(R.color.black))
            findViewById<TextView>(R.id.passwordTextView).setTextColor(resources.getColor(R.color.black))
            findViewById<TextView>(R.id.dividerText).setTextColor(resources.getColor(R.color.black))
            findViewById<TextView>(R.id.signUpText).setTextColor(resources.getColor(R.color.black))
            findViewById<EditText>(R.id.email).background = resources.getDrawable(R.drawable.textfield_light)
            findViewById<EditText>(R.id.password).background = resources.getDrawable(R.drawable.textfield_light)
            findViewById<CheckBox>(R.id.rememberMeCheckBox).setTextColor(resources.getColor(R.color.black))
        }

        // Optional: Check the Remember Me checkbox if credentials exist
        if (savedEmail.isNotEmpty()) {
            findViewById<CheckBox>(R.id.rememberMeCheckBox).isChecked = true
        }
    }

    // Called when the Sign-Up button is clicked
    fun btnSignUpClicked(view: View) {
        val intent = Intent(this, RegisterScreen::class.java)
        startActivity(intent)
    }

    // Called when the Login button is clicked (Email/Password Login)
    fun btnLoginClicked(view: View) {
        val inputEmail = email.text.toString()
        val inputPassword = password.text.toString()
        val rememberMe = findViewById<CheckBox>(R.id.rememberMeCheckBox).isChecked

        if (inputEmail.isNotEmpty() && inputPassword.isNotEmpty()) {
            // Call login API using Retrofit
            RetrofitClient.apiService.loginUser(inputEmail, inputPassword)
                .enqueue(object : Callback<UserResponse> {
                    override fun onResponse(
                        call: Call<UserResponse>,
                        response: Response<UserResponse>
                    ) {
                        if (response.isSuccessful) {
                            val user = response.body()

                            // Format date of birth
                            val dateOfBirthString = user?.dateOfBirth // This can be null



                            val formattedDateOfBirth = if (dateOfBirthString != null && dateOfBirthString != "") {
                                formatDateOfBirth(dateOfBirthString)
                            } else {
                                "" // Assign a default value like "N/A" or leave it empty
                            }

                            // Save user details and token in SharedPreferences
                            val editor = sharedPreferences.edit()
                            editor.putString("token", user?.token)
                            editor.putString("uid", user?.uid)
                            editor.putString("email", user?.email)
                            editor.putString("firstname", user?.firstName)
                            editor.putString("lastname", user?.lastName)
                            editor.putString("gender", user?.gender)
                            editor.putString("dateOfBirth", formattedDateOfBirth) // Store the formatted date
                            editor.putString("profilePictureUrl", user?.profilePicture)
                            editor.putString("language", user?.language ?: "english")
                            editor.putBoolean("notifications", user?.notifications ?: false)
                            editor.putString("theme", user?.theme ?: "dark")
                            editor.putBoolean("biometricAuthentication", user?.biometricAuthentication ?: false)




                            if (rememberMe) {
                                editor.putString("savedEmail", inputEmail)
                                editor.putString("savedPassword", inputPassword)
                            } else {
                                // Clear saved credentials if unchecked
                                editor.remove("savedEmail")
                                editor.remove("savedPassword")
                            }

                            editor.apply() // Ensure this line is called after all changes to SharedPreferences.

                            if (user?.biometricAuthentication == false) {
                                proceedToMainScreen()
                            } else {
                                showBiometricPrompt()
                            }
                        } else {
                            Toast.makeText(this@LoginScreen, getString(R.string.invalid_login_credentials), Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                        Toast.makeText(this@LoginScreen, getString(R.string.login_failed, t.message), Toast.LENGTH_SHORT).show()
                    }
                })
        } else {
            Toast.makeText(this, getString(R.string.enter_email_password), Toast.LENGTH_SHORT).show()
        }
    }

    // Helper function to format the date
    private fun formatDateOfBirth(dateString: String?): String? {
        return dateString?.let {
            // Remove the "Timestamp: " prefix if it exists
            val cleanDateString = it.replace("Timestamp: ", "").trim()

            // Parse the original timestamp format
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            val date: Date? = inputFormat.parse(cleanDateString)

            // Format it to the desired format
            val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            date?.let { outputFormat.format(it) }
        }
    }

    // Called when Forgot Password is clicked
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
                                Toast.makeText(this, getString(R.string.reset_instructions_sent), Toast.LENGTH_LONG).show()
                            } else
                            {  // Handle the case where sending the email failed.
                                Toast.makeText(this, getString(R.string.failed_to_send_reset_email), Toast.LENGTH_LONG).show()
                            }
                        }
                } else
                {  // If the email field is empty, prompt the user to fill it.
                    Toast.makeText(this, getString(R.string.email_field_empty), Toast.LENGTH_LONG).show()
                }
            }.setNegativeButton(
                "Cancel",
                null
            )  // Define a 'Cancel' button that simply dismisses the dialog.
            .show()  // Display the dialog.
    }

    // Called when the Google Login button is clicked
    fun btnGoogleLoginClicked(view: View) {
        googleSignInClient.signOut()
        // Prompt user to select an account
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    // Handle the result of the Google Sign-In intent
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign-In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)
                handleGoogleSignIn(account!!)
            } catch (e: ApiException) {
                // Google Sign-In failed
                Toast.makeText(this, getString(R.string.google_sign_in_failed, e.message), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun proceedToMainScreen() {
        Toast.makeText(this, getString(R.string.login_successful), Toast.LENGTH_SHORT).show()
        val intent = Intent(this@LoginScreen, MainScreen::class.java)
        startActivity(intent)
        finish()
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                // Authentication succeeded, proceed with login
                proceedToMainScreen()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(this@LoginScreen, getString(R.string.biometric_auth_failed), Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(this@LoginScreen, getString(R.string.auth_error, errString), Toast.LENGTH_SHORT).show()
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Authentication")
            .setSubtitle("Authenticate using your fingerprint or face")
            .setDescription("We need your biometric authentication to proceed")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun handleGoogleSignIn(account: GoogleSignInAccount) {

        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    val user = firebaseAuth.currentUser
                    // Get the ID token and other details from the account
                    val uid = user?.uid ?: "" // Use id instead of idToken
                    val email = account.email ?: ""
                    val firstName = account.givenName ?: ""
                    val lastName = account.familyName ?: ""
                    val profilePicture = account.photoUrl?.toString() // Convert to string if it's not null

                    // Call your backend to register/sign in the user with Google token
                    registerUserWithGoogle(uid, email, firstName, lastName, profilePicture)
                } else {
                    // Sign in failed
                    Toast.makeText(this@LoginScreen, getString(R.string.google_sign_in_failed_backend), Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun registerUserWithGoogle(uid: String, email: String, firstName: String, lastName: String, profilePicture: String?) {
        Log.d("RegisterScreen", "Registering user with Google uid: $uid, email: $email, firstName: $firstName, lastName: $lastName, pfp: $profilePicture") // Log uid and email

        // Call your API to create a new user record in Firestore
        RetrofitClient.apiService.registerUserWithGoogle(uid, email, firstName, lastName, profilePicture).enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    // Google Sign-In successful
                    val user = response.body()

                    val editor = sharedPreferences.edit()
                    editor.putString("token", uid)
                    editor.putString("uid", uid)
                    editor.putString("email", email)
                    editor.putString("firstname", firstName)
                    editor.putString("lastname", lastName)
                    editor.putString("profilePictureUrl", profilePicture)

                    editor.apply()

                    Toast.makeText(this@LoginScreen, getString(R.string.welcome_user, user?.email ?: ""), Toast.LENGTH_SHORT).show()


                    // Navigate to the main screen
                    val intent = Intent(this@LoginScreen, MainScreen::class.java)
                    startActivity(intent)
                } else {
                    // Handle errors from backend
                    Log.e("RegisterScreen", "Google Sign-In backend response: ${response.errorBody()?.string()}")
                    Toast.makeText(this@LoginScreen, getString(R.string.google_sign_in_failed_backend), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                // Network or other unexpected errors
                Toast.makeText(this@LoginScreen, getString(R.string.google_sign_in_failed_backend), Toast.LENGTH_SHORT).show()
            }
        })
    }
}