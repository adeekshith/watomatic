package com.parishod.watomatic.model.subscription

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.parishod.watomatic.model.preferences.PreferencesManager

/**
 * Background worker to periodically check subscription status.
 * This ensures that if a subscription expires or renews in the background,
 * the app state is updated accordingly.
 */
class SubscriptionCheckWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val context = applicationContext
            val prefs = PreferencesManager.getPreferencesInstance(context)

            // Use Reflection to instantiate SubscriptionManagerImpl since it resides in flavor source sets
            // and cannot be referenced directly from 'main' source set at compile time.
            try {
                val clazz =
                    Class.forName("com.parishod.watomatic.model.subscription.SubscriptionManagerImpl")
                val constructor =
                    clazz.getConstructor(Context::class.java, PreferencesManager::class.java)
                val subscriptionManager =
                    constructor.newInstance(context, prefs) as SubscriptionManager

                subscriptionManager.refreshSubscriptionStatus()
                Result.success()
            } catch (e: ClassNotFoundException) {
                // Should not happen if build is correct
                e.printStackTrace()
                Result.failure()
            } catch (e: Exception) {
                e.printStackTrace()
                if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        } catch (ignore: Exception) {
            Result.failure()
        }
    }
}
