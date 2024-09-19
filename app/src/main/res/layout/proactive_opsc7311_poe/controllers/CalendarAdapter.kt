package com.example.proactive_opsc7311_poe.controllers

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.proactive_opsc7311_poe.R

class CalendarAdapter(
    private val context: Context,
    private val dayImages: MutableList<Int>,
    private val dayVisibility: MutableList<Int>
) : RecyclerView.Adapter<CalendarAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.calendar_day_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dayImages[position], dayVisibility[position])
    }

    override fun getItemCount(): Int {
        return dayImages.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dayImage: ImageView = itemView.findViewById(R.id.day_image)

        fun bind(imageResource: Int, visibility: Int) {
            dayImage.setImageResource(imageResource)
            dayImage.visibility = visibility
        }
    }

    fun updateDayImages(newDayImages: List<Int>, newDayVisibility: List<Int>) {
        dayImages.clear()
        dayImages.addAll(newDayImages)
        dayVisibility.clear()
        dayVisibility.addAll(newDayVisibility)
        notifyDataSetChanged()
    }
}