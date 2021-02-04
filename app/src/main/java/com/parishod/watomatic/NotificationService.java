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
import com.parishod.watomatic.model.logs.whatsapp.WhatsappAutoReplyLogs;
import com.parishod.watomatic.model.logs.whatsapp.WhatsappAutoReplyLogsDB;
import com.parishod.watomatic.model.preferences.PreferencesManager;

import java.util.List;

import static java.lang.Math.max;

public class NotificationService extends NotificationListenerService {
    private static final CharSequence REPLY_KEYWORD = "reply";
    private final String TAG = NotificationService.class.getSimpleName();
    CustomRepliesData customRepliesData;
    private WhatsappAutoReplyLogsDB whatsappAutoReplyLogsDB;
    private final int DELAY_BETWEEN_REPLY_IN_MILLISEC = 30 * 1000;
    private final int DELAY_BETWEEN_NOTIFICATION_RECEIVED_IN_MILLISEC = 60 * 1000;

    /*
        These are the package names of the apps. for which we want to
        listen the notifications
     */
    private static final class SupportedPackageNames {
        public static final String WHATSAPP_PACK_NAME = "com.whatsapp";
    }

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
            localBundle.putCharSequence(remoteInputs[i].getResultKey(), customRepliesData.get());
            i++;
        }

        RemoteInput.addResultsToIntent(remoteInputs, localIntent, localBundle);
        try {
            logReply(sbn.getNotification().extras.getString("android.title"));
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
     * Extract WearNotification with RemoteInputs that can be used to send a response
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

    private boolean isSupportedPackage(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        switch (packageName){
            case SupportedPackageNames.WHATSAPP_PACK_NAME:
                return true;
            default:
                return false;
        }
    }

    private boolean canSendReplyNow(StatusBarNotification sbn){
        String userId = sbn.getNotification().extras.getString("android.title");
        whatsappAutoReplyLogsDB = WhatsappAutoReplyLogsDB.getInstance(getApplicationContext());
        long timeDelay = PreferencesManager.getPreferencesInstance(this).getAutoReplyDelay();
        return (System.currentTimeMillis() - whatsappAutoReplyLogsDB.logsDao().getLastReplyTimeStamp(userId) >= max(timeDelay, DELAY_BETWEEN_REPLY_IN_MILLISEC));
    }

    private void logReply(String userId){
        whatsappAutoReplyLogsDB = WhatsappAutoReplyLogsDB.getInstance(getApplicationContext());
        WhatsappAutoReplyLogs logs = new WhatsappAutoReplyLogs(userId, System.currentTimeMillis());
        whatsappAutoReplyLogsDB.logsDao().logReply(logs);
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
                (System.currentTimeMillis() - sbn.getNotification().when) < DELAY_BETWEEN_NOTIFICATION_RECEIVED_IN_MILLISEC;
    }
}
