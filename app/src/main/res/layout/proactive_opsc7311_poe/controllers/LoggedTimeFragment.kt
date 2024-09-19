package com.example.proactive_opsc7311_poe.controllers

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.proactive_opsc7311_poe.R
import com.example.proactive_opsc7311_poe.models.Exercise
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.type.Date
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class LoggedTimeFragment : Fragment()
{

    private lateinit var username: TextView
    private lateinit var exerciseName: TextView
    private lateinit var min: TextView
    private lateinit var max: TextView
    private lateinit var backButton: ImageButton
    private lateinit var exerciseTime: TextView
    private lateinit var logTime: Button
    private lateinit var loggedTime: EditText

    private var workoutID = ""
    private var exerciseID = ""

    private val db = Firebase.firestore

    private lateinit var helpButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View?
    {
        val view = inflater.inflate(R.layout.logged_time_fragment, container, false)

        username = view.findViewById(R.id.username)
        exerciseName = view.findViewById(R.id.exerciseName)
        exerciseTime = view.findViewById(R.id.exerciseTime)
        min = view.findViewById(R.id.minTime)
        max = view.findViewById(R.id.maxTime)
        loggedTime = view.findViewById(R.id.loggedTime)

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

        logTime = view.findViewById(R.id.btnLogTime)
        logTime.setOnClickListener {
            btnLogTimeClicked(this)
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
                "help_title_log_exercise_data", "help_content_log_exercise_data", requireContext()
            )
        )
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

    private fun btnLogTimeClicked(fragment: Fragment)
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

                        val userDocRef = document.reference

                        userDocRef.collection("workouts").whereEqualTo("workoutID", workoutID).get()
                            .addOnSuccessListener { workoutsSnapshot ->
                                if (!workoutsSnapshot.isEmpty)
                                {
                                    val workoutDocument = workoutsSnapshot.documents[0]

                                    val workoutProgress =
                                        workoutDocument.getLong("progress")?.toInt() ?: 0

                                    val workoutLoggedTime =
                                        workoutDocument.getLong("totalLoggedTime")?.toDouble()
                                            ?: 0.00

                                    val workoutDocRef = workoutDocument.reference

                                    workoutDocRef.collection("exercises")
                                        .whereEqualTo("exerciseID", exerciseID).get()
                                        .addOnSuccessListener { exercisesSnapshot ->
                                            val exerciseDocument = exercisesSnapshot.documents[0]

                                            val exerciseName =
                                                exerciseDocument.getString("name") ?: ""
                                            val exerciseMin =
                                                exerciseDocument.getLong("min")?.toDouble() ?: 0.00
                                            val exerciseMax =
                                                exerciseDocument.getLong("max")?.toDouble() ?: 0.00
                                            val exerciseLoggedTime =
                                                exerciseDocument.getLong("loggedTime")?.toDouble()
                                                    ?: 0.00

                                            if (exerciseLoggedTime <= 0 || exerciseLoggedTime.toString()
                                                    .isEmpty()
                                            )
                                            {
                                                workoutDocRef.update(
                                                    mapOf(
                                                        "progress" to (workoutProgress + 1),
                                                        "totalLoggedTime" to (workoutLoggedTime + loggedTime.text.toString()
                                                            .toInt())
                                                    )
                                                )
                                            }

                                            val exerciseRefDoc = exerciseDocument.reference

                                            if (loggedTime.text.toString()
                                                    .toDouble() < exerciseMax && loggedTime.text.toString()
                                                    .toDouble() > exerciseMin
                                            )
                                            {
                                                exerciseRefDoc.update(
                                                    mapOf(
                                                        "loggedTime" to loggedTime.text.toString()
                                                            .toDouble(), "goalsMet" to true
                                                    )
                                                )
                                            } else
                                            {
                                                exerciseRefDoc.update(
                                                    mapOf(
                                                        "loggedTime" to loggedTime.text.toString()
                                                            .toDouble(), "goalsMet" to false
                                                    )
                                                )
                                            }

                                            Toast.makeText(
                                                fragment.requireContext(),
                                                "Logged new time: $exerciseName",
                                                Toast.LENGTH_SHORT
                                            ).show()

                                            loggedTime.setText("")

                                            btnBackClicked(this)

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

    private fun populateComponents(exercise: Exercise)
    {
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat(
            "h:mm a", Locale.getDefault()
        ) // This will format the time as "9:00 AM"

        // Convert com.google.type.DateTime to java.util.Date
        val calendar = Calendar.getInstance()
        calendar.set(
            exercise.date.year, exercise.date.month - 1, // Calendar.MONTH is zero-based
            exercise.date.day
        )
        val date = calendar.time

        exerciseName.text = exercise.name

        // Format and set the start and end times
        val startTime = exercise.startTime.toDate() // Assuming exercise.startTime is a Timestamp
        val endTime = exercise.endTime.toDate() // Assuming exercise.endTime is a Timestamp
        exerciseTime.text =
            "From " + timeFormat.format(startTime) + " to " + timeFormat.format(endTime)

        if (exercise.min >= 60.00)
        {
            val hours = exercise.min / 60.00
            val formatter = DecimalFormat("0.00")
            val formattedHours = formatter.format(hours)
            min.text = formattedHours + " hour/s"
        } else
        {
            min.text = exercise.min.toString() + " min/s"
        }

        if (exercise.max >= 60.00)
        {
            val hours = exercise.max / 60.00
            val formatter = DecimalFormat("0.00")
            val formattedHours = formatter.format(hours)
            max.text = formattedHours + " hour/s"
        } else
        {
            max.text = exercise.max.toString() + " min/s"
        }
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