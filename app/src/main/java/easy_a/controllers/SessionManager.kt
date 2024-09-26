package easy_a.controllers

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("com.example.easy_a", Context.MODE_PRIVATE)

    // Check if the user is logged in
    fun isLoggedIn(): Boolean {
        val token = sharedPreferences.getString("token", null)
        return token != null
    }

    // Get the stored token
    fun getToken(): String? {
        return sharedPreferences.getString("token", null)
    }

    // Get the stored token
    fun getUid(): String? {
        return sharedPreferences.getString("uid", null)
    }

    // Get user details (add more methods as needed)
    fun getEmail(): String? {
        return sharedPreferences.getString("email", null)
    }

    fun getFirstName(): String? {
        return sharedPreferences.getString("firstname", null)
    }

    fun getLastName(): String? {
        return sharedPreferences.getString("lastname", null)
    }

    fun getGender(): String? {
        return sharedPreferences.getString("gender", null)
    }

    fun getDateOfBirth(): String? {
        return sharedPreferences.getString("dateOfBirth", null)
    }

    fun getProfilePictureUrl(): String? {
        return sharedPreferences.getString("profilePictureUrl", null)
    }

    // Clear user session (use this for logging out)
    fun clearSession() {
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }
}