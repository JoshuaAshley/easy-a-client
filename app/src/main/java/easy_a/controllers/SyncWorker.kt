package easy_a.controllers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import easy_a.application.R
import easy_a.models.EventResult
import easy_a.models.offlineDB.EasyDatabase
import easy_a.models.offlineDB.Event
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Random

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private lateinit var sessionManager: SessionManager

    override suspend fun doWork(): Result {
        val db = EasyDatabase.getDatabase(applicationContext)
        val unsyncedEvents = db.EasyDao().getUnsyncedEvents()

        if (isNetworkAvailable()) {
            for (event in unsyncedEvents) {
                uploadEventToAPI(event)
            }
        }

        return Result.success()
    }

    // Checks if network is available
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    // Upload event to API using Retrofit
    private fun uploadEventToAPI(event: Event) {
        sessionManager = SessionManager(applicationContext)

        // Assuming you have a Retrofit API service defined somewhere
        val retrofitService = RetrofitClient.apiService

        val uidRequestBody = RequestBody.create("text/plain".toMediaTypeOrNull(), sessionManager.getUid()!!)

        // Convert event data to RequestBody or any required format
        val eventNameRequestBody = RequestBody.create("text/plain".toMediaTypeOrNull(), event.eventName)
        val eventDateRequestBody = RequestBody.create("text/plain".toMediaTypeOrNull(), event.eventDueDate)

        // Call the API to upload the event with the required parameters
        retrofitService.createEvent(
            uidRequestBody,
            eventNameRequestBody,
            eventDateRequestBody,
        ).enqueue(object : Callback<EventResult> {
            override fun onResponse(call: Call<EventResult>, response: Response<EventResult>) {
                if (response.isSuccessful) {
                    // Mark the event as synced in the local database
                    GlobalScope.launch {
                        markEventAsSynced(event)
                    }

                    // Send a push notification upon successful sync
                    sendPushNotification("Sync Successful", "Event '${event.eventName}' has been synced successfully.")
                } else {
                    logError("Failed to upload event: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<EventResult>, t: Throwable) {
                logError("Error uploading event: ${t.message}")
            }
        })
    }

    // Marks the event as synced in the database
    private suspend fun markEventAsSynced(event: Event) {
        val db = EasyDatabase.getDatabase(applicationContext)
        db.EasyDao().updateEventSyncedStatus(event.eventId)
    }

    // Logs errors for debugging purposes
    private fun logError(message: String) {
        Log.e("SyncWorker", message)
    }

    // Push Notification
    private fun sendPushNotification(title: String, message: String) {
        createNotificationChannel()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = Random().nextInt()

        val notificationBuilder = NotificationCompat.Builder(applicationContext, "sync_channel_id")
            .setSmallIcon(R.mipmap.ic_easy_a_launcher_round)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channelId = "sync_channel_id"
            val channelName = "Sync Notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Notifications for event sync status"
            }
            val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}