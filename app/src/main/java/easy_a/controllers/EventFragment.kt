package easy_a.controllers

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import easy_a.application.R
import easy_a.models.EventResponse
import easy_a.models.EventResult
import easy_a.models.offlineDB.EasyDatabase
import easy_a.models.offlineDB.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EventFragment : Fragment(), CalendarAdapter.OnDayClickListener {

    private lateinit var username: TextView
    private lateinit var sessionManager: SessionManager

    private lateinit var calendarRecyclerView: RecyclerView
    private val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    private var currentMonthIndex = Calendar.getInstance().get(Calendar.MONTH)
    private var currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private lateinit var btnDueDate: Button
    private lateinit var btnAddEvent: Button

    private lateinit var eventName: EditText

    private var events = mutableListOf<EventResult>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.event_fragment, container, false)

        // Set up session manager and username
        username = view.findViewById(R.id.username)
        sessionManager = SessionManager(requireContext())
        val firstName = sessionManager.getFirstName() ?: ""
        val lastName = sessionManager.getLastName() ?: ""
        val email = sessionManager.getEmail() ?: ""
        username.text = if (firstName.isEmpty()) email else "$firstName $lastName"

        // Set up RecyclerView
        calendarRecyclerView = view.findViewById(R.id.calendar_recycler_view)
        calendarRecyclerView.layoutManager = GridLayoutManager(requireContext(), 7)
        val adapter = CalendarAdapter(requireContext(), mutableListOf(), mutableListOf(), this, currentYear, currentMonthIndex)
        calendarRecyclerView.adapter = adapter

        updateIconsForMonth(adapter)

        val btnPreviousMonth = view.findViewById<ImageButton>(R.id.btnPreviousMonth)
        val btnNextMonth = view.findViewById<ImageButton>(R.id.btnNextMonth)
        val monthTitle = view.findViewById<TextView>(R.id.monthTitle)

        val title = view.findViewById<TextView>(R.id.titleTextView)
        val add = view.findViewById<TextView>(R.id.addTextView)
        val event = view.findViewById<TextView>(R.id.eventName)

        if (!sessionManager.isDarkMode()) {
            // Set all text elements to black for light mode
            title.setTextColor(resources.getColor(R.color.black))
            add.setTextColor(resources.getColor(R.color.black))
            event.background = resources.getDrawable(R.drawable.textfield_light)
        }

        eventName = view.findViewById(R.id.eventName)
        btnDueDate = view.findViewById(R.id.btnDueDate)

        monthTitle.text = months[currentMonthIndex]

        btnDueDate.setOnClickListener {
            showDatePickerDialog()
        }

        btnPreviousMonth.setOnClickListener {
            if (currentMonthIndex > 0) {
                currentMonthIndex--
            } else {
                currentMonthIndex = 11
                currentYear--
            }
            monthTitle.text = months[currentMonthIndex]
            updateIconsForMonth(adapter)
        }

        btnNextMonth.setOnClickListener {
            if (currentMonthIndex < months.size - 1) {
                currentMonthIndex++
            } else {
                currentMonthIndex = 0
                currentYear++
            }
            monthTitle.text = months[currentMonthIndex]
            updateIconsForMonth(adapter)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnAddEvent = view.findViewById(R.id.btnAddEvent)
        btnAddEvent.setOnClickListener {
            btnAddEventClicked(this)
        }
    }

    private fun btnAddEventClicked(fragment: Fragment) {
        val eventName = eventName.text.toString().trim()
        val eventDueDate = btnDueDate.text.toString().trim()

        // Validate inputs
        when {
            eventName.isEmpty() -> {
                Toast.makeText(requireContext(), "Please enter an event name.", Toast.LENGTH_SHORT).show()
                return
            }
            eventDueDate.isEmpty() -> {
                Toast.makeText(requireContext(), "Please enter an event due date.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val event = Event(
            eventName = eventName,
            eventDescription = "", // Add description if needed
            eventDueDate = eventDueDate,
            synced = false // Not synced initially
        )

        if (isNetworkAvailable()) {
            // Online: Directly upload to API
            uploadEventToAPI(event)
        } else {
            // Offline: Save locally
            saveEventLocally(event)
            Toast.makeText(requireContext(), "Event saved locally. Will sync when connected.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveEventLocally(event: Event) {
        // Save event to Room Database
        lifecycleScope.launch {
            val db = EasyDatabase.getDatabase(requireContext())
            db.EasyDao().insertEvent(event)
        }
    }

    private fun uploadEventToAPI(event: Event) {
        // Create RequestBody instances for the API call
        val uidRequestBody = RequestBody.create("text/plain".toMediaTypeOrNull(), sessionManager.getUid()!!)
        val eventNameRequestBody = RequestBody.create("text/plain".toMediaTypeOrNull(), event.eventName)
        val eventDueDateRequestBody = RequestBody.create("text/plain".toMediaTypeOrNull(), event.eventDueDate)

        RetrofitClient.apiService.createEvent(
            uidRequestBody,
            eventNameRequestBody,
            eventDueDateRequestBody
        ).enqueue(object : Callback<EventResult> {
            override fun onResponse(call: Call<EventResult>, response: Response<EventResult>) {
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Event created successfully!", Toast.LENGTH_SHORT).show()
                    navigateToFragment(HomeFragment())
                } else {
                    Toast.makeText(requireContext(), "Failed to create event: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<EventResult>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showDatePickerDialog() {
        // Get the current date
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Create a DatePickerDialog
        val datePickerDialog = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            // Format the selected date and display it in the EditText
            val selectedDate = "$selectedYear-${selectedMonth + 1}-$selectedDay"
            btnDueDate.text = selectedDate
        }, year, month, day)

        // Show the dialog
        datePickerDialog.show()
    }

    private fun updateIconsForMonth(adapter: CalendarAdapter) {
        val daysInMonth = getDaysInMonth(currentYear, currentMonthIndex)
        val startDayOffset = getStartDayOffset(currentYear, currentMonthIndex)
        val totalDays = daysInMonth + startDayOffset

        val dayImages = MutableList(totalDays) { R.drawable.no_icon }
        val dayVisibility = MutableList(totalDays) { View.VISIBLE }

        val calendar = Calendar.getInstance()
        calendar.set(currentYear, currentMonthIndex, 1)

        // Format the start and end dates as strings
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startDate = dateFormat.format(calendar.time)

        calendar.set(currentYear, currentMonthIndex, daysInMonth, 23, 59, 59)
        val endDate = dateFormat.format(calendar.time)

        val uid = sessionManager.getUid() // Get the user ID
        Log.e("EventFragment", "Requesting events for: $startDate to $endDate")

        if (!isNetworkAvailable()) {
            // Fetch events from local storage when offline
            CoroutineScope(Dispatchers.IO).launch {
                val localEvents =
                    EasyDatabase.getDatabase(requireContext()).EasyDao().getEventsByDateRange(startDate, endDate)

                withContext(Dispatchers.Main) {
                    // Update icons with local events
                    for (i in 0 until daysInMonth) {
                        val index = i + startDayOffset
                        if (index < dayImages.size) {
                            calendar.set(currentYear, currentMonthIndex, i + 1)
                            val dateString =
                                dateFormat.format(calendar.time) // Format current date as string

                            // Check if the local event date matches the formatted date string
                            if (localEvents.any { it.eventDueDate == dateString }) {
                                dayImages[index] = R.drawable.event_day_icon
                            } else {
                                dayImages[index] = R.drawable.no_icon
                            }
                        }
                    }

                    // Update visibility for extra days
                    for (i in 0 until startDayOffset) {
                        dayVisibility[i] = View.GONE
                    }
                    for (i in startDayOffset + daysInMonth until totalDays) {
                        dayVisibility[i] = View.GONE
                    }

                    // Notify the adapter of changes
                    adapter.updateDayImages(dayImages, dayVisibility)
                }
            }
        }

        // API call to get events for the month range
        RetrofitClient.apiService.getEventsByMonthRange(uid!!, startDate, endDate).enqueue(
            object : Callback<EventResponse> {
                @SuppressLint("NotifyDataSetChanged")
                override fun onResponse(
                    call: Call<EventResponse>,
                    response: Response<EventResponse>
                ) {
                    if (response.isSuccessful) {
                        events = (response.body()?.eventList?.toMutableList() ?: emptyList()).toMutableList()
                        Log.e("EventFragment", "Fetched events: $events")

                        // Update icons after fetching events
                        for (i in 0 until daysInMonth) {
                            val index = i + startDayOffset
                            if (index < dayImages.size) {
                                calendar.set(currentYear, currentMonthIndex, i + 1)
                                val dateString = dateFormat.format(calendar.time) // Format current date as string
                                // Check if the event date matches the formatted date string
                                if (events.any { it.eventDate == dateString }) {
                                    dayImages[index] = R.drawable.event_day_icon
                                } else {
                                    dayImages[index] = R.drawable.no_icon
                                }
                            }
                        }

                        // Update visibility for extra days
                        for (i in 0 until startDayOffset) {
                            dayVisibility[i] = View.GONE
                        }
                        for (i in startDayOffset + daysInMonth until totalDays) {
                            dayVisibility[i] = View.GONE
                        }

                        // Notify the adapter of changes
                        adapter.updateDayImages(dayImages, dayVisibility)

                    } else {
                        // Log the error if the response isn't successful
                        Log.e("EventFragment", "Error: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<EventResponse>, t: Throwable) {
                    // Log the error when the call fails
                    Log.e("EventFragment", "API call failed: ${t.message}", t)
                }
            }
        )
    }

    fun isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun getDaysInMonth(year: Int, month: Int): Int {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1)
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    private fun getStartDayOffset(year: Int, month: Int): Int {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1)
        return calendar.get(Calendar.DAY_OF_WEEK) - 1
    }

    private fun showDayPopup(position: Int) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_day_details, null)

        val calendar = Calendar.getInstance()
        calendar.set(currentYear, currentMonthIndex, 1)
        calendar.add(Calendar.DAY_OF_MONTH, position - getStartDayOffset(currentYear, currentMonthIndex)) // Adjust the position based on offset

        val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formattedDate = dayFormat.format(calendar.time)

        val dayDetailsTitle = dialogView.findViewById<TextView>(R.id.dayDetailsTitle)
        val dayDetailsMessage = dialogView.findViewById<TextView>(R.id.dayDetailsMessage)

        dayDetailsTitle.text = "Events for $formattedDate"

        val eventsListView = dialogView.findViewById<ListView>(R.id.eventsListView)

        if (!isNetworkAvailable()) {
            fetchEventsFromLocalDB(formattedDate, dayDetailsMessage, eventsListView)
        }
        else {
            // API call to get events for the selected date
            val uid = sessionManager.getUid() // Get the user ID
            RetrofitClient.apiService.getEventsByDate(uid!!, formattedDate).enqueue(
                object : Callback<EventResponse> {
                    override fun onResponse(call: Call<EventResponse>, response: Response<EventResponse>) {
                        if (response.isSuccessful) {
                            val events = response.body()?.eventList ?: emptyList()
                            if (events.isNotEmpty()) {
                                // Prepare a simple adapter for the ListView
                                val eventTitles = events.map { it.eventName }
                                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, eventTitles)
                                eventsListView.adapter = adapter
                            } else {
                                dayDetailsMessage.text = "No events found for this date."
                            }
                        } else {
                            dayDetailsMessage.text = "Error fetching events: ${response.errorBody()?.string()}"
                        }
                    }

                    override fun onFailure(call: Call<EventResponse>, t: Throwable) {
                        dayDetailsMessage.text = "API call failed: ${t.message}"
                    }
                }
            )
        }

        // Create and show the dialog
        val builder = AlertDialog.Builder(requireContext())
        builder.setView(dialogView)
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        builder.create().show()
    }

    private fun fetchEventsFromLocalDB(date: String, messageView: TextView, eventsListView: ListView) {
        // Launch a coroutine to fetch events from the local database
        CoroutineScope(Dispatchers.Main).launch {
            val localEvents =
                EasyDatabase.getDatabase(requireContext()).EasyDao().getEventsByDate(date)

            if (localEvents.isNotEmpty()) {
                // Prepare a simple adapter for the ListView
                val eventTitles = localEvents.map { it.eventName }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, eventTitles)
                eventsListView.adapter = adapter
            } else {
                messageView.text = "No events found for this date."
            }
        }
    }

    override fun onDayClick(position: Int) {
        showDayPopup(position)
    }

    private fun navigateToFragment(fragment: Fragment) {
        // Replace the current fragment with the new fragment
        parentFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit()
    }
}