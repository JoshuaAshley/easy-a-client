package easy_a.controllers

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.easy_a.R
import easy_a.models.QuestionPaperResponse
import easy_a.models.QuestionResult
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.TimeUnit

class PomodoroFragment : Fragment() {

    private lateinit var back: ImageButton
    private lateinit var sessionManager: SessionManager
    private lateinit var totalLoggedTime: TextView

    private lateinit var timerText: TextView
    private lateinit var pauseButton: ImageView
    private lateinit var playButton: ImageView
    private lateinit var addButton: ImageView
    private lateinit var countDownTimer: CountDownTimer
    private var isPaused = false
    private var timeLeftInMillis: Long = 30000 // 30 seconds example
    private var timerRunning = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.pomodoro_fragment, container, false)

        sessionManager = SessionManager(requireContext())

        totalLoggedTime = view.findViewById(R.id.preparation_time_value)

        timerText = view.findViewById(R.id.pomodoro_timer_text)
        pauseButton = view.findViewById(R.id.pause_button)
        playButton = view.findViewById(R.id.play_button)
        addButton = view.findViewById(R.id.add_button)

        if (isPaused) {
            updateTimerText(timeLeftInMillis)
        } else if (timerRunning) {
            startTimer(timeLeftInMillis)
        }

        // Set up button listeners
        playButton.setOnClickListener {
            if (!timerRunning) {
                startTimer(timeLeftInMillis)
            }
        }

        pauseButton.setOnClickListener {
            pauseTimer()
        }

        readQuestion()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        back = view.findViewById(R.id.btnBack)
        back.setOnClickListener {
            btnBack(this)
        }
    }

    private fun startTimer(timeInMillis: Long) {
        countDownTimer = object : CountDownTimer(timeInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimerText(timeLeftInMillis)
            }

            override fun onFinish() {
                timerText.text = "00:00"
                timerRunning = false
                sessionManager.setTimeLeftInMillis(0)
            }
        }.start()

        timerRunning = true
        isPaused = false
        sessionManager.setTimerPaused(false)
    }

    private fun pauseTimer() {
        countDownTimer.cancel()
        timerRunning = false
        isPaused = true
        sessionManager.setTimerPaused(true)
        sessionManager.setTimeLeftInMillis(timeLeftInMillis)
    }

    private fun updateTimerText(timeInMillis: Long) {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis) % 60
        timerText.text = String.format("%02d:%02d", minutes, seconds)
    }

    override fun onPause() {
        super.onPause()
        if (timerRunning) {
            pauseTimer()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (timerRunning) {
            pauseTimer()
        }
    }

    private fun readQuestion()
    {
        val uid = sessionManager.getUid()
        val questionPaperId = sessionManager.getQuestionPaperId()
        val questionId = sessionManager.getQuestionId()

        RetrofitClient.apiService.getQuestion(uid!!, questionPaperId!!, questionId!!).enqueue(
            object : Callback<QuestionResult> {
                @SuppressLint("NotifyDataSetChanged")
                override fun onResponse(
                    call: Call<QuestionResult>,
                    response: Response<QuestionResult>
                ) {
                    if (response.isSuccessful) {
                        Log.d("StudyListFragment", "Time: ${response.body()?.totatLoggedTime}")
                        totalLoggedTime.text = response.body()?.totatLoggedTime.toString() + " mins"
                    } else {
                        // Log the error if the response isn't successful
                        Log.e("StudyListFragment", "Error: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<QuestionResult>, t: Throwable) {
                    // Log the error when the call fails
                    Log.e("StudyListFragment", "API call failed: ${t.message}", t)
                }
            }
        )
    }

    private fun btnBack(fragment: Fragment) {
        navigateToFragment(QuestionListFragment())
    }

    private fun navigateToFragment(fragment: Fragment) {
        // Replace the current fragment with the new fragment
        parentFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit()
    }
}
