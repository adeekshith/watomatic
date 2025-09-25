package com.parishod.watomatic.service

import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.parishod.watomatic.model.preferences.PreferencesManager
import com.parishod.watomatic.model.utils.NotificationUtils
import java.util.concurrent.TimeUnit

class NlsHealthCheckWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val enabled = Settings.Secure.getString(
            applicationContext.contentResolver,
            "enabled_notification_listeners"
        )
        val isEnabled = enabled?.contains(applicationContext.packageName) == true

        if (!isEnabled && PreferencesManager.getPreferencesInstance(applicationContext).isServiceEnabled) {
            Log.w("NLS", "Notification access disabled! Prompt user to re-enable.")
            NotificationUtils.showAccessRevokedNotification(applicationContext)
        } else {
            Log.d("NLS", "Health check OK")
        }
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<NlsHealthCheckWorker>(6, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
