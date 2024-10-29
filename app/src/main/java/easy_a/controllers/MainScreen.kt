package easy_a.controllers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import easy_a.application.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.ShapeAppearanceModel
import com.squareup.picasso.Picasso
import java.util.Calendar
import java.util.concurrent.TimeUnit
import easy_a.controllers.LanguageHelper


interface ProfileUpdateListener {
    fun onProfileUpdated(newProfilePictureUrl: String?)
}

class MainScreen : AppCompatActivity(), ProfileUpdateListener {
    private lateinit var sessionManager: SessionManager
    private lateinit var profileIcon: ShapeableImageView

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Set the selected language from SharedPreferences
        val sharedPreferences = getSharedPreferences("com.example.easy_a", Context.MODE_PRIVATE)
        val languageCode = sharedPreferences.getString("language", "en") ?: "en"
        LanguageHelper.setLocale(this, languageCode)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.main_screen)

        requestNotificationPermission()
        setRelativeSizes()

        sessionManager = SessionManager(this)
        profileIcon = findViewById(R.id.profileIcon)

        // Check if the user is logged in
        if (!sessionManager.isLoggedIn()) {
            val intent = Intent(this, LoginScreen::class.java)
            startActivity(intent)
            finish() // Close MainScreen
        } else {
            val profilePictureUrl = sessionManager.getProfilePictureUrl()
            profileIcon.shapeAppearanceModel = ShapeAppearanceModel.builder()
                .setAllCornerSizes(ShapeAppearanceModel.PILL).build()
            profileIcon.scaleType = ImageView.ScaleType.CENTER_INSIDE
            loadProfilePicture(profilePictureUrl)

            if (sessionManager.isNotifications()) {
                scheduleEventWorker()
                scheduleSyncWorker()
            }
        }

        val mainLayout = findViewById<FrameLayout>(R.id.main)
        val topBar = findViewById<ConstraintLayout>(R.id.top_bar)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        if (!sessionManager.isDarkMode()) {
            mainLayout.setBackgroundColor(resources.getColor(R.color.white))
            topBar.setBackgroundColor(resources.getColor(R.color.easy_a_blue))
            bottomNavigation.setBackgroundColor(resources.getColor(R.color.easy_a_blue))
        } else {
            mainLayout.setBackgroundColor(resources.getColor(R.color.dark_gray))
            topBar.setBackgroundColor(resources.getColor(R.color.black))
            bottomNavigation.setBackgroundColor(resources.getColor(R.color.black))
        }

        navigateToFragment(HomeFragment())

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.home -> {
                    navigateToFragment(HomeFragment())
                    true
                }
                R.id.add -> {
                    navigateToFragment(StudyListFragment())
                    true
                }
                R.id.progress -> {
                    navigateToFragment(ProgressChartFragment())
                    true
                }
                R.id.view -> {
                    navigateToFragment(EZFragment())
                    true
                }
                R.id.account -> {
                    navigateToFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }
    }


    private fun scheduleSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Only run when connected to the network
            .build()

        val syncWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(syncWorkRequest)
    }

    // Function to request notification permission
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, schedule your notifications
                scheduleEventWorker()
            } else {
                // Permission denied, handle accordingly
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun scheduleEventWorker() {
        val workRequest = OneTimeWorkRequestBuilder<EventWorker>()
            .build()

        WorkManager.getInstance(this).enqueue(workRequest)

        // Observe the work state to re-enqueue the worker after completion
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(workRequest.id)
            .observe(this) { workInfo ->
                if (workInfo != null && workInfo.state == WorkInfo.State.SUCCEEDED) {
                    // Optionally handle success
                    // For example, you might want to show a Toast or Log
                    Log.d("EventWorker", "Worker completed successfully")
                    // You can call the function again if you want to repeat the process later.
                    // scheduleTestEventWorker() // Uncomment if you want to repeat under certain conditions.
                }
            }
    }

    private fun getScreenHeight(): Int {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }

    private fun setRelativeSizes() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Calculate the height based on a percentage of the screen height
        val screenHeight = getScreenHeight()
        val heightPercentage = 0.15 // Adjust this percentage as needed
        val height = (screenHeight * heightPercentage).toInt()

        // Set the height of the BottomNavigationView
        val layoutParams = bottomNavigationView.layoutParams
        layoutParams.height = height
        bottomNavigationView.layoutParams = layoutParams

        // Calculate the icon size based on a percentage of the screen height
        val iconSizePercentage = 0.03 // Adjust this percentage as needed
        val iconSize = (screenHeight * iconSizePercentage).toInt()

        // Set the size of the icons in the BottomNavigationView
        bottomNavigationView.itemIconSize = iconSize
    }

    private fun navigateToFragment(fragment: Fragment) {
        // Replace the current fragment with the new fragment
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun navigateToActivity(activityClass: Class<*>) {
        // Start the new activity
        val intent = Intent(this, activityClass)
        startActivity(intent)
    }

    // Activity Viewer Fragment back button
    fun btnBackClicked(view: View) {
        //navigateToFragment(ViewWorkoutFragment())
    }

    // Function to load the profile picture, apply circular transformation, and resize it
    private fun loadProfilePicture(url: String?) {
        Log.d("RegisterScreen", "" + url)
        val desiredSize = 250 // Adjust this value for a larger image size

        if (url != null && url.isNotEmpty()) {
            Picasso.get()
                .load(url)
                .placeholder(R.drawable.avatar)  // Placeholder image while loading
                .error(R.drawable.avatar)        // Fallback image in case of error
                .resize(desiredSize, desiredSize) // Resize the image to desired size
                .into(profileIcon)
        // Load image into the profileIcon
        } else {
            // If URL is null or empty, show the default avatar
            profileIcon.setImageResource(R.drawable.avatar)
        }
    }

    fun profileIconClicked(view: View) {
        navigateToFragment(AccountFragment())
    }

    override fun onProfileUpdated(newProfilePictureUrl: String?) {
        // Load the new profile picture using Picasso
        loadProfilePicture(newProfilePictureUrl)
    }
}