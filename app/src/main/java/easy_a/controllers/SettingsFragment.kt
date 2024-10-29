package easy_a.controllers

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import easy_a.application.R
import easy_a.models.SettingsResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import easy_a.controllers.LanguageHelper


class SettingsFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private lateinit var spinnerLanguage: Spinner
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var switchNotifications: Switch
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var switchDarkMode: Switch
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var switchBiometric: Switch
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.settings_fragment, container, false)

        sharedPreferences = this.requireContext().getSharedPreferences("com.example.easy_a", Context.MODE_PRIVATE)

        sessionManager = SessionManager(requireContext())

        spinnerLanguage = view.findViewById(R.id.spinnerLanguage)
        switchNotifications = view.findViewById(R.id.switchNotifications)
        switchDarkMode = view.findViewById(R.id.switchDarkMode)
        switchBiometric = view.findViewById(R.id.switchBiometric)
        val btnSave = view.findViewById<Button>(R.id.btnChangePassword)

        setInitialSettings()

        btnSave.setOnClickListener {
            saveSettings()
        }

        val titleTextView = view.findViewById<TextView>(R.id.titleTextView)
        val languageTextView = view.findViewById<TextView>(R.id.languageTextView)
        val notificationsTextView = view.findViewById<TextView>(R.id.notificationsTextView)
        val themeTextView = view.findViewById<TextView>(R.id.themeTextView)
        val biometricTextView = view.findViewById<TextView>(R.id.biometricTextView)
        val switchNotifications = view.findViewById<Switch>(R.id.switchNotifications)
        val switchDarkMode = view.findViewById<Switch>(R.id.switchDarkMode)
        val switchBiometric = view.findViewById<Switch>(R.id.switchBiometric)

        if (!sessionManager.isDarkMode()) {
            // Set all text elements to black for light mode
            titleTextView.setTextColor(resources.getColor(R.color.black))
            languageTextView.setTextColor(resources.getColor(R.color.black))
            notificationsTextView.setTextColor(resources.getColor(R.color.black))
            themeTextView.setTextColor(resources.getColor(R.color.black))
            biometricTextView.setTextColor(resources.getColor(R.color.black))
            switchNotifications.setTextColor(resources.getColor(R.color.black))
            switchDarkMode.setTextColor(resources.getColor(R.color.black))
            switchBiometric.setTextColor(resources.getColor(R.color.black))
        }

        return view
    }

    private fun setInitialSettings() {
        // Get saved language code or default to "en"
        val languageCode = sharedPreferences.getString("language", "en")
        val languageArray = resources.getStringArray(R.array.language_array)

        // Map language code to display name for the spinner
        val languageName = when (languageCode) {
            "af" -> "Afrikaans"
            "zu" -> "Zulu"
            else -> "English"
        }

        // Set spinner selection based on saved language
        val languagePosition = languageArray.indexOf(languageName)
        if (languagePosition >= 0) {
            spinnerLanguage.setSelection(languagePosition)
        }

        switchNotifications.isChecked = sessionManager.isNotifications()
        switchDarkMode.isChecked = sessionManager.isDarkMode()
        switchBiometric.isChecked = sessionManager.isBiometricAuthentication()
    }

    private fun saveSettings() {
        val uid = sessionManager.getUid() ?: return // Ensure uid is not null
        val language = spinnerLanguage.selectedItem.toString()
        val notifications = switchNotifications.isChecked
        val theme = if (switchDarkMode.isChecked) "dark" else "light"
        val biometricAuthentication = if (switchBiometric.isChecked) "true" else "false"

        // Map selected language to its code
        val languageCode = when (language) {
            "Afrikaans" -> "af"
            "Zulu" -> "zu"
            else -> "en"
        }

        // Make API call to update settings
        RetrofitClient.apiService.updateSystemSettings(
            uid, languageCode, notifications, theme, biometricAuthentication
        ).enqueue(object : Callback<SettingsResponse> {
            override fun onResponse(
                call: Call<SettingsResponse>,
                response: Response<SettingsResponse>
            ) {
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), R.string.toast_user_updated, Toast.LENGTH_SHORT).show()

                    // Save the new settings in SharedPreferences
                    sharedPreferences.edit().apply {
                        putString("language", languageCode)
                        putBoolean("notifications", notifications)
                        putString("theme", theme)
                        putBoolean("biometricAuthentication", switchBiometric.isChecked)
                        apply()
                    }

                    // Apply the language change and restart the activity
                    LanguageHelper.setLocale(requireContext(), languageCode)
                    requireActivity().recreate() // Recreate to apply changes
                    recreateAllFragments()
                } else {
                    Toast.makeText(requireContext(), R.string.toast_update_failed, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<SettingsResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })

    }
    private fun recreateAllFragments() {
        val fragmentManager = requireActivity().supportFragmentManager
        val fragmentList = fragmentManager.fragments

        // Detach all fragments
        fragmentList.forEach { fragment ->
            fragment?.let {
                fragmentManager.beginTransaction().detach(it).commitAllowingStateLoss()
            }
        }

        // Execute the pending detach transactions
        fragmentManager.executePendingTransactions()

        // Reattach all fragments
        fragmentList.forEach { fragment ->
            fragment?.let {
                fragmentManager.beginTransaction().attach(it).commitAllowingStateLoss()
            }
        }
    }
}