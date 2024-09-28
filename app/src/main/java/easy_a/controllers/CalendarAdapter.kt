package easy_a.controllers

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.easy_a.R

class CalendarAdapter(
    private val context: Context,
    private val dayImages: MutableList<Int>,
    private val dayVisibility: MutableList<Int>,
    private val listener: OnDayClickListener, // Correct type for listener
    private val currentYear: Int,
    private val currentMonth: Int
) : RecyclerView.Adapter<CalendarAdapter.ViewHolder>() {

    // Interface to handle day click events
    interface OnDayClickListener {
        fun onDayClick(position: Int) // Callback function
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.calendar_day_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dayImages[position], dayVisibility[position])

        // Handle item clicks and pass the position
        holder.itemView.setOnClickListener {
            listener.onDayClick(position)
        }
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