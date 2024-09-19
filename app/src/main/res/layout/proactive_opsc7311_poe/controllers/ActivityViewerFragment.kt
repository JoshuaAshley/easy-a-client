package com.example.proactive_opsc7311_poe.controllers

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proactive_opsc7311_poe.R
import com.example.proactive_opsc7311_poe.models.Exercise
import com.example.proactive_opsc7311_poe.models.Workout
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.type.Date
import java.util.Calendar

class ActivityViewerFragment : Fragment()
{

    // Reference to Firebase Firestore
    private val db = Firebase.firestore

    private val exercises = mutableListOf<Exercise>()
    private val workouts = mutableListOf<Workout>()

    private lateinit var dateRange: Button
    private lateinit var workoutRecyclerView: RecyclerView
    private lateinit var exerciseRecyclerView: RecyclerView
    private lateinit var spinnerWorkoutsExercises: Spinner
    private lateinit var helpButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View?
    {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.activity_viewer_fragment, container, false)

        // Initialize the RecyclerViews
        workoutRecyclerView = view.findViewById(R.id.workoutRecyclerView)
        exerciseRecyclerView = view.findViewById(R.id.exerciseRecyclerView)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        dateRange = view.findViewById(R.id.btnSelectDateRange)

        dateRange.setOnClickListener {
            showDateRangePickerDialog(this)
        }

        // Initialize the Spinner
        spinnerWorkoutsExercises = view.findViewById<Spinner>(R.id.spinnerWorkoutsExercises)

        // Populate the Spinner with options
        val workoutOrExerciseOptions = arrayOf("Workout", "Exercise")
        val adapter =
            ArrayAdapter(requireContext(), R.layout.spinner_item_layout, workoutOrExerciseOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerWorkoutsExercises.adapter = adapter

        helpButton = view.findViewById(R.id.btnHelp)
        helpButton.setOnClickListener {
            btnHelpClicked()
        }

        // Call the method to retrieve user data
        retrieveUserName(view)
    }

    private fun btnHelpClicked()
    {
        navigateToFragment(
            HelpFragment(
                "help_title_view_workouts_and_exercises",
                "help_content_view_workouts_and_exercises",
                requireContext()
            )
        )
    }

