package com.example.proactive_opsc7311_poe.controllers

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proactive_opsc7311_poe.R
import com.example.proactive_opsc7311_poe.models.Workout
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment(), OnWorkoutClickListener
{

    private val db = Firebase.firestore

    private val workouts = mutableListOf<Workout>()

    private lateinit var helpButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View?
    {
        val view = inflater.inflate(R.layout.home_fragment, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewWorkouts)

        // Assuming you have an adapter class named 'WorkoutAdapter'
        val adapter = WorkoutAdapter(workouts, this)

        recyclerView.adapter = adapter
        recyclerView.layoutManager =
            LinearLayoutManager(context) // or GridLayoutManager(context, numberOfColumns)

        // Set the text for date progress
        val dateProgress = view.findViewById<TextView>(R.id.date_progress)
        dateProgress.text = "Todays Progress - ${getCurrentDate()}"

        readData()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        helpButton = view.findViewById(R.id.btnHelp)
        helpButton.setOnClickListener {
            btnHelpClicked()
        }
    }

    private fun btnHelpClicked()
    {
        navigateToFragment(
            HelpFragment(
                "help_title_home_page",
                "help_content_home_page",
                requireContext()
            )
        )
    }

    private fun navigateToFragment(fragment: Fragment)
    {
        // Replace the current fragment with the new fragment
        parentFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit()
    }

    override fun onWorkoutClicked(workoutName: String)
    {
        val fragment = ViewExercisesFragment().apply {
            arguments = Bundle().apply {
                putString("workout_id", workoutName)
            }
        }
        parentFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment)
            .addToBackStack(null).commit()
    }

    // Function to get current date in readable format
    private fun getCurrentDate(): String
    {
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun readData()
    {
        val user = FirebaseAuth.getInstance().currentUser
        // Simplified date format without time
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        Log.w("readData", "Current Date " + currentDate)

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

                        // Check if the "workouts" collection exists for the user
                        userDocRef.collection("workouts").get()
                            .addOnSuccessListener { workoutsSnapshot ->
                                for (workoutDocument in workoutsSnapshot.documents)
                                {
                                    val workoutId = workoutDocument.getString("workoutID") ?: ""
                                    val workoutProgress =
                                        workoutDocument.getLong("progress")?.toInt() ?: 0
                                    val totalExercises =
                                        workoutDocument.getLong("total-exercises")?.toInt() ?: 0

                                    // Check if the workout is completed, if so, skip adding it to the list
                                    if (workoutProgress != totalExercises)
                                    {
                                        // Query the 'exercises' subcollection
                                        userDocRef.collection("workouts").document(workoutId)
                                            .collection("exercises").get()
                                            .addOnSuccessListener { exercisesSnapshot ->
                                                if (!exercisesSnapshot.isEmpty)
                                                {
                                                    for (exerciseDocument in exercisesSnapshot.documents)
                                                    {
                                                        val exerciseDate =
                                                            exerciseDocument.getTimestamp("date")
                                                                ?.toDate()?.let { date ->
                                                                dateFormat.format(date)
                                                            }

                                                        if (currentDate == exerciseDate.toString())
                                                        {
                                                            val workoutName =
                                                                workoutDocument.getString("name")
                                                                    ?: ""
                                                            val workoutDescription =
                                                                workoutDocument.getString("description")
                                                                    ?: ""

                                                            val newWorkout = Workout(
                                                                workoutId,
                                                                workoutName,
                                                                workoutDescription,
                                                                workoutProgress,
                                                                totalExercises
                                                            )

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
                                                    }
                                                    updateRecyclerView(workouts)
                                                }
                                            }
                                    }
                                }
                            }.addOnFailureListener { e ->
                                // Handle any errors
                                Log.w("readData", "Error getting workouts: ", e)
                            }
                    }
                }
        }
    }

    private fun updateRecyclerView(workouts: List<Workout>)
    {
        val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerViewWorkouts)
        val adapter = WorkoutAdapter(workouts, this)
        recyclerView?.adapter = adapter
        recyclerView?.layoutManager = LinearLayoutManager(context)
    }
}
