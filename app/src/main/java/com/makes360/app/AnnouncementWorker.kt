package com.makes360.app

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.makes360.app.ui.client.ClientLogin
import com.makes360.app.ui.intern.InternLogin
import com.makes360.app.ui.trainee.TraineeLogin
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

class AnnouncementWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // Check if the current time is within the allowed window (8:00 AM to 12:00 AM)
        if (!isWithinAllowedTimeWindow()) {
            return Result.success() // Skip work outside the allowed time
        }

        // Your existing code to fetch announcements and show notifications
        fetchAnnouncementsAndNotify()
        return Result.success()
    }

    private fun isWithinAllowedTimeWindow(): Boolean {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        // Check if the current hour is between 6 AM (6) and 12 AM (24)
        return currentHour in 6..24
    }

    private fun fetchAnnouncementsAndNotify() {
        val internPreferences = applicationContext.getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
        val clientPreferences = applicationContext.getSharedPreferences("ClientLoginPrefs", Context.MODE_PRIVATE)
        val traineePreferences = applicationContext.getSharedPreferences("TraineeLoginPrefs", Context.MODE_PRIVATE)
        val sharedPreferences = applicationContext.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

        // Determine the URLs to check based on the logged-in user type
        val urlsToCheck = when {
            internPreferences.contains("user_email") -> listOf(
                "https://www.makes360.com/application/makes360/internship/announcement.php"
            )
            clientPreferences.contains("client_email") -> listOf(
                "https://www.makes360.com/application/makes360/client/announcement.php"
            )
            traineePreferences.contains("trainee_email") -> listOf(
                "https://www.makes360.com/application/makes360/trainee/announcement.php"
            )
            else -> emptyList() // No logged-in user
        }

        urlsToCheck.forEach { url ->
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    connection.disconnect()

                    val jsonResponse = JSONObject(response)
                    val success = jsonResponse.optBoolean("success", false)

                    if (success) {
                        val announcements = jsonResponse.getJSONArray("announcements")
                        val recentAnnouncement = (0 until announcements.length())
                            .map { announcements.getJSONObject(it) }
                            .maxByOrNull { it.getInt("id") }

                        recentAnnouncement?.let { announcement ->
                            val id = announcement.getInt("id")
                            val message = announcement.getString("message")

                            // Check the last announcement ID stored in SharedPreferences
                            val lastAnnouncementId = sharedPreferences.getInt("last_announcement_id", -1)

                            if (id > lastAnnouncementId) {
                                // Save the new announcement ID
                                sharedPreferences.edit().putInt("last_announcement_id", id).apply()

                                // Show notification for the new announcement
                                showNotification(applicationContext, message, url)
                            }
                        }
                    }
                } else {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showNotification(context: Context, message: String, url: String) {
        val channelId = "announcement_channel"
        val notificationId = System.currentTimeMillis().toInt()

        val plainTextMessage =
            HtmlCompat.fromHtml(message, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()

        val intent = when (url) {
            "https://www.makes360.com/application/makes360/internship/announcement.php" -> {
                Intent(context, InternLogin::class.java)
            }
            "https://www.makes360.com/application/makes360/client/announcement.php" -> {
                Intent(context, ClientLogin::class.java)
            }
            "https://www.makes360.com/application/makes360/trainee/announcement.php" -> {
                Intent(context, TraineeLogin::class.java)
            }
            else -> null
        }

        val pendingIntent = intent?.let {
            PendingIntent.getActivity(
                context,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Announcements",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for announcement notifications"
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Request permission
                ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
                return // Exit to avoid calling notify without permission
            }
        }

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentTitle("Makes360 New Announcement")
            .setContentText(plainTextMessage)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        pendingIntent?.let { notificationBuilder.setContentIntent(it) }

        NotificationManagerCompat.from(context).notify(notificationId, notificationBuilder.build())
    }
}