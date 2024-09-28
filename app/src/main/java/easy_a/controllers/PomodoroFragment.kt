package easy_a.controllers

import android.annotation.SuppressLint
import android.app.TimePickerDialog
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
import android.widget.TimePicker
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.easy_a.R
import easy_a.models.QuestionPaperResponse
import easy_a.models.QuestionResult
import easy_a.models.QuestionUpdateResult
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar
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
    private var timeLeftInMillis: Long = 0
    private var timeChosen: Long = 0
    private var timerRunning = false
    private lateinit var chooseStudyDurationButton: TextView
    private lateinit var completeButton: TextView

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
        chooseStudyDurationButton = view.findViewById(R.id.choose_study_duration_button)
        completeButton = view.findViewById(R.id.complete_button)

        addButton.isEnabled = false

        // Set click listeners
        playButton.setOnClickListener { startTimer(timeLeftInMillis) }
        pauseButton.setOnClickListener { pauseTimer() }

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

        // Listen for study duration button click
        chooseStudyDurationButton.setOnClickListener {
            showTimePickerDialog()
        }

        completeButton.setOnClickListener {
            completeQuestion()
        }

        readQuestion()

        return view
    }

    private fun completeQuestion() {
        val uid = sessionManager.getUid()
        val questionPaperId = sessionManager.getQuestionPaperId()
        val questionId = sessionManager.getQuestionId()

        if (uid != null && questionPaperId != null && questionId != null) {
            RetrofitClient.apiService.completeQuestion(uid, questionPaperId, questionId).enqueue(
                object : Callback<QuestionUpdateResult> {
                    override fun onResponse(call: Call<QuestionUpdateResult>, response: Response<QuestionUpdateResult>) {
                        if (response.isSuccessful) {
                            Toast.makeText(requireContext(), "Question completed successfully!", Toast.LENGTH_SHORT).show()
                            navigateToFragment(QuestionListFragment())
                        } else {
                            // Log the error if the response isn't successful
                            Log.e("PomodoroFragment", "Error logging time: ${response.errorBody()?.string()}")
                        }
                    }

                    override fun onFailure(call: Call<QuestionUpdateResult>, t: Throwable) {
                        // Log the error when the call fails
                        Log.e("PomodoroFragment", "Failed to log time: ${t.message}", t)
                    }
                }
            )
        } else {
            Log.e("PomodoroFragment", "UID, QuestionPaperID, or QuestionID is null")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        back = view.findViewById(R.id.btnBack)
        back.setOnClickListener {
            btnBack(this)
        }

        addButton.setOnClickListener {
            logTimeClicked(this)
        }
    }

    private fun logTimeClicked(fragment: Fragment) {
        val timeInMinutes = TimeUnit.MILLISECONDS.toMinutes(timeChosen).toInt()
        //val timeInMinutes = 10;

        val uid = sessionManager.getUid()
        val questionPaperId = sessionManager.getQuestionPaperId()
        val questionId = sessionManager.getQuestionId()

        val timeToLog = RequestBody.create(MediaType.parse("text/plain"), timeInMinutes.toString())

        if (uid != null && questionPaperId != null && questionId != null) {
            RetrofitClient.apiService.logTime(uid, questionPaperId, questionId, timeToLog).enqueue(
                object : Callback<QuestionUpdateResult> {
                    override fun onResponse(call: Call<QuestionUpdateResult>, response: Response<QuestionUpdateResult>) {
                        if (response.isSuccessful) {
                            Toast.makeText(requireContext(), "Time logged successfully!", Toast.LENGTH_SHORT).show()
                            navigateToFragment(QuestionListFragment())
                        } else {
                            // Log the error if the response isn't successful
                            Log.e("PomodoroFragment", "Error logging time: ${response.errorBody()?.string()}")
                        }
                    }

                    override fun onFailure(call: Call<QuestionUpdateResult>, t: Throwable) {
                        // Log the error when the call fails
                        Log.e("PomodoroFragment", "Failed to log time: ${t.message}", t)
                    }
                }
            )
        } else {
            Log.e("PomodoroFragment", "UID, QuestionPaperID, or QuestionID is null")
        }
    }

    private fun showTimePickerDialog() {
        // Get the current time for default values
        val currentTime = Calendar.getInstance()
        val hour = currentTime.get(Calendar.HOUR_OF_DAY)
        val minute = currentTime.get(Calendar.MINUTE)

        // Create a TimePickerDialog to choose the study duration
        TimePickerDialog(requireContext(), { _: TimePicker, selectedHour: Int, selectedMinute: Int ->
            // Convert selected time to milliseconds
            val selectedTimeInMillis = (selectedHour * 60 * 60 * 1000 + selectedMinute * 60 * 1000).toLong()

            // Update the timer with the selected time
            timeLeftInMillis = selectedTimeInMillis
            updateTimerText(timeLeftInMillis)
            timeChosen = selectedTimeInMillis
            addButton.isEnabled = false
            addButton.alpha = 0.5f

        }, hour, minute, true).show()
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
                addButton.isEnabled = true
                addButton.alpha = 1f
            }
        }.start()

        timerRunning = true
        isPaused = false
        sessionManager.setTimerPaused(false)
        addButton.isEnabled = false // Disable the add button while the timer is running
    }

    private fun updateTimerText(timeInMillis: Long) {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis) % 60
        timerText.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun pauseTimer() {
        countDownTimer.cancel()
        timerRunning = false
        isPaused = true
        sessionManager.setTimerPaused(true)
        sessionManager.setTimeLeftInMillis(timeLeftInMillis)
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
