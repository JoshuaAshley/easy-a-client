package com.example.proactive_opsc7311_poe.controllers

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proactive_opsc7311_poe.R
import com.example.proactive_opsc7311_poe.models.Exercise
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.type.Date
import java.util.Calendar

class ViewExercisesFragment : Fragment(), OnExerciseClickListener, OnLogTimeClickListener
{

    private lateinit var exerciseRecyclerView: RecyclerView
    private lateinit var exerciseAdapter: ExerciseAdapter
    private lateinit var exerciseName: TextView
    private lateinit var backButton: ImageButton
    private lateinit var startExercise: Button

    private val exercises = mutableListOf<Exercise>()

    private var workoutID = ""

    private val db = Firebase.firestore

    private lateinit var helpButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View?
    {
        val view = inflater.inflate(R.layout.view_exercises_fragment, container, false)

        exerciseRecyclerView = view.findViewById(R.id.recyclerViewExercises)

        exerciseName = view.findViewById(R.id.exerciseName)

        exerciseAdapter = ExerciseAdapter(exercises, this, this)

        exerciseRecyclerView.adapter = exerciseAdapter

        workoutID = arguments?.getString("workout_id") ?: ""

        readData(workoutID)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        backButton = view.findViewById(R.id.btnBack)
        backButton.setOnClickListener {
            btnBackClicked(this)
        }

        startExercise = view.findViewById(R.id.btnStartExercise)
        startExercise.setOnClickListener {
            btnStartExerciseClicked()
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
                "help_title_exercise_page", "help_content_exercise_page", requireContext()
            )
        )
    }

    override fun onExerciseClicked(exerciseID: String)
    {
        val fragment = ViewExerciseFragment().apply {
            arguments = Bundle().apply {
                putString("exercise_id", exerciseID)
                putString("workout_id", workoutID)
            }
        }
        parentFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment)
            .addToBackStack(null).commit()
    }

    override fun onLogTimeClicked(exerciseID: String)
    {
        val fragment = LoggedTimeFragment().apply {
            arguments = Bundle().apply {
                putString("exercise_id", exerciseID)
                putString("workout_id", workoutID)
            }
        }
        parentFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment)
            .addToBackStack(null).commit()
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

    private fun readData(workoutID: String)
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

                                    val workoutName = workoutDocument.getString("name") ?: ""

                                    exerciseName.text = workoutName

                                    val workoutDocRef = workoutDocument.reference

                                    workoutDocRef.collection("exercises").get()
                                        .addOnSuccessListener { exercisesSnapshot ->
                                            exercises.clear()
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
                                                    exerciseDocument.getLong("min")?.toDouble()
                                                        ?: 0.00
                                                val exerciseMax =
                                                    exerciseDocument.getLong("max")?.toDouble()
                                                        ?: 0.00
                                                val exerciseLoggedTime =
                                                    exerciseDocument.getLong("loggedTime")
                                                        ?.toDouble() ?: 0.00
                                                val exerciseGoalsMet =
                                                    exerciseDocument.getBoolean("goalsMet") ?: false

                                                val exercise = Exercise(
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
                                                exercises.add(exercise)
                                            }
                                            updateRecyclerView(exercises)
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

    private fun updateRecyclerView(exercises: List<Exercise>)
    {
        val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerViewExercises)
        val adapter = ExerciseAdapter(exercises, this, this)
        recyclerView?.adapter = adapter
        recyclerView?.layoutManager = LinearLayoutManager(context)
    }

    private fun btnBackClicked(fragment: Fragment)
    {
        navigateToFragment(ViewWorkoutFragment())
    }

    private fun btnStartExerciseClicked()
    {
        val fragment = AddExerciseFragment().apply {
            arguments = Bundle().apply {
                putString("workout_id", workoutID)
            }
        }
        parentFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment)
            .addToBackStack(null).commit()
    }

    private fun navigateToFragment(fragment: Fragment)
    {
        // Replace the current fragment with the new fragment
        parentFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit()
    }
}