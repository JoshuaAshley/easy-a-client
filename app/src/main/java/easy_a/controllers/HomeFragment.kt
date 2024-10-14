package easy_a.controllers

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import easy_a.application.R
import easy_a.models.QuestionPaperListResponse
import easy_a.models.QuestionPaperResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment(), OnPaperClickListener {

    private lateinit var sessionManager: SessionManager

    private lateinit var eventButton: ImageButton

    private val papers = mutableListOf<QuestionPaperResponse>()
    private lateinit var adapter: StudyPaperAdapter // Declare the adapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.home_fragment, container, false)

        // Find the date TextView
        val dateTextView = view.findViewById<TextView>(R.id.dateTextView)

        sessionManager = SessionManager(requireContext())

        // Set the current date
        val currentDate = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())
        dateTextView.text = currentDate

        val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerViewWorkouts)
        adapter = StudyPaperAdapter(papers, this) // Initialize adapter with empty list
        recyclerView?.adapter = adapter
        recyclerView?.layoutManager = LinearLayoutManager(context)

        eventButton = view.findViewById(R.id.eventButton)

        eventButton.setOnClickListener {
            eventButtonClicked(this)
        }

        readData()

        return view
    }

    private fun eventButtonClicked(fragment: Fragment) {
        navigateToFragment(EventFragment())
    }

    private fun readData() {
        val uid = sessionManager.getUid() // Get the user ID

        RetrofitClient.apiService.getHomeInfo(uid!!).enqueue(
            object : Callback<QuestionPaperListResponse> {
                @SuppressLint("NotifyDataSetChanged")
                override fun onResponse(
                    call: Call<QuestionPaperListResponse>,
                    response: Response<QuestionPaperListResponse>
                ) {
                    if (response.isSuccessful) {
                        val questionPaperList = response.body()?.questionPapers ?: emptyList()
                        Log.d("StudyListFragment", "Received question papers: $questionPaperList")

                        papers.clear()
                        papers.addAll(questionPaperList)
                        adapter.notifyDataSetChanged() // Notify the adapter that data has changed
                    } else {
                        // Log the error if the response isn't successful
                        Log.e("StudyListFragment", "Error: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<QuestionPaperListResponse>, t: Throwable) {
                    // Log the error when the call fails
                    Log.e("StudyListFragment", "API call failed: ${t.message}", t)
                }
            }
        )
    }

    private fun navigateToFragment(fragment: Fragment) {
        // Replace the current fragment with the new fragment
        parentFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit()
    }

    override fun onPaperClicked(questionPaperId: String) {
        val sharedPreferences = this.requireContext().getSharedPreferences("com.example.easy_a", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("questionPaperId", questionPaperId)
        editor.apply()
        navigateToFragment(QuestionListFragment())
    }
}