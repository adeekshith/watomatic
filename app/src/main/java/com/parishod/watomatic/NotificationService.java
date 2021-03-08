package com.parishod.watomatic;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.RemoteInput;

import com.parishod.watomatic.model.CustomRepliesData;
import com.parishod.watomatic.model.logs.AppLogs;
import com.parishod.watomatic.model.logs.AppPackage;
import com.parishod.watomatic.model.logs.MessageLog;
import com.parishod.watomatic.model.logs.MessageLogsDB;
import com.parishod.watomatic.model.preferences.PreferencesManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.lang.Math.max;

public class NotificationService extends NotificationListenerService {
    private final String TAG = NotificationService.class.getSimpleName();
    CustomRepliesData customRepliesData;
    private MessageLogsDB messageLogsDB;
    // Do not reply to consecutive notifications from same person/group that arrive in below time
    // This helps to prevent infinite loops when users on both end uses Watomatic or similar app
    private final int DELAY_BETWEEN_REPLY_IN_MILLISEC = 10 * 1000;
    // Do not reply to notifications whose timestamp is older than 2 minutes
    private final int MAX_OLD_NOTIFICATION_CAN_BE_REPLIED_TIME_MS = 2 * 60 * 1000;

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        AppLogs.getInstance().writeToSDFile("===== Notification Received =======\n");
        if(canReply(sbn)) {
            sendReply(sbn);
        }
        AppLogs.getInstance().writeToSDFile("===== Notification End =======\n\n");
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
        AppLogs.getInstance().writeToSDFile("\tsendReply start\n");
        NotificationWear notificationWear;
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){
            notificationWear = extractQuickReplyNotification(sbn);
        }else{
            notificationWear = extractWearNotification(sbn);
        }
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
            localBundle.putCharSequence(remoteInputs[i].getResultKey(), customRepliesData.getTextToSendOrElse(null));
            i++;
        }

        RemoteInput.addResultsToIntent(remoteInputs, localIntent, localBundle);
        try {
            if (notificationWear.getPendingIntent() != null) {
                logReply(sbn);
                notificationWear.getPendingIntent().send(this, 0, localIntent);
                AppLogs.getInstance().writeToSDFile("\tsendReply success \n");
            }
        } catch (PendingIntent.CanceledException e) {
            Log.e(TAG, "replyToLastNotification error: " + e.getLocalizedMessage());
            AppLogs.getInstance().writeToSDFile("\tsendReply error: " + e.getLocalizedMessage() + "\n");
        }
    }

    private static NotificationWear extractQuickReplyNotification(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        List<NotificationCompat.Action> actions = new ArrayList<>();
        for(int i = 0; i < NotificationCompat.getActionCount(notification); i++) {
            NotificationCompat.Action action = NotificationCompat.getAction(notification, i);
            actions.add(action);
        }
        List<RemoteInput> remoteInputs = new ArrayList<>();
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
                sbn.getPackageName(),
                pendingIntent,
                remoteInputs,
                null,
                sbn.getNotification().extras,
                sbn.getTag(),
                UUID.randomUUID().toString()
        );
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
        AppLogs.getInstance().writeToSDFile("\tMessage from App: " + sbn.getPackageName() + "\n");
        return PreferencesManager.getPreferencesInstance(this)
                .getEnabledApps()
                .contains(sbn.getPackageName());
    }

    private boolean canSendReplyNow(StatusBarNotification sbn){
        String title = sbn.getNotification().extras.getString("android.title");
        if(title != null){
            AppLogs.getInstance().writeToSDFile("\tMessage from: " + title + "\n");
        }
        String selfDisplayName = sbn.getNotification().extras.getString("android.selfDisplayName");
        if(title.equalsIgnoreCase(selfDisplayName)){ //to protect double reply in case where if notification is not dismissed and existing notification is updated with our reply
            return false;
        }
        messageLogsDB = MessageLogsDB.getInstance(getApplicationContext());
        long timeDelay = PreferencesManager.getPreferencesInstance(this).getAutoReplyDelay();
        return (System.currentTimeMillis() - messageLogsDB.logsDao().getLastReplyTimeStamp(title, sbn.getPackageName()) >= max(timeDelay, DELAY_BETWEEN_REPLY_IN_MILLISEC));
    }

    private void logReply(StatusBarNotification sbn){
        String title = sbn.getNotification().extras.getString("android.title");
        messageLogsDB = MessageLogsDB.getInstance(getApplicationContext());
        int packageIndex = messageLogsDB.appPackageDao().getPackageIndex(sbn.getPackageName());
        if(packageIndex <= 0){
            AppPackage appPackage = new AppPackage(sbn.getPackageName());
            messageLogsDB.appPackageDao().insertAppPackage(appPackage);
            packageIndex = messageLogsDB.appPackageDao().getPackageIndex(sbn.getPackageName());
        }
        MessageLog logs = new MessageLog(packageIndex, title, sbn.getNotification().when, customRepliesData.getTextToSendOrElse(null), System.currentTimeMillis());
        messageLogsDB.logsDao().logReply(logs);
    }

    private boolean isGroupMessageAndReplyAllowed(StatusBarNotification sbn){
        if(!sbn.getNotification().extras.getBoolean("android.isGroupConversation")){
            return true;
        }else {
            return PreferencesManager.getPreferencesInstance(this).isGroupReplyEnabled();
        }
    }

    private boolean isServiceEnabled(){
        AppLogs.getInstance().writeToSDFile("\tisServiceEnabled: " + PreferencesManager.getPreferencesInstance(this).isServiceEnabled() + "\n");
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
