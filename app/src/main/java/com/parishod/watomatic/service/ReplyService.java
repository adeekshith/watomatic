package com.parishod.watomatic.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class ReplyService extends Service {
    private static final String TAG = "ReplyService";
    private static final String CHANNEL_ID = "watomatic_reply_channel";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForegroundReply();

        if (intent != null) {
            PendingIntent pendingIntent = intent.getParcelableExtra("pendingIntent");
            Intent fillInIntent = intent.getParcelableExtra("fillInIntent");

            if (pendingIntent != null && fillInIntent != null) {
                try {
                    Log.d(TAG, "Sending reply via ReplyService");
                    pendingIntent.send(this, 0, fillInIntent);
                } catch (PendingIntent.CanceledException e) {
                    Log.e(TAG, "Failed to send reply: " + e.getMessage());
                }
            }
        }

        stopForeground(true);
        stopSelf();
        return START_NOT_STICKY;
    }

    private void startForegroundReply() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                    NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Reply Service", NotificationManager.IMPORTANCE_LOW);
                    notificationManager.createNotificationChannel(channel);
                }

                Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("Sending Reply")
                        .setContentText("Watomatic is replying...")
                        .setSmallIcon(android.R.drawable.stat_notify_chat)
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .build();

                if (Build.VERSION.SDK_INT >= 34) { // Android 14
                    startForeground(102, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE);
                } else {
                    startForeground(102, notification);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to start foreground service: " + e.getMessage());
            }
        }
    }
}
