package com.parishod.watomatic.model.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Parcelable;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;

import androidx.core.app.NotificationCompat;
import androidx.core.app.RemoteInput;

import com.parishod.watomatic.NotificationWear;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NotificationUtils {
    // Do not reply to notifications whose timestamp is older than 2 minutes
    private final static int MAX_OLD_NOTIFICATION_CAN_BE_REPLIED_TIME_MS = 2 * 60 * 1000;

    public static String getTitle(StatusBarNotification sbn) {
        String title;
        if (sbn.getNotification().extras.getBoolean("android.isGroupConversation")) {
            title = sbn.getNotification().extras.getString("android.hiddenConversationTitle");
            //Just to avoid null cases, if by any chance hiddenConversationTitle comes null for group message
            // then extract group name from title
            if (title == null) {
                title = sbn.getNotification().extras.getString("android.title");
                int index = title.indexOf(':');
                if (index != -1) {
                    title = title.substring(0, index);
                }
            }

            //To eliminate the case where group title has number of messages count in it
            Parcelable[] b = (Parcelable[]) sbn.getNotification().extras.get("android.messages");
            if (b != null && b.length > 1) {
                int startIndex = title.lastIndexOf('(');
                if (startIndex != -1) {
                    title = title.substring(0, startIndex);
                }
            }
        } else {
            title = sbn.getNotification().extras.getString("android.title");
        }
        return title;
    }

    /*
   This method is used to avoid replying to unreplied notifications
   which are posted again when next message is received
    */
    public static boolean isNewNotification(StatusBarNotification sbn) {
        //For apps targeting {@link android.os.Build.VERSION_CODES#N} and above, this time is not shown
        //by default unless explicitly set by the apps hence checking not 0
        return sbn.getNotification().when == 0 ||
                (System.currentTimeMillis() - sbn.getNotification().when) < MAX_OLD_NOTIFICATION_CAN_BE_REPLIED_TIME_MS;
    }

    /**
     * Extract WearNotification with RemoteInputs that can be used to send a response
     */
    public static NotificationWear extractWearNotification(StatusBarNotification statusBarNotification) {
        //Should work for communicators such:"com.whatsapp", "com.facebook.orca", "com.google.android.talk", "jp.naver.line.android", "org.telegram.messenger"

        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender(statusBarNotification.getNotification());
        List<NotificationCompat.Action> actions = wearableExtender.getActions();
        List<RemoteInput> remoteInputs = new ArrayList<>(actions.size());
        PendingIntent pendingIntent = null;
        for (NotificationCompat.Action act : actions) {
            if (act != null && act.getRemoteInputs() != null) {
                for (int x = 0; x < act.getRemoteInputs().length; x++) {
                    RemoteInput remoteInput = act.getRemoteInputs()[x];
                    remoteInputs.add(remoteInput);
                    pendingIntent = act.actionIntent;
                }
            }
        }

        return new NotificationWear(
                statusBarNotification.getPackageName(),
                pendingIntent,
                remoteInputs,
                statusBarNotification.getNotification().extras,
                statusBarNotification.getTag(),
                UUID.randomUUID().toString()
        );
    }

    public static String getTitleRaw(StatusBarNotification sbn) {
        return sbn.getNotification().extras.getString("android.title");
    }

    private static final String CHANNEL_ID = "nls_health_channel";
    private static final String CHANNEL_NAME = "Notification Access Alerts";
    private static final int NOTIFICATION_ID = 1001;

    public static void showAccessRevokedNotification(Context context) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create Notification Channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Alerts when Notification Access is disabled");
                channel.enableVibration(true);
                channel.enableLights(true);
                notificationManager.createNotificationChannel(channel);
            }
        }

        // Intent to open Notification Access Settings
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build Notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle("Notification Access Disabled")
                .setContentText("Tap to re-enable notification access for auto-reply.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ERROR)
                .setAutoCancel(true)
                .addAction(android.R.drawable.ic_menu_preferences, "Enable Now", pendingIntent)
                .setContentIntent(pendingIntent);

        // Show Notification
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}
