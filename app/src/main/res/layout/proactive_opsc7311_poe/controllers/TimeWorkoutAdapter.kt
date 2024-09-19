package com.example.proactive_opsc7311_poe.controllers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proactive_opsc7311_poe.R
import com.example.proactive_opsc7311_poe.models.Workout
import java.text.DecimalFormat

class TimeWorkoutAdapter(private val workouts: List<Workout>) :
    RecyclerView.Adapter<TimeWorkoutAdapter.WorkoutViewHolder>()
{
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder
    {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_time_workout, parent, false)
        return WorkoutViewHolder(view)
    }

    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int)
    {
        val workout = workouts[position]
        holder.bind(workout)
    }

    override fun getItemCount() = workouts.size

    inner class WorkoutViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        private val titleTextView: TextView = itemView.findViewById(R.id.textViewWorkoutTitle)
        private val descriptionTextView: TextView =
            itemView.findViewById(R.id.textViewWorkoutDescription)
        private val timeTextView: TextView = itemView.findViewById(R.id.textViewTotalTime)

        fun bind(workout: Workout)
        {
            titleTextView.text = workout.getName()
            descriptionTextView.text = workout.getDescription()
            var totalTime = workout.getTotalLoggedTime()

            if (totalTime >= 60)
            {
                val hours = totalTime / 60.00
                val formatter = DecimalFormat("0.00")
                val formattedHours = formatter.format(hours)
                timeTextView.text = "Total time logged: $formattedHours hour/s"
            } else
            {
                val mins = workout.totalLoggedTime
                val formatter = DecimalFormat("0.00")
                val formattedMins = formatter.format(mins)
                timeTextView.text = "Total time logged: " + formattedMins + " min/s"
            }

        }
    }
}