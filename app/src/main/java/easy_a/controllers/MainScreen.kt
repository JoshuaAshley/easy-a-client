package easy_a.controllers

import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import easy_a.application.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.ShapeAppearanceModel
import com.squareup.picasso.Picasso

interface ProfileUpdateListener {
    fun onProfileUpdated(newProfilePictureUrl: String?)
}

class MainScreen : AppCompatActivity(), ProfileUpdateListener {
    private lateinit var sessionManager: SessionManager
    private lateinit var profileIcon: ShapeableImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.main_screen)

        setRelativeSizes()

        sessionManager = SessionManager(this)
        profileIcon = findViewById(R.id.profileIcon)

        // Check if the user is logged in
        if (!sessionManager.isLoggedIn()) {
            // If not logged in, redirect to LoginScreen
            val intent = Intent(this, LoginScreen::class.java)
            startActivity(intent)
            finish()  // Close MainScreen
        } else {
            // User is logged in, fetch profile picture URL
            val profilePictureUrl = sessionManager.getProfilePictureUrl() // Fetch profile picture URL from session

            // Make the profileIcon circular
            profileIcon.shapeAppearanceModel = ShapeAppearanceModel.builder()
                .setAllCornerSizes(ShapeAppearanceModel.PILL) // PILL for circular shapes
                .build()

            // Set scaleType to make the image fit well in the circular view
            profileIcon.scaleType = ImageView.ScaleType.CENTER_CROP

            // Load the profile picture using Picasso.
            loadProfilePicture(profilePictureUrl)
        }

        //set to this on startup
        navigateToFragment(HomeFragment())

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Set listener to handle navigation item clicks
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
                    navigateToFragment(AccountFragment())
                    true
                }

                else -> false
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