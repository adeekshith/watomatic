package com.parishod.watomatic

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.parishod.watomatic.model.subscription.SubscriptionCheckWorker
import java.util.concurrent.TimeUnit

class WatomaticApp : Application() {

    override fun onCreate() {
        super.onCreate()
        scheduleSubscriptionCheck()
    }

    private fun scheduleSubscriptionCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Check subscription status once every 12 hours
        val subscriptionCheckRequest = PeriodicWorkRequestBuilder<SubscriptionCheckWorker>(
            12, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SubscriptionCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            subscriptionCheckRequest
        )
    }
}