    private fun retrieveUserName(view: View)
    {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let { currentUser ->
            val userId = currentUser.uid
            db.collection("users").whereEqualTo("uid", userId).get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty)
                    {
                        val document = querySnapshot.documents[0]
                        val userData = document.data
                        val firstname = userData?.get("firstname") as? String ?: "User"
                        val usernameTextView = view.findViewById<TextView>(R.id.username)
                        if (firstname.isNotEmpty())
                        {
                            usernameTextView.text = firstname
                        } else
                        {
                            usernameTextView.text = "User"
                        }
                    } else
                    {
                        // Log or show no user data found
                        Toast.makeText(context, "No user data found.", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener { exception ->
                    // Handle any errors here, possibly with a Toast
                    Toast.makeText(
                        context,
                        "Failed to retrieve user data: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    // Utility function to convert Timestamp to com.google.type.Date
    fun timestampToDate(timestamp: Timestamp): com.google.type.Date
    {
        val calendar = Calendar.getInstance()
        calendar.time = timestamp.toDate()
        return com.google.type.Date.newBuilder().setYear(calendar.get(Calendar.YEAR))
            .setMonth(calendar.get(Calendar.MONTH) + 1) // Calendar.MONTH is zero-based
            .setDay(calendar.get(Calendar.DAY_OF_MONTH)).build()
    }

    private fun readWorkoutData(startDate: Timestamp, endDate: Timestamp)
    {
        Log.w("readData", startDate.toString())
        Log.w("readData", endDate.toString())
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

                        // Check if the "workouts" collection exists for the user
                        userDocRef.collection("workouts").get()
                            .addOnSuccessListener { workoutsSnapshot ->
                                workouts.clear()
                                for (workoutDocument in workoutsSnapshot.documents)
                                {
                                    val workoutDocRef = workoutDocument.reference

                                    workoutDocRef.collection("exercises")
                                        .whereGreaterThanOrEqualTo("date", startDate)
                                        .whereLessThanOrEqualTo("date", endDate).get()
                                        .addOnSuccessListener { exercisesSnapshot ->
                                            exercises.clear()
                                            for (exerciseDocument in exercisesSnapshot.documents)
                                            {
                                                val workoutId =
                                                    workoutDocument.getString("workoutID") ?: ""
                                                val workoutProgress =
                                                    workoutDocument.getLong("progress")?.toInt()
                                                        ?: 0
                                                val totalExercises =
                                                    workoutDocument.getLong("total-exercises")
                                                        ?.toInt() ?: 0
                                                val workoutName =
                                                    workoutDocument.getString("name") ?: ""
                                                val workoutDescription =
                                                    workoutDocument.getString("description") ?: ""
                                                val workoutTotalTime =
                                                    workoutDocument.getLong("totalLoggedTime")
                                                        ?.toDouble() ?: 0.00
                                                val newWorkout = Workout(
                                                    workoutId,
                                                    workoutName,
                                                    workoutDescription,
                                                    workoutProgress,
                                                    totalExercises
                                                )
                                                newWorkout.totalLoggedTime = workoutTotalTime

                                                var exists = false

                                                for (workout in workouts)
                                                {
                                                    if (workout.workoutID == newWorkout.workoutID)
                                                    {
                                                        exists = true
                                                    }
                                                }

                                                if (!exists)
                                                {
                                                    workouts.add(newWorkout)
                                                }
                                            }
                                            // Update the RecyclerView with the retrieved workouts
                                            updateWorkoutRecyclerView(workouts)
                                        }.addOnFailureListener { e ->
                                            Log.w("readData", "Error getting exercises: ", e)
                                        }
                                }
                            }
                    }
                }
        }
    }

    private fun readExercisesData(startDate: Timestamp, endDate: Timestamp)
    {
        Log.w("readData", startDate.toString())
        Log.w("readData", endDate.toString())
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

                        // Check if the "workouts" collection exists for the user
                        userDocRef.collection("workouts").get()
                            .addOnSuccessListener { workoutsSnapshot ->
                                exercises.clear()
                                for (workoutDocument in workoutsSnapshot.documents)
                                {
                                    val workoutDocRef = workoutDocument.reference

                                    workoutDocRef.collection("exercises")
                                        .whereGreaterThanOrEqualTo("date", startDate)
                                        .whereLessThanOrEqualTo("date", endDate).get()
                                        .addOnSuccessListener { exercisesSnapshot ->
                                            for (exerciseDocument in exercisesSnapshot.documents)
                                            {
                                                val exerciseID =
                                                    exerciseDocument.getString("exerciseID") ?: ""
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

                                                var exists = false

                                                for (exercise in exercises)
                                                {
                                                    if (exercise.exerciseID == newExercise.exerciseID)
                                                    {
                                                        exists = true
                                                    }
                                                }

                                                if (!exists)
                                                {
                                                    exercises.add(newExercise)
                                                }
                                            }
                                            // Update the RecyclerView with the retrieved workouts
                                            updateExerciseRecyclerView(exercises)
                                        }.addOnFailureListener { e ->
                                            Log.w("readData", "Error getting exercises: ", e)
                                        }
                                }
                            }
                    }
                }
        }
    }

    private fun updateWorkoutRecyclerView(workouts: List<Workout>)
    {
        val recyclerView = view?.findViewById<RecyclerView>(R.id.workoutRecyclerView)
        val adapter = TimeWorkoutAdapter(workouts)
        recyclerView?.adapter = adapter
        recyclerView?.layoutManager = LinearLayoutManager(context)
    }

    private fun updateExerciseRecyclerView(exercises: List<Exercise>)
    {
        val recyclerView = view?.findViewById<RecyclerView>(R.id.exerciseRecyclerView)
        val adapter = TimeExerciseAdapter(exercises)
        recyclerView?.adapter = adapter
        recyclerView?.layoutManager = LinearLayoutManager(context)
    }

    // Method to show DatePicker in Activity Viewer Fragment
    fun showDateRangePickerDialog(fragment: Fragment)
    {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // First, pick the start date
        val startDatePicker =
            DatePickerDialog(requireContext(), { _, startYear, startMonth, startDay ->
                // Create a Calendar instance for the start date
                val startCal = Calendar.getInstance()
                startCal.set(startYear, startMonth, startDay, 0, 0, 0)
                startCal.set(Calendar.MILLISECOND, 0)
                val startDate = startCal.time
                val startTimestamp = Timestamp(startDate)
                val startDateView = "$startDay/${startMonth + 1}/$startYear"

                // Then, pick the end date
                val endDatePicker =
                    DatePickerDialog(requireContext(), { _, endYear, endMonth, endDay ->
                        // Create a Calendar instance for the end date
                        val endCal = Calendar.getInstance()
                        endCal.set(endYear, endMonth, endDay, 23, 59, 59)
                        endCal.set(Calendar.MILLISECOND, 999)
                        val endDate = endCal.time
                        val endTimestamp = Timestamp(endDate)
                        val endDateView = "$endDay/${endMonth + 1}/$endYear"
                        // Handle the date chosen by the user
                        dateRange.text = "$startDateView - $endDateView"

                        // Check the selected item from the spinner and display the data accordingly
                        val activityType = spinnerWorkoutsExercises.selectedItem.toString()
                        if (activityType == "Workout")
                        {
                            workoutRecyclerView.isVisible = true
                            exerciseRecyclerView.isVisible = false
                            readWorkoutData(startTimestamp, endTimestamp)
                        } else if (activityType == "Exercise")
                        {
                            exerciseRecyclerView.isVisible = true
                            workoutRecyclerView.isVisible = false
                            readExercisesData(startTimestamp, endTimestamp)
                        }

                    }, year, month, day)

                // Set the minimum date for the end date picker to be the selected start date
                val startCalendar = Calendar.getInstance()
                startCalendar.set(startYear, startMonth, startDay)
                endDatePicker.datePicker.minDate = startCalendar.timeInMillis

                endDatePicker.setTitle("Select Second Date") // Set custom title for the end date picker dialog
                endDatePicker.show()
            }, year, month, day)

        startDatePicker.setTitle("Select Start Date")
        startDatePicker.show()
    }

    private fun navigateToFragment(fragment: Fragment)
    {
        // Replace the current fragment with the new fragment
        parentFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit()
    }
}