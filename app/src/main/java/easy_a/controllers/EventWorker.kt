package easy_a.controllers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import easy_a.application.R
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Random

class EventWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    private lateinit var sessionManager: SessionManager

    override fun doWork(): Result {
        // Check current date
        val tomorrow = Calendar.getInstance()
        tomorrow.add(Calendar.DATE, 1)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val tomorrowDate = dateFormat.format(tomorrow.time)

        sessionManager = SessionManager(applicationContext)

        val uid = sessionManager.getUid()

        // Make an API call to check for events tomorrow
        val url = "https://easy-a-api-dbfghva5hkaqgsdc.southafricanorth-01.azurewebsites.net/api/Event/list/$uid/date/$tomorrowDate"
        val request = Request.Builder().url(url).build()

        if (sessionManager.isNotifications()) {
            val client = OkHttpClient()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }

                override fun onResponse(call: Call, response: Response) {
                    response.body?.string()?.let { responseData ->
                        val json = JSONObject(responseData)
                        val events = json.getJSONArray("eventList")

                        if (events.length() > 0) {
                            // If there are events for tomorrow, send a notification
                            for (i in 0 until events.length()) {

                                val event = events.getJSONObject(i)

                                val eventName = event.getString("eventName")

                                Log.d("NotificationTest", eventName)

                                sendPushNotification("Reminder", "Event '$eventName' is due tomorrow!")
                            }
                        }
                    }
                }
            })
        }

        return Result.success()
    }

    private fun sendPushNotification(title: String, message: String) {
        createNotificationChannel()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = Random().nextInt()

        val notificationBuilder = NotificationCompat.Builder(applicationContext, "event_channel_id")
            .setSmallIcon(R.mipmap.ic_easy_a_launcher_round)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        Log.d("NotificationTest", "Sending notification with title: $title and message: $message")

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channelId = "event_channel_id"
            val channelName = "Event Notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Notifications for upcoming events"
            }
            val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}