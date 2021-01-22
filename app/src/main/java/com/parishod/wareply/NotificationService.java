package com.parishod.wareply;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.RemoteInput;

import com.parishod.wareply.model.CustomRepliesData;

import java.util.List;

public class NotificationService extends NotificationListenerService {
    private static final CharSequence REPLY_KEYWORD = "reply";
    private String TAG = NotificationService.class.getSimpleName();
    CustomRepliesData customRepliesData;

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        if(sbn.getPackageName().equals("com.whatsapp")) {
            sendReply(extractWearNotification(sbn));
        }
    }

    private void sendReply(NotificationWear notificationWear) {
        // Possibly transient or non-user notification from WhatsApp like
        // "Checking for new messages" or "WhatsApp web is Active"
        if (notificationWear == null || notificationWear.remoteInputs.isEmpty()) { return;}

        customRepliesData = CustomRepliesData.getInstance(this);

        RemoteInput[] remoteInputs = new RemoteInput[notificationWear.remoteInputs.size()];

        Intent localIntent = new Intent();
        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Bundle localBundle = new Bundle();//notificationWear.bundle;
        int i = 0;
        for(RemoteInput remoteIn : notificationWear.remoteInputs){
            remoteInputs[i] = remoteIn;
            // This works. Might need additional parameter to make it for Hangouts? (notification_tag?)
            localBundle.putCharSequence(remoteInputs[i].getResultKey(), "WaReply: " + customRepliesData.get());
            i++;
        }

        RemoteInput.addResultsToIntent(remoteInputs, localIntent, localBundle);
        try {
            notificationWear.pendingIntent.send(this, 0, localIntent);
        } catch (PendingIntent.CanceledException e) {
            Log.e(TAG, "replyToLastNotification error: " + e.getLocalizedMessage());
        }
    }

    //unused for now
    private void getDetailsOfNotification(RemoteInput remoteInput) {
        //Some more details of RemoteInput... no idea what for but maybe it will be useful at some point
        String resultKey = remoteInput.getResultKey();
        String label = remoteInput.getLabel().toString();
        Boolean canFreeForm = remoteInput.getAllowFreeFormInput();
        if(remoteInput.getChoices() != null && remoteInput.getChoices().length > 0) {
            String[] possibleChoices = new String[remoteInput.getChoices().length];
            for(int i = 0; i < remoteInput.getChoices().length; i++){
                possibleChoices[i] = remoteInput.getChoices()[i].toString();
            }
        }
    }

    /**
     * To extract WearNotification with RemoteInputs that we can use to respond later on
     * @param statusBarNotification
     * @return
     */
    private NotificationWear extractWearNotification(StatusBarNotification statusBarNotification) {
        //Should work for communicators such:"com.whatsapp", "com.facebook.orca", "com.google.android.talk", "jp.naver.line.android", "org.telegram.messenger"
        NotificationWear notificationWear = new NotificationWear();
        notificationWear.packageName = statusBarNotification.getPackageName();

        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender(statusBarNotification.getNotification());
        List<NotificationCompat.Action> actions = wearableExtender.getActions();
        for(NotificationCompat.Action act : actions) {
            if(act != null && act.getRemoteInputs() != null) {
                for(int x = 0; x < act.getRemoteInputs().length; x++) {
                    RemoteInput remoteInput = act.getRemoteInputs()[x];
                    if (remoteInput.getLabel().toString().toLowerCase().contains(REPLY_KEYWORD)) {
                        notificationWear.remoteInputs.add(remoteInput);
                        notificationWear.pendingIntent = act.actionIntent;
                    }
                }
            }
        }
        List<Notification> pages = wearableExtender.getPages();
        notificationWear.pages.addAll(pages);

        notificationWear.bundle = statusBarNotification.getNotification().extras;
        notificationWear.tag = statusBarNotification.getTag();//TODO find how to pass Tag with sending PendingIntent, might fix Hangout problem

        return notificationWear;
    }
}
