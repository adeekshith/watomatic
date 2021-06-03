package com.parishod.watomatic.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.parishod.watomatic.service.KeepAliveService

class NotificationServiceRestartReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        if(action?.equals(Intent.ACTION_BOOT_COMPLETED) == true
                || action?.equals("Watomatic-RestartService-Broadcast") == true) {
            context?.let { restartService(context = context) }
        }
    }

    private fun restartService(context: Context) {
        val serviceIntent = Intent(context, KeepAliveService::class.java)
        context.startService(serviceIntent)
    }
}