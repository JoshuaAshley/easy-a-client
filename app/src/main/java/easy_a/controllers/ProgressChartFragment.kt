package easy_a.controllers

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import easy_a.application.R
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import easy_a.models.ChartResponse
import easy_a.models.ChartResult
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.roundToInt

class ProgressChartFragment : Fragment() {

    private lateinit var username: TextView
    private lateinit var totalHoursTextView: TextView
    private lateinit var barChart: BarChart
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.progress_chart_fragment, container, false)

        username = view.findViewById(R.id.username)
        totalHoursTextView = view.findViewById(R.id.totalHours)
        barChart = view.findViewById(R.id.barChartGraph)

        sessionManager = SessionManager(requireContext())

        val firstName = sessionManager.getFirstName() ?: ""
        val lastName = sessionManager.getLastName() ?: ""
        val fullName = "$firstName $lastName"
        val email = sessionManager.getEmail() ?: ""

        username.text = if (firstName.isEmpty()) email else fullName

        readChart() // Fetch and display chart data

        return view
    }

    private fun readChart() {
        val uid = sessionManager.getUid()

        RetrofitClient.apiService.getStudyChart(uid!!).enqueue(
            object : Callback<ChartResponse> {
                @SuppressLint("NotifyDataSetChanged")
                override fun onResponse(
                    call: Call<ChartResponse>,
                    response: Response<ChartResponse>
                ) {
                    if (response.isSuccessful) {
                        val chartData = response.body()?.data
                        if (chartData != null) {
                            populateChart(chartData) // Populate chart with data
                            calculateTotalTime(chartData) // Calculate and show total logged time
                        }
                    } else {
                        Log.e("ProgressChartFragment", "Error: ${response}")
                    }
                }

                override fun onFailure(call: Call<ChartResponse>, t: Throwable) {
                    Log.e("ProgressChartFragment", "API call failed: ${t.message}", t)
                }
            }
        )
    }

    // Function to populate the BarChart with data
    private fun populateChart(chartData: List<ChartResult>) {
        val barEntries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()

        chartData.forEachIndexed { index, data ->
            barEntries.add(BarEntry(index.toFloat(), data.totalLoggedTime.toFloat()))
            if (!labels.contains(data.questionPaperName)) {  // Avoid duplicate labels
                labels.add(data.questionPaperName)
            }
        }

        val barDataSet = BarDataSet(barEntries, "Logged Time")
        barDataSet.colors = listOf(
            Color.BLUE, Color.RED, Color.GREEN, Color.YELLOW, Color.CYAN
        ) // Different bar colors
        barDataSet.valueTextColor = Color.WHITE
        barDataSet.valueTextSize = 16f

        val barData = BarData(barDataSet)

        barChart.data = barData
        barChart.description.isEnabled = false
        barChart.setFitBars(true)

        // Set X-axis labels (question paper names)
        val xAxis = barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textColor = Color.WHITE
        xAxis.labelRotationAngle = 0f
        xAxis.granularity = 1f // Ensure each label corresponds to a bar

        // Set other BarChart configurations
        barChart.axisLeft.textColor = Color.WHITE
        barChart.axisRight.isEnabled = false
        barChart.legend.textColor = Color.WHITE
        barChart.invalidate() // Refresh the chart with new data
    }

    // Function to calculate and display total logged time
    private fun calculateTotalTime(chartData: List<ChartResult>) {
        var totalLoggedTime = 0f

        chartData.forEach { data ->
            totalLoggedTime += data.totalLoggedTime.toFloat()
        }

        val totalTimeInMinutes = totalLoggedTime.roundToInt()
        val totalTimeInHours = totalTimeInMinutes / 60
        val displayTime = if (totalTimeInHours >= 1) {
            "$totalTimeInHours hours"
        } else {
            "$totalTimeInMinutes minutes"
        }

        totalHoursTextView.text = "$displayTime"
    }
}