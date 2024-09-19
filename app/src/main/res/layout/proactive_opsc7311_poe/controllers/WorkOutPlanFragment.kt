package com.example.proactive_opsc7311_poe.controllers

import android.os.Bundle
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
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

class WorkOutPlanFragment : Fragment()
{

    private val db = Firebase.firestore

    private lateinit var helpButton: Button
    private lateinit var createWorkoutButton: Button
    private lateinit var backButton: ImageButton

    private lateinit var workoutNameEditText: EditText
    private lateinit var workoutDescriptionEditText: EditText
    private var userId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View?
    {
        val view = inflater.inflate(R.layout.workout_plan_fragment, container, false)

        workoutNameEditText = view.findViewById(R.id.editWorkoutName)
        workoutDescriptionEditText = view.findViewById(R.id.editDescription)

        readData()

        return view
    }

    private fun readData()
    {
        val user = FirebaseAuth.getInstance().currentUser

        user?.let { currentUser ->
            userId = currentUser.uid

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
                    }
                }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        helpButton = view.findViewById(R.id.btnHelp)
        helpButton.setOnClickListener {
            btnHelpClicked(this)
        }

        backButton = view.findViewById(R.id.btnBack)
        backButton.setOnClickListener {
            btnBackClicked(this)
        }

        createWorkoutButton = view.findViewById(R.id.btnCreateWorkout)
        createWorkoutButton.setOnClickListener {
            btnCreateWorkoutClicked(this)
        }
    }

    fun btnCreateWorkoutClicked(fragment: Fragment)
    {
        val workoutName = workoutNameEditText.text.toString().trim()
        val workoutDescription = workoutDescriptionEditText.text.toString().trim()

        // Validation for name and description length
        when
        {
            workoutName.isEmpty() || workoutDescription.isEmpty() ->
            {
                Toast.makeText(
                    fragment.requireContext(),
                    "Please enter workout details before saving.",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            workoutName.length >= 50 ->
            {
                Toast.makeText(
                    fragment.requireContext(),
                    "Please ensure workout name character length no more than 50.",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            workoutDescription.length >= 150 ->
            {
                Toast.makeText(
                    fragment.requireContext(),
                    "Please ensure workout description character length no more than 150.",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        }

        val workoutDetails = hashMapOf(
            "name" to workoutName,
            "description" to workoutDescription,
            "progress" to 0,
            "total-exercises" to 0
        )

        userId?.let { uid ->

            db.collection("users").whereEqualTo("uid", userId).get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty)
                    {
                        val userDocumentRef = querySnapshot.documents[0].reference
                        val workoutsCollectionRef = userDocumentRef.collection("workouts")

                        // Check if the user document exists before adding the workout
                        userDocumentRef.get().addOnSuccessListener { documentSnapshot ->
                                if (documentSnapshot.exists())
                                {
                                    // User document exists, add the workout details to the "workouts" collection
                                    workoutsCollectionRef.add(workoutDetails)
                                        .addOnSuccessListener { documentReference ->
                                            val workoutID = documentReference.id

                                            documentReference.update(
                                                mapOf(
                                                    "workoutID" to workoutID
                                                )
                                            )

                                            Toast.makeText(
                                                fragment.requireContext(),
                                                "Added new workout: $workoutName",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            workoutNameEditText.setText("")
                                            workoutDescriptionEditText.setText("")
                                            navigateToFragment(ViewWorkoutFragment())
                                        }.addOnFailureListener {
                                            Toast.makeText(
                                                fragment.requireContext(),
                                                "Error while adding new workout.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                } else
                                {
                                    // User document does not exist, handle accordingly
                                    Toast.makeText(
                                        fragment.requireContext(),
                                        "User document does not exist.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }.addOnFailureListener {
                                Toast.makeText(
                                    fragment.requireContext(),
                                    "Error checking user document.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                }
        }
    }

    fun btnHelpClicked(fragment: Fragment)
    {
        navigateToFragment(
            HelpFragment(
                "help_title_new_workout_plan",
                "help_content_new_workout_plan",
                requireContext()
            )
        )
    }

    fun btnBackClicked(fragment: Fragment)
    {
        navigateToFragment(ViewWorkoutFragment())
    }

    private fun navigateToFragment(fragment: Fragment)
    {
        // Replace the current fragment with the new fragment
        parentFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit()
    }
}
