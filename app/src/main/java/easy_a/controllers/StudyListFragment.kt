package easy_a.controllers

import android.annotation.SuppressLint
import android.content.Context
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
import com.example.easy_a.R
import easy_a.models.QuestionPaperListResponse
import easy_a.models.QuestionPaperResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StudyListFragment : Fragment(), OnPaperClickListener {

    private val papers = mutableListOf<QuestionPaperResponse>()
    private lateinit var adapter: StudyPaperAdapter // Declare the adapter

    private lateinit var username: TextView
    private lateinit var startpaper: Button
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.study_list_fragment, container, false)

        username = view.findViewById(R.id.username)

        sessionManager = SessionManager(requireContext())

        val firstName = sessionManager.getFirstName() ?: ""
        val lastName = sessionManager.getLastName() ?: ""
        val fullName = "$firstName $lastName"
        val email = sessionManager.getEmail() ?: ""

        username.text = if (firstName.isEmpty()) email else fullName

        // Set up RecyclerView
        val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerViewPapers)
        adapter = StudyPaperAdapter(papers, this) // Initialize adapter with empty list
        recyclerView?.adapter = adapter
        recyclerView?.layoutManager = LinearLayoutManager(context)

        readData() // Load papers from the API

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        startpaper = view.findViewById(R.id.btnStartPaper)
        startpaper.setOnClickListener {
            btnStartPaperClicked(this)
        }
    }

    private fun readData() {
        val uid = sessionManager.getUid() // Get the user ID
        Log.d("StudyListFragment", "Fetching question papers for user ID: $uid")

        RetrofitClient.apiService.getListQuestionPapers(uid!!).enqueue(
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

    private fun btnStartPaperClicked(fragment: Fragment) {
        navigateToFragment(StudyCreateFragment())
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