package com.parishod.watomatic

import android.app.PendingIntent
import android.os.Bundle
import androidx.core.app.RemoteInput

data class NotificationWear (
        val packageName: String,
        val pendingIntent: PendingIntent?,
        val remoteInputs: List<RemoteInput>,
        val bundle: Bundle?,
        val tag: String?, // Tag can be null for some notifications
        val id: String
)