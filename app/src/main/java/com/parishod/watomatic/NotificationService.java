package com.parishod.watomatic;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.RemoteInput;

import com.parishod.watomatic.model.CustomRepliesData;
import com.parishod.watomatic.model.logs.facebook.FacebookAutoReplyLogs;
import com.parishod.watomatic.model.logs.whatsapp.WhatsappAutoReplyLogs;
import com.parishod.watomatic.model.logs.AutoReplyLogsDB;
import com.parishod.watomatic.model.preferences.PreferencesManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.lang.Math.max;

public class NotificationService extends NotificationListenerService {
    private final String TAG = NotificationService.class.getSimpleName();
    CustomRepliesData customRepliesData;
    private AutoReplyLogsDB autoReplyLogsDB;
    // Do not reply to consecutive notifications from same person/group that arrive in below time
    // This helps to prevent infinite loops when users on both end uses Watomatic or similar app
    private final int DELAY_BETWEEN_REPLY_IN_MILLISEC = 10 * 1000;
    // Do not reply to notifications whose timestamp is older than 2 minutes
    private final int MAX_OLD_NOTIFICATION_CAN_BE_REPLIED_TIME_MS = 2 * 60 * 1000;

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        if(canReply(sbn)) {
            sendReply(sbn);
        }
    }

    private boolean canReply(StatusBarNotification sbn){
        return isServiceEnabled() &&
                isSupportedPackage(sbn) &&
                isNewNotification(sbn) &&
                isGroupMessageAndReplyAllowed(sbn) &&
                canSendReplyNow(sbn);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        //START_STICKY  to order the system to restart your service as soon as possible when it was killed.
        return START_STICKY;
    }

    private void sendReply(StatusBarNotification sbn) {
        NotificationWear notificationWear = extractWearNotification(sbn);
        // Possibly transient or non-user notification from WhatsApp like
        // "Checking for new messages" or "WhatsApp web is Active"
        if (notificationWear.getRemoteInputs().isEmpty()) { return;}


        customRepliesData = CustomRepliesData.getInstance(this);

        RemoteInput[] remoteInputs = new RemoteInput[notificationWear.getRemoteInputs().size()];

        Intent localIntent = new Intent();
        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Bundle localBundle = new Bundle();//notificationWear.bundle;
        int i = 0;
        for(RemoteInput remoteIn : notificationWear.getRemoteInputs()){
            remoteInputs[i] = remoteIn;
            // This works. Might need additional parameter to make it for Hangouts? (notification_tag?)
            localBundle.putCharSequence(remoteInputs[i].getResultKey(), customRepliesData.get());
            i++;
        }

        RemoteInput.addResultsToIntent(remoteInputs, localIntent, localBundle);
        try {
            if (notificationWear.getPendingIntent() != null) {
                logReply(sbn);
                notificationWear.getPendingIntent().send(this, 0, localIntent);
            }
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
     * Extract WearNotification with RemoteInputs that can be used to send a response
     * @param statusBarNotification
     * @return
     */
    private NotificationWear extractWearNotification(StatusBarNotification statusBarNotification) {
        //Should work for communicators such:"com.whatsapp", "com.facebook.orca", "com.google.android.talk", "jp.naver.line.android", "org.telegram.messenger"

        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender(statusBarNotification.getNotification());
        List<NotificationCompat.Action> actions = wearableExtender.getActions();
        List<RemoteInput> remoteInputs = new ArrayList<>(actions.size());
        PendingIntent pendingIntent = null;
        for(NotificationCompat.Action act : actions) {
            if(act != null && act.getRemoteInputs() != null) {
                for(int x = 0; x < act.getRemoteInputs().length; x++) {
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
                wearableExtender.getPages(),
                statusBarNotification.getNotification().extras,
                statusBarNotification.getTag(),
                UUID.randomUUID().toString()
        );
    }

    private boolean isSupportedPackage(StatusBarNotification sbn) {
        return PreferencesManager.getPreferencesInstance(this)
                .getEnabledApps()
                .contains(sbn.getPackageName());
    }

    private boolean canSendReplyNow(StatusBarNotification sbn){
        String userId = sbn.getNotification().extras.getString("android.title");
        autoReplyLogsDB = AutoReplyLogsDB.getInstance(getApplicationContext());
        long timeDelay = PreferencesManager.getPreferencesInstance(this).getAutoReplyDelay();
        if(sbn.getPackageName().contains("whatsapp")) {
            return (System.currentTimeMillis() - autoReplyLogsDB.whatsappAutoReplyLogsDao().getLastReplyTimeStamp(userId) >= max(timeDelay, DELAY_BETWEEN_REPLY_IN_MILLISEC));
        }else {
            return (System.currentTimeMillis() - autoReplyLogsDB.facebookAutoReplyLogsDao().getLastReplyTimeStamp(userId) >= max(timeDelay, DELAY_BETWEEN_REPLY_IN_MILLISEC));
        }
    }

    private void logReply(StatusBarNotification sbn){
        String userId = sbn.getNotification().extras.getString("android.title");
        autoReplyLogsDB = AutoReplyLogsDB.getInstance(getApplicationContext());
        if(sbn.getPackageName().contains("whatsapp")) {
            WhatsappAutoReplyLogs logs = new WhatsappAutoReplyLogs(userId, System.currentTimeMillis());
            autoReplyLogsDB.whatsappAutoReplyLogsDao().logReply(logs);
        }else if(sbn.getPackageName().contains("facebook")) {
            FacebookAutoReplyLogs logs = new FacebookAutoReplyLogs(userId, System.currentTimeMillis());
            autoReplyLogsDB.facebookAutoReplyLogsDao().logReply(logs);
        }
    }

    private boolean isGroupMessageAndReplyAllowed(StatusBarNotification sbn){
        if(!sbn.getNotification().extras.getBoolean("android.isGroupConversation")){
            return true;
        }else {
            return PreferencesManager.getPreferencesInstance(this).isGroupReplyEnabled();
        }
    }

    private boolean isServiceEnabled(){
        return PreferencesManager.getPreferencesInstance(this).isServiceEnabled();
    }

    /*
    This method is used to avoid replying to unreplied notifications
    which are posted again when next message is received
     */
    private boolean isNewNotification(StatusBarNotification sbn){
        //For apps targeting {@link android.os.Build.VERSION_CODES#N} and above, this time is not shown
        //by default unless explicitly set by the apps hence checking not 0
        return sbn.getNotification().when == 0 ||
                (System.currentTimeMillis() - sbn.getNotification().when) < MAX_OLD_NOTIFICATION_CAN_BE_REPLIED_TIME_MS;
    }
}
