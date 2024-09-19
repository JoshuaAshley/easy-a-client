package com.example.proactive_opsc7311_poe.controllers

import android.content.ContentValues
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proactive_opsc7311_poe.R
import com.example.proactive_opsc7311_poe.models.Exercise
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import com.squareup.picasso.Picasso

class TimeExerciseAdapter(private val exercises: List<Exercise>) :
    RecyclerView.Adapter<TimeExerciseAdapter.WorkoutViewHolder>()
{
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder
    {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_date_exercise, parent, false)
        return WorkoutViewHolder(view)
    }

    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int)
    {
        val exercise = exercises[position]
        holder.bind(exercise)
    }

    override fun getItemCount() = exercises.size

    inner class WorkoutViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        private val titleTextView: TextView = itemView.findViewById(R.id.textViewExerciseTitle)
        private val descriptionTextView: TextView =
            itemView.findViewById(R.id.textViewExerciseDescription)
        private val progressImage: ImageView = itemView.findViewById(R.id.progressImage)

        fun bind(exercise: Exercise)
        {
            titleTextView.text = exercise.getName()
            descriptionTextView.text = exercise.getDescription()

            // Check if the user has a profile picture URL
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
                .into(progressImage, object : com.squareup.picasso.Callback
                {
                    override fun onSuccess()
                    {
                        // Image loaded successfully
                        progressImage.visibility = View.VISIBLE
                        progressImage.scaleType = ImageView.ScaleType.CENTER_CROP
                        progressImage.clipToOutline =
                            true // Clip the image to the outline of the background
                    }

                    override fun onError(e: Exception?)
                    {
                        // Handle error loading image
                        Log.e(ContentValues.TAG, "Error loading image", e)
                    }
                })
        }
    }
}