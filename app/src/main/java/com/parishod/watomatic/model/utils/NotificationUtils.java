package com.parishod.watomatic.model.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.res.Resources;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Parcelable;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.RemoteInput;

import com.parishod.watomatic.NotificationWear;
import com.parishod.watomatic.R;
import com.parishod.watomatic.activity.subscription.SubscriptionInfoActivity;

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
                if (title == null) {
                    return null;
                }
                int index = title.indexOf(':');
                if (index != -1) {
                    title = title.substring(0, index);
                }
            }

            //To eliminate the case where group title has number of messages count in it
            Object messagesObj = sbn.getNotification().extras.get("android.messages");
            Parcelable[] b = (messagesObj instanceof Parcelable[]) ? (Parcelable[]) messagesObj : null;
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

        List<NotificationCompat.Action> actions = new ArrayList<>();
        
        // 1. Check standard actions FIRST (more reliable for phone apps like Messenger)
        int actionCount = NotificationCompat.getActionCount(statusBarNotification.getNotification());
        for (int i = 0; i < actionCount; i++) {
            actions.add(NotificationCompat.getAction(statusBarNotification.getNotification(), i));
        }
        
        // 2. Check WearableExtender actions SECOND
        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender(statusBarNotification.getNotification());
        actions.addAll(wearableExtender.getActions());

        List<RemoteInput> remoteInputs = new ArrayList<>();
        PendingIntent pendingIntent = null;
        
        // Strategy: Look for the best action.
        // Priority 1: Action with RemoteInput AND title containing "Reply"
        // Priority 2: Action with RemoteInput (first one found if no "Reply" title match)
        
        NotificationCompat.Action bestAction = null;
        RemoteInput bestRemoteInput = null;

        for (NotificationCompat.Action act : actions) {
            if (act != null && act.getRemoteInputs() != null) {
                for (int x = 0; x < act.getRemoteInputs().length; x++) {
                    RemoteInput remoteInput = act.getRemoteInputs()[x];
                    if(remoteInput.getAllowFreeFormInput()) {
                        // Found a candidate
                        if (bestAction == null) {
                            bestAction = act;
                            bestRemoteInput = remoteInput;
                        } else {
                            // If we already have a candidate, check if this one is better (has "Reply" in title)
                            boolean currentHasReply = act.getTitle() != null && act.getTitle().toString().toLowerCase().contains("reply");
                            boolean bestHasReply = bestAction.getTitle() != null && bestAction.getTitle().toString().toLowerCase().contains("reply");
                            
                            if (currentHasReply && !bestHasReply) {
                                bestAction = act;
                                bestRemoteInput = remoteInput;
                            }
                        }
                    }
                }
            }
        }

        if (bestAction != null && bestRemoteInput != null) {
            remoteInputs.add(bestRemoteInput);
            pendingIntent = bestAction.actionIntent;
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

    private static final String QUOTA_CHANNEL_ID = "default";
    private static final String QUOTA_CHANNEL_NAME = "Quota Alerts";
    private static final int QUOTA_NOTIFICATION_ID = 1002;

    /**
     * Show a notification when the user's quota is exhausted.
     * If the user is on the highest plan, show the renewal date without an Upgrade button.
     * Otherwise, show an Upgrade button that navigates to SubscriptionInfoActivity.
     *
     * @param context       Application context
     * @param isHighestPlan true if the user is already on the highest plan (pro)
     * @param renewalDate   formatted renewal date string, or null if unknown
     */
    public static void showQuotaExhaustedNotification(Context context, boolean isHighestPlan, String renewalDate) {
        Log.d("QuotaExhaustedChecker", "showQuotaExhaustedNotification");
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(QUOTA_CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(
                        QUOTA_CHANNEL_ID,
                        QUOTA_CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Alerts when your AI reply quota is exhausted");
//                channel.enableVibration(true);
                notificationManager.createNotificationChannel(channel);
            }
        }

        // Build the notification message
        String title;
        try {
            title = context.getString(R.string.quota_exhausted_title);
        } catch (Resources.NotFoundException e) {
            title = "AI Reply Quota Exhausted";
        }
        String message;
        try {
            if (isHighestPlan && renewalDate != null) {
                message = context.getString(R.string.quota_exhausted_message_highest_plan, renewalDate);
            } else {
                message = context.getString(R.string.quota_exhausted_message);
            }
        } catch (Resources.NotFoundException e) {
            message = "Your AI reply quota has been exhausted. Please upgrade your plan.";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, QUOTA_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logo_full)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setAutoCancel(true);

        // Only show Upgrade button if user is NOT on the highest plan
        if (!isHighestPlan) {
            // Create intent to open SubscriptionInfoActivity in UPGRADE mode
            Intent upgradeIntent = new Intent(context, SubscriptionInfoActivity.class);
            upgradeIntent.putExtra("subscription_mode", "UPGRADE");
            upgradeIntent.putExtra("from_quota_notification", true);
            upgradeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent upgradePendingIntent = PendingIntent.getActivity(
                    context, QUOTA_NOTIFICATION_ID, upgradeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            builder.addAction(0, context.getString(R.string.upgrade_now), upgradePendingIntent);
            builder.setContentIntent(upgradePendingIntent);
        }

        notificationManager.notify(QUOTA_NOTIFICATION_ID, builder.build());
    }
}
