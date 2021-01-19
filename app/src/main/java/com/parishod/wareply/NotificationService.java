package com.parishod.wareply;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public class NotificationService extends NotificationListenerService {
    private String TAG = NotificationService.class.getSimpleName();

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        String title = sbn.getNotification().extras.getString("android:title") != null
                ? sbn.getNotification().extras.getString("android:title") : "";
        String message = sbn.getNotification().extras.getCharSequence("android:text") != null
                ? sbn.getNotification().extras.getCharSequence("android:text").toString() : "";
        Log.d(TAG," Title " + title);
        Log.d(TAG," Message " + message);
    }
}
