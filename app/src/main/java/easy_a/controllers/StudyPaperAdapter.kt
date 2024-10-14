package easy_a.controllers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import easy_a.application.R
import easy_a.models.QuestionPaperResponse

// Adapter for displaying a list of question papers
class StudyPaperAdapter(
    private val papers: List<QuestionPaperResponse>,
    private val clickListener: OnPaperClickListener
) : RecyclerView.Adapter<StudyPaperAdapter.PaperViewHolder>() {

    // ViewHolder to hold item views
    inner class PaperViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val paperTitle: TextView = itemView.findViewById(R.id.textViewPaperTitle)
        private val paperDescription: TextView = itemView.findViewById(R.id.textViewPaperDescription)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBarPaper)

        // Bind data to views
        fun bind(paper: QuestionPaperResponse) {
            paperTitle.text = paper.questionPaperName
            paperDescription.text = paper.questionPaperDescription

            // Calculate progress: completed questions / total questions
            val totalQuestions = paper.numQuestions ?: 0
            val completedQuestions = paper.numCompletedQuestions ?: 0

            progressBar.progress = if (totalQuestions.toInt() >= 0) {
                ((completedQuestions.toFloat() / totalQuestions.toFloat()) * 100).toInt()
            } else {
                0 // Handle the case where no questions are set
            }

            // Set click listener to notify when a paper is clicked
            itemView.setOnClickListener {
                clickListener.onPaperClicked(paper.questionPaperId)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaperViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_paper, parent, false)
        return PaperViewHolder(view)
    }

    override fun onBindViewHolder(holder: PaperViewHolder, position: Int) {
        holder.bind(papers[position]) // Bind data to the ViewHolder
    }

    override fun getItemCount(): Int = papers.size // Return the size of the paper list
}

// Interface for handling paper click events
interface OnPaperClickListener {
    fun onPaperClicked(questionPaperId: String)
}