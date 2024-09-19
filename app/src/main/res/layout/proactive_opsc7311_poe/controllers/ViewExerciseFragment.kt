package com.example.proactive_opsc7311_poe.controllers

import android.content.ContentValues
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.proactive_opsc7311_poe.R
import com.example.proactive_opsc7311_poe.models.Exercise
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import com.google.type.Date
import com.squareup.picasso.Picasso
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ViewExerciseFragment : Fragment()
{
    private lateinit var username: TextView
    private lateinit var exerciseName: TextView
    private lateinit var progressPhotoView: ImageView
    private lateinit var date: TextView
    private lateinit var exerciseDate: TextView
    private lateinit var doneBackground: LinearLayout
    private lateinit var doneImage: ImageView
    private lateinit var done: TextView
    private lateinit var time: TextView
    private lateinit var category: TextView
    private lateinit var categoryDescription: TextView
    private lateinit var stats: TextView
    private lateinit var statsDescription: TextView
    private lateinit var backButton: ImageButton

    private val db = Firebase.firestore

    private var workoutID = ""
    private var exerciseID = ""

    private lateinit var helpButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View?
    {
        val view = inflater.inflate(R.layout.view_exercise_fragment, container, false)

        username = view.findViewById(R.id.username)
        exerciseName = view.findViewById(R.id.exerciseName)
        progressPhotoView = view.findViewById(R.id.progressPhotoView)
        date = view.findViewById(R.id.date)
        exerciseDate = view.findViewById(R.id.exerciseDate)
        doneBackground = view.findViewById(R.id.doneBackground)
        doneImage = view.findViewById(R.id.doneImage)
        done = view.findViewById(R.id.done)
        time = view.findViewById(R.id.time)
        category = view.findViewById(R.id.category)
        categoryDescription = view.findViewById(R.id.categoryDescription)
        stats = view.findViewById(R.id.stats)
        statsDescription = view.findViewById(R.id.statsDescription)

        progressPhotoView.clipToOutline = true

        workoutID = arguments?.getString("workout_id") ?: ""
        exerciseID = arguments?.getString("exercise_id") ?: ""

        readData(exerciseID, workoutID)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        backButton = view.findViewById(R.id.btnBack)
        backButton.setOnClickListener {
            btnBackClicked(this)
        }

        helpButton = view.findViewById(R.id.btnHelp)
        helpButton.setOnClickListener {
            btnHelpClicked()
        }
    }

    private fun btnHelpClicked()
    {
        navigateToFragment(
            HelpFragment(
                "help_title_view_exercise_stats",
                "help_content_view_exercise_stats",
                requireContext()
            )
        )
    }

    private fun populateComponents(exercise: Exercise)
    {
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())

        // Convert com.google.type.DateTime to java.util.Date
        val calendar = Calendar.getInstance()
        calendar.set(
            exercise.date.year, exercise.date.month - 1, // Calendar.MONTH is zero-based
            exercise.date.day
        )
        val date = calendar.time

        exerciseName.text = exercise.name
        exerciseDate.text = dateFormat.format(date)

        // Check if the loggedTime is not null and not empty
        if (exercise.loggedTime != null && exercise.loggedTime > 0.00)
        {
            // Set the background tint to green and the icon to done_true
            doneBackground.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2AD300"))
            doneImage.setImageResource(R.drawable.done_true)

            var time = ""

            if (exercise.loggedTime >= 60.00)
            {
                val hours = exercise.loggedTime / 60.00
                val formatter = DecimalFormat("0.00")
                val formattedHours = formatter.format(hours)
                time = formattedHours + " hour/s"
            } else
            {
                val mins = exercise.loggedTime
                val formatter = DecimalFormat("0.00")
                val formattedMins = formatter.format(mins)
                time = formattedMins + " min/s"
            }

            this.time.text = time
        } else
        {
            exercise.loggedTime = 0.00
        }

        if ((exercise.loggedTime < exercise.max && exercise.loggedTime > exercise.min) && exercise.loggedTime > 0.00)
        {
            exercise.isGoalsMet = true
            statsDescription.text = "Daily Goals were met. Well Done!"
        } else
        {
            if (!(exercise.loggedTime < exercise.max && exercise.loggedTime > exercise.min) && exercise.loggedTime > 0.00)
            {
                exercise.isGoalsMet = false
                statsDescription.text = "Daily Goals have not been met."
            } else
            {
                // Set the background tint to proactive red and the icon to done_false
                doneBackground.backgroundTintList =
                    ColorStateList.valueOf(Color.parseColor("#FF0000")) // proactive red color code
                doneImage.setImageResource(R.drawable.done_false)
                time.text = "0 mins"
                statsDescription.text = "Daily Goals have not been met yet."
            }
        }

        categoryDescription.text = exercise.category

        val progressPictureUrl = exercise.image

        if (progressPictureUrl.isNotEmpty())
        {
            val storageRef = Firebase.storage.reference

            // Get the reference to the image file in Firebase Storage
            val progressImageRef = storageRef.child(progressPictureUrl)

            // Get the download URL for the image
            progressImageRef.downloadUrl.addOnSuccessListener { uri ->
                // Call loadProfilePicture with the URL if the URI is not null and not empty
                if (uri.toString().isNotEmpty())
                {
                    loadProgressPicture(uri.toString())
                } else
                {
                    // Handle the case when the download URL is empty
                    Log.d(ContentValues.TAG, "Progress picture URL is empty.")
                    // You can set a default profile picture or handle it according to your app's logic
                }
            }.addOnFailureListener { exception ->
                Log.e(ContentValues.TAG, "Error getting download URL", exception)
                // Handle the failure to get the download URL
            }
        }
    }

    private fun loadProgressPicture(imageUrl: String)
    {
        // Load image with Picasso
        Picasso.get().load(imageUrl) // Add an error placeholder image
            .into(progressPhotoView, object : com.squareup.picasso.Callback
            {
                override fun onSuccess()
                {
                    // Image loaded successfully
                    progressPhotoView.visibility = View.VISIBLE
                    progressPhotoView.scaleType = ImageView.ScaleType.CENTER_CROP
                }

                override fun onError(e: Exception?)
                {
                    // Handle error loading image
                    Log.e(ContentValues.TAG, "Error loading image", e)
                }
            })
    }

    // Utility function to convert Timestamp to com.google.type.Date
    fun timestampToDate(timestamp: Timestamp): Date
    {
        val calendar = Calendar.getInstance()
        calendar.time = timestamp.toDate()
        return Date.newBuilder().setYear(calendar.get(Calendar.YEAR))
            .setMonth(calendar.get(Calendar.MONTH) + 1) // Calendar.MONTH is zero-based
            .setDay(calendar.get(Calendar.DAY_OF_MONTH)).build()
    }

    private fun readData(exerciseID: String, workoutID: String)
    {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let { currentUser ->
            val userId = currentUser.uid

            db.collection("users").whereEqualTo("uid", userId).get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty)
                    {
                        // There should be only one document with the given UID
                        val document = querySnapshot.documents[0]
                        val userData = document.data

                        val username = view?.findViewById<TextView>(R.id.username)

                        val firstname = userData?.get("firstname") as? String ?: ""

                        if (firstname.isNotEmpty())
                        {
                            username?.text = firstname
                        } else
                        {
                            username?.text = "User"
                        }

                        val userDocRef = document.reference

                        userDocRef.collection("workouts").whereEqualTo("workoutID", workoutID).get()
                            .addOnSuccessListener { workoutsSnapshot ->
                                if (!workoutsSnapshot.isEmpty)
                                {
                                    val workoutDocument = workoutsSnapshot.documents[0]

                                    val workoutDocRef = workoutDocument.reference

                                    workoutDocRef.collection("exercises")
                                        .whereEqualTo("exerciseID", exerciseID).get()
                                        .addOnSuccessListener { exercisesSnapshot ->
                                            val exerciseDocument = exercisesSnapshot.documents[0]

                                            val exerciseName =
                                                exerciseDocument.getString("name") ?: ""
                                            val exerciseDescription =
                                                exerciseDocument.getString("description") ?: ""
                                            val exerciseImage =
                                                exerciseDocument.getString("image") ?: ""
                                            val exerciseTimestamp =
                                                exerciseDocument.getTimestamp("date")
                                            val exerciseDate =
                                                exerciseTimestamp?.let { timestampToDate(it) }
                                                    ?: Date.getDefaultInstance()
                                            val exerciseStartTime =
                                                exerciseDocument.getTimestamp("startTime")
                                                    ?: Timestamp.now()
                                            val exerciseEndTime =
                                                exerciseDocument.getTimestamp("endTime")
                                                    ?: Timestamp.now()
                                            val exerciseCategory =
                                                exerciseDocument.getString("category") ?: ""
                                            val exerciseMin =
                                                exerciseDocument.getLong("min")?.toDouble() ?: 0.00
                                            val exerciseMax =
                                                exerciseDocument.getLong("max")?.toDouble() ?: 0.00
                                            val exerciseLoggedTime =
                                                exerciseDocument.getLong("loggedTime")?.toDouble()
                                                    ?: 0.00
                                            val exerciseGoalsMet =
                                                exerciseDocument.getBoolean("goalsMet") ?: false

                                            val newExercise = Exercise(
                                                exerciseID,
                                                exerciseName,
                                                exerciseDescription,
                                                exerciseImage,
                                                exerciseDate,
                                                exerciseStartTime,
                                                exerciseEndTime,
                                                exerciseCategory,
                                                exerciseMin,
                                                exerciseMax,
                                            )

                                            if (exerciseLoggedTime > 0.00)
                                            {
                                                newExercise.loggedTime = exerciseLoggedTime
                                                newExercise.isGoalsMet = exerciseGoalsMet
                                            }

                                            populateComponents(newExercise)
                                        }.addOnFailureListener { e ->
                                            Log.w("readData", "Error getting exercises: ", e)
                                        }
                                }
                            }.addOnFailureListener { e ->
                                Log.w("readData", "Error getting workouts: ", e)
                            }
                    }
                }
        }
    }

    private fun btnBackClicked(fragment: Fragment)
    {
        val backFragment = ViewExercisesFragment().apply {
            arguments = Bundle().apply {
                putString("workout_id", workoutID)
            }
        }
        navigateToFragment(backFragment)
    }

    private fun navigateToFragment(fragment: Fragment)
    {
        // Replace the current fragment with the new fragment
        parentFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit()
    }

}