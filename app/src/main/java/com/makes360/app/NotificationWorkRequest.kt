package com.makes360.app

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class NotificationWorkRequest : Application() {
    override fun onCreate() {
        super.onCreate()

        // Setup WorkManager for periodic checks
        val workRequest = PeriodicWorkRequestBuilder<AnnouncementWorker>(4, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "AnnouncementCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

}
