package com.parishod.watomatic.receivers

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.parishod.watomatic.NotificationService
import com.parishod.watomatic.model.preferences.PreferencesManager
import com.parishod.watomatic.service.KeepAliveService

class NotificationServiceRestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        if (action?.equals(Intent.ACTION_BOOT_COMPLETED) == true
                || action?.equals("Watomatic-RestartService-Broadcast") == true) {
            context?.let { restartService(context = context) }
        }
    }

    private fun restartService(context: Context) {
        val serviceIntent = Intent(context, KeepAliveService::class.java)
        // ToDo: Should probably start using foreground service to prevent IllegalState exception below
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            }else{
                context.startService(serviceIntent)
            }
        } catch (e: IllegalStateException) {
            Log.e("NotifServiceRestart", "Unable to restart notification service")
        }
    }

    private fun enableService(context: Context) {
        val packageManager: PackageManager = context.packageManager
        val componentName = ComponentName(context, NotificationService::class.java)
        val settingCode = PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        // enable notificationservicelistener (as it is disabled in the manifest.xml)
        packageManager.setComponentEnabledSetting(
                componentName,
                settingCode,
                PackageManager.DONT_KILL_APP
        )
    }
}