package com.parishod.watomatic.model.utils;

import static java.lang.Math.max;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.text.SpannableString;
import android.util.Log;

import androidx.core.app.RemoteInput;

import com.parishod.watomatic.NotificationWear;
import com.parishod.watomatic.R;
import com.parishod.watomatic.model.App;
import com.parishod.watomatic.model.CustomRepliesData;
import com.parishod.watomatic.model.interfaces.OnMessageReplied;
import com.parishod.watomatic.model.preferences.PreferencesManager;

public class MessagingHelper {
    final private Context mContext;
    CustomRepliesData customRepliesData;
    private DbUtils dbUtils;
    private EventLogger eventLogger;
    final private OnMessageReplied onMessageReplied;

    public MessagingHelper(Context context, OnMessageReplied onMessageReplied){
        this.mContext = context;
        this.onMessageReplied = onMessageReplied;
    }

    public void handleMessage(StatusBarNotification sbn){
        eventLogger = new EventLogger();
        if (canReply(sbn) && shouldReply(sbn)) {
            sendReply(sbn);
        }else{
            //if notification is from supported apps only log the data
            for (App supportedApp : Constants.SUPPORTED_APPS) {
                if(supportedApp.getPackageName().equalsIgnoreCase(sbn.getPackageName())){
                    dbUtils.logReply(sbn, NotificationUtils.getTitle(sbn), false, eventLogger.getEvent().toString());
                    break;
                }
            }
        }
    }

    private boolean canReply(StatusBarNotification sbn) {
        eventLogger.setNewNotification(NotificationUtils.isNewNotification(sbn));
        return isServiceEnabled() &&
                isSupportedPackage(sbn) &&
                NotificationUtils.isNewNotification(sbn) &&
                isGroupMessageAndReplyAllowed(sbn) &&
                canSendReplyNow(sbn);
    }

    private boolean shouldReply(StatusBarNotification sbn) {
        PreferencesManager prefs = PreferencesManager.getPreferencesInstance(mContext);
        boolean isGroup = sbn.getNotification().extras.getBoolean("android.isGroupConversation");
        boolean isContactsReplyEnabled = prefs.isContactReplyEnabled();

        eventLogger.setContactsReplyEnabled(isContactsReplyEnabled);

        //Check contact based replies
        if (isContactsReplyEnabled && !isGroup) {
            //Title contains sender name (at least on WhatsApp)
            String senderName = sbn.getNotification().extras.getString("android.title");
            if (!ContactsHelper.getInstance(mContext).hasContactPermission()){
                eventLogger.setContactsReplyReason(mContext.getString(R.string.log_msg_contact_permission_denied));
            }
            if (ContactsHelper.getInstance(mContext).hasContactPermission() && !prefs.getReplyToNames().contains(senderName)){
                eventLogger.setContactsReplyReason(mContext.getString(R.string.log_msg_contact_mismatch));
            }
            if (!prefs.getCustomReplyNames().contains(senderName)){
                eventLogger.setContactsReplyReason(mContext.getString(R.string.log_msg_contact_mismatch));
            }
            if(prefs.isContactReplyBlacklistMode()){
                eventLogger.setContactsReplyReason(mContext.getString(R.string.log_msg_contact_blacklisted));
            }
            //Check if should reply to contact
            boolean isNameSelected =
                    (ContactsHelper.getInstance(mContext).hasContactPermission()
                            && prefs.getReplyToNames().contains(senderName)) ||
                            prefs.getCustomReplyNames().contains(senderName);
            if ((isNameSelected && prefs.isContactReplyBlacklistMode()) ||
                    !isNameSelected && !prefs.isContactReplyBlacklistMode()) {
                //If contact is on the list and contact reply is on blacklist mode,
                // or contact is not in the list and reply is on whitelist mode,
                // we don't want to reply
                return false;
            }
        }

        //Check more conditions on future feature implementations

        //If we got here, all conditions to reply are met
        return true;
    }

    private void sendReply(StatusBarNotification sbn) {
        NotificationWear notificationWear = NotificationUtils.extractWearNotification(sbn);
        // Possibly transient or non-user notification from WhatsApp like
        // "Checking for new messages" or "WhatsApp web is Active"
        if (notificationWear.getRemoteInputs().isEmpty()) {
            eventLogger.setRemoteInputsEmpty(true);
            dbUtils.logReply(sbn, NotificationUtils.getTitle(sbn), false, eventLogger.getEvent().toString());
            return;
        }

        eventLogger.setRemoteInputsEmpty(false);

        customRepliesData = CustomRepliesData.getInstance(mContext);

        RemoteInput[] remoteInputs = new RemoteInput[notificationWear.getRemoteInputs().size()];

        Intent localIntent = new Intent();
        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Bundle localBundle = new Bundle();//notificationWear.bundle;
        int i = 0;
        for (RemoteInput remoteIn : notificationWear.getRemoteInputs()) {
            remoteInputs[i] = remoteIn;
            // This works. Might need additional parameter to make it for Hangouts? (notification_tag?)
            localBundle.putCharSequence(remoteInputs[i].getResultKey(), customRepliesData.getTextToSendOrElse());
            i++;
        }

        RemoteInput.addResultsToIntent(remoteInputs, localIntent, localBundle);
        try {
            if (notificationWear.getPendingIntent() != null) {
                if (dbUtils == null) {
                    dbUtils = new DbUtils(mContext.getApplicationContext());
                }
                dbUtils.logReply(sbn, NotificationUtils.getTitle(sbn), true, eventLogger.getEvent().toString());
                notificationWear.getPendingIntent().send(mContext, 0, localIntent);
                if (PreferencesManager.getPreferencesInstance(mContext).isShowNotificationEnabled()) {
                    NotificationHelper.getInstance(mContext.getApplicationContext()).sendNotification(sbn.getNotification().extras.getString("android.title"), sbn.getNotification().extras.getString("android.text"), sbn.getPackageName());
                }
                onMessageReplied.onMessageReplied(sbn.getKey());
                if (canPurgeMessages()) {
                    dbUtils.purgeMessageLogs();
                    PreferencesManager.getPreferencesInstance(mContext).setPurgeMessageTime(System.currentTimeMillis());
                }
            }
        } catch (PendingIntent.CanceledException e) {
            Log.e("MessagingHelper", "replyToLastNotification error: " + e.getLocalizedMessage());
            eventLogger.setReplyErrReason(e.getLocalizedMessage());
            dbUtils.logReply(sbn, NotificationUtils.getTitle(sbn), false, eventLogger.getEvent().toString());
        }
    }

    private boolean canPurgeMessages() {
        //Added L to avoid numeric overflow expression
        //https://stackoverflow.com/questions/43801874/numeric-overflow-in-expression-manipulating-timestamps
        long daysBeforePurgeInMS = 30 * 24 * 60 * 60 * 1000L;
        return (System.currentTimeMillis() - PreferencesManager.getPreferencesInstance(mContext).getLastPurgedTime()) > daysBeforePurgeInMS;
    }

    private boolean isSupportedPackage(StatusBarNotification sbn) {
        boolean isSupportedPackage = PreferencesManager.getPreferencesInstance(mContext)
                .getEnabledApps()
                .contains(sbn.getPackageName());
        eventLogger.setSupportedPackage(isSupportedPackage);
        return isSupportedPackage;
    }

    private boolean canSendReplyNow(StatusBarNotification sbn) {
        // Do not reply to consecutive notifications from same person/group that arrive in below time
        // This helps to prevent infinite loops when users on both end uses Watomatic or similar app
        int DELAY_BETWEEN_REPLY_IN_MILLISEC = 10 * 1000;

        String title = NotificationUtils.getTitle(sbn);
        String selfDisplayName = sbn.getNotification().extras.getString("android.selfDisplayName");
        if (title != null && title.equalsIgnoreCase(selfDisplayName)) { //to protect double reply in case where if notification is not dismissed and existing notification is updated with our reply
            eventLogger.setReplyErrReason("Possibly Duplicate Reply");
            return false;
        }
        if (dbUtils == null) {
            dbUtils = new DbUtils(mContext.getApplicationContext());
        }
        long timeDelay = PreferencesManager.getPreferencesInstance(mContext).getAutoReplyDelay();
        boolean canReplyNow = (System.currentTimeMillis() - dbUtils.getLastRepliedTime(sbn.getPackageName(), title) >= max(timeDelay, DELAY_BETWEEN_REPLY_IN_MILLISEC));
        eventLogger.setCanSendReplyNow(canReplyNow);
        return canReplyNow;
    }

    private boolean isGroupMessageAndReplyAllowed(StatusBarNotification sbn) {
        String rawTitle = NotificationUtils.getTitleRaw(sbn);
        //android.text returning SpannableString
        SpannableString rawText = SpannableString.valueOf("" + sbn.getNotification().extras.get("android.text"));
        // Detect possible group image message by checking for colon and text starts with camera icon #181
        boolean isPossiblyAnImageGrpMsg = ((rawTitle != null) && (": ".contains(rawTitle) || "@ ".contains(rawTitle)))
                && ((rawText != null) && rawText.toString().contains("\uD83D\uDCF7"));
        eventLogger.setGroupReplyEnabled(PreferencesManager.getPreferencesInstance(mContext).isGroupReplyEnabled());
        eventLogger.setIsGroupMessage(sbn.getNotification().extras.getBoolean("android.isGroupConversation"));
        if (!sbn.getNotification().extras.getBoolean("android.isGroupConversation")) {
            return !isPossiblyAnImageGrpMsg;
        } else {
            return PreferencesManager.getPreferencesInstance(mContext).isGroupReplyEnabled();
        }
    }

    private boolean isServiceEnabled() {
        boolean isServiceEnabled = PreferencesManager.getPreferencesInstance(mContext).isServiceEnabled();
        eventLogger.setServiceEnabled(isServiceEnabled);
        return isServiceEnabled;
    }
}
