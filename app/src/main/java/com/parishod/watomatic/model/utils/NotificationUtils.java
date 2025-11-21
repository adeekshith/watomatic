package com.parishod.watomatic.model.utils;

import android.app.PendingIntent;
import android.os.Parcelable;
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
}
