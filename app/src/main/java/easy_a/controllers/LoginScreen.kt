package easy_a.controllers

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.easy_a.R
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

class LoginScreen : AppCompatActivity() {

    // Declare variables for Email/Password fields
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var sharedPreferences: SharedPreferences

    // Google Sign-In related variables
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth

    // Constant for Google Sign-In Intent
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_screen)

        // Initialize SharedPreferences to store and retrieve login credentials
        sharedPreferences = getSharedPreferences("com.example.easy_a", Context.MODE_PRIVATE)

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
        val savedEmail = sharedPreferences.getString("email", "") ?: ""
        val savedPassword = sharedPreferences.getString("password", "") ?: ""
        if (savedEmail.isNotEmpty() && savedPassword.isNotEmpty()) {
            email.setText(savedEmail)
            password.setText(savedPassword)
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

                            // Save user details and token in SharedPreferences
                            val editor = sharedPreferences.edit()
                            editor.putString("token", user?.token)
                            editor.putString("email", user?.email)
                            editor.putString("firstname", user?.firstName)
                            editor.putString("lastname", user?.lastName)
                            editor.putString("gender", user?.gender)
                            editor.putString("dateOfBirth", user?.dateOfBirth)
                            editor.putString("profilePictureUrl", user?.profilePicture)

                            editor.apply()

                            Toast.makeText(this@LoginScreen, "Welcome ${user?.email}", Toast.LENGTH_SHORT).show()

                            // Navigate to MainScreen
                            val intent = Intent(this@LoginScreen, MainScreen::class.java)
                            startActivity(intent)
                            finish()
                        } else {
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
                Toast.makeText(this, "Reset link sent to $enteredEmail", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
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
                Toast.makeText(this, "Google sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
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
                    val uid = user?.uid ?: "" // Use id instead of idToken
                    val email = account.email ?: ""
                    val firstName = account.givenName ?: ""
                    val lastName = account.familyName ?: ""
                    val profilePicture = account.photoUrl?.toString() // Convert to string if it's not null

                    // Call your backend to register/sign in the user with Google token
                    registerUserWithGoogle(uid, email, firstName, lastName, profilePicture)
                } else {
                    // Sign in failed
                    Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show()
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
                    editor.putString("email", email)
                    editor.putString("firstname", firstName)
                    editor.putString("lastname", lastName)
                    editor.putString("profilePictureUrl", profilePicture)

                    editor.apply()

                    Toast.makeText(this@LoginScreen, "Welcome ${user?.email}", Toast.LENGTH_SHORT).show()

                    // Navigate to the main screen
                    val intent = Intent(this@LoginScreen, MainScreen::class.java)
                    startActivity(intent)
                } else {
                    // Handle errors from backend
                    Log.e("RegisterScreen", "Google Sign-In backend response: ${response.errorBody()?.string()}")
                    Toast.makeText(this@LoginScreen, "Google Sign-In failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                // Network or other unexpected errors
                Toast.makeText(this@LoginScreen, "Google Sign-In failed: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}