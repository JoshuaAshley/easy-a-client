package easy_a.controllers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import easy_a.application.R
import com.squareup.picasso.Picasso
import easy_a.models.QuestionResult

// Adapter for displaying a list of questions
class QuestionAdapter(
    private val questions: List<QuestionResult>,
    private val clickListener: OnQuestionClickListener
) : RecyclerView.Adapter<QuestionAdapter.QuestionViewHolder>() {

    // ViewHolder to hold item views
    inner class QuestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val questionNumber: TextView = itemView.findViewById(R.id.textViewQuestionNumber)
        private val questionDescription: TextView = itemView.findViewById(R.id.textViewQuestionDescription)
        private val progressImage: ImageView = itemView.findViewById(R.id.progressImage)
        private val btnViewQuestion: Button = itemView.findViewById(R.id.btnViewQuestion)

        // Bind data to views
        fun bind(question: QuestionResult) {
            // Bind the question number
            questionNumber.text = question.questionName

            // Bind the question description
            questionDescription.text = question.questionDescription

            // Use Picasso to load the image from imageLocation URL
            if (!question.imageLocation.isNullOrEmpty()) {
                Picasso.get()
                    .load(question.imageLocation) // Load image from URL
                    .into(progressImage) // Set the image into the ImageView
                progressImage.scaleType = ImageView.ScaleType.CENTER_CROP
                progressImage.clipToOutline =
                    true
            }

            // Set click listener for the "View Question" button
            btnViewQuestion.setOnClickListener {
                clickListener.onQuestionClicked(question.questionId)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_question, parent, false)
        return QuestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        holder.bind(questions[position]) // Bind data to the ViewHolder
    }

    override fun getItemCount(): Int = questions.size // Return the size of the question list
}

// Interface for handling question click events
interface OnQuestionClickListener {
    fun onQuestionClicked(questionId: String)
}