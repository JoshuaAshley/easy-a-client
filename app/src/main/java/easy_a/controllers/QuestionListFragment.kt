package easy_a.controllers

import android.annotation.SuppressLint
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
import com.example.easy_a.R
import easy_a.models.QuestionPaperListResponse
import easy_a.models.QuestionPaperResponse
import easy_a.models.QuestionResult
import easy_a.models.QuestionsListResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class QuestionListFragment : Fragment(), OnQuestionClickListener {

    private val questions = mutableListOf<QuestionResult>()
    private lateinit var adapter: QuestionAdapter // Declare the adapter

    private lateinit var username: TextView
    private lateinit var startQuestion: Button
    private lateinit var pdfView: ImageButton
    private lateinit var paperName: TextView
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.question_list_fragment, container, false)

        username = view.findViewById(R.id.username)

        sessionManager = SessionManager(requireContext())

        paperName = view.findViewById(R.id.paperName)

        val firstName = sessionManager.getFirstName() ?: ""
        val lastName = sessionManager.getLastName() ?: ""
        val fullName = "$firstName $lastName"
        val email = sessionManager.getEmail() ?: ""

        username.text = if (firstName.isEmpty()) email else fullName

        readQuestionPaper()

        val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerViewQuestions)
        adapter = QuestionAdapter(questions, this) // Initialize adapter with empty list
        recyclerView?.adapter = adapter
        recyclerView?.layoutManager = LinearLayoutManager(context)

        readQuestions()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        startQuestion = view.findViewById(R.id.btnStartQuestion)
        startQuestion.setOnClickListener {
            btnStartQuestionClicked(this)
        }

        pdfView = view.findViewById(R.id.pdfButton)
        pdfView.setOnClickListener {
            pdfButtonClicked(this)
        }
    }

    private fun pdfButtonClicked(fragment: Fragment) {
        navigateToFragment(PDFFragment())
    }

    private fun btnStartQuestionClicked(fragment: Fragment) {
        navigateToFragment(QuestionAddFragment())
    }

    private fun readQuestions()
    {
        val uid = sessionManager.getUid()
        val questionPaperId = sessionManager.getQuestionPaperId()


        RetrofitClient.apiService.getListQuestions(uid!!, questionPaperId!!).enqueue(
            object : Callback<QuestionsListResponse> {
                @SuppressLint("NotifyDataSetChanged")
                override fun onResponse(
                    call: Call<QuestionsListResponse>,
                    response: Response<QuestionsListResponse>
                ) {
                    if (response.isSuccessful) {
                        val questionPaperList = response.body()?.questions ?: emptyList()
                        Log.d("StudyListFragment", "Received question papers: $questionPaperList")

                        questions.clear()
                        questions.addAll(questionPaperList)
                        adapter.notifyDataSetChanged()
                    } else {
                        // Log the error if the response isn't successful
                        Log.e("StudyListFragment", "Error: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<QuestionsListResponse>, t: Throwable) {
                    // Log the error when the call fails
                    Log.e("StudyListFragment", "API call failed: ${t.message}", t)
                }
            }
        )
    }

    private fun readQuestionPaper()
    {
        val uid = sessionManager.getUid()
        val questionPaperId = sessionManager.getQuestionPaperId()


        RetrofitClient.apiService.getQuestionPaper(uid!!, questionPaperId!!).enqueue(
            object : Callback<QuestionPaperResponse> {
                @SuppressLint("NotifyDataSetChanged")
                override fun onResponse(
                    call: Call<QuestionPaperResponse>,
                    response: Response<QuestionPaperResponse>
                ) {
                    if (response.isSuccessful) {
                        paperName.text = response.body()?.questionPaperName.toString()
                        Log.e("StudyListFragment", "Name: ${response.body()?.questionPaperName.toString()}")
                    } else {
                        // Log the error if the response isn't successful
                        Log.e("StudyListFragment", "Error: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<QuestionPaperResponse>, t: Throwable) {
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

    override fun onQuestionClicked(questionId: String) {
        TODO("Not yet implemented")
    }
}