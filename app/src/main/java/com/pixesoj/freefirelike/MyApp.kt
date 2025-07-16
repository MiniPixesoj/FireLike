package com.pixesoj.freefirelike

import android.app.Application
import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.orhanobut.hawk.Hawk
import com.pixesoj.freefirelike.worker.NotificationWorker
import java.util.concurrent.TimeUnit

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Hawk.init(this).build()
        scheduleDailyWorker()
    }

    private fun scheduleDailyWorker() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .setRequiresDeviceIdle(false)
            .setRequiresCharging(false)
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val dailyWorkRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "notifications_ff",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWorkRequest
        )
    }

    companion object {
        fun runWorkerNow(context: Context) {
            val oneTimeRequest = OneTimeWorkRequestBuilder<NotificationWorker>().build()
            WorkManager.getInstance(context).enqueue(oneTimeRequest)
        }
    }
}