package com.parishod.watomatic;

import android.content.Intent;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import com.parishod.watomatic.model.interfaces.OnMessageReplied;
import com.parishod.watomatic.model.utils.MessagingHelper;

public class NotificationService extends NotificationListenerService implements OnMessageReplied {

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        new MessagingHelper(this, this).handleMessage(sbn);
    }

    @Override
    public void onMessageReplied(String key) {
        cancelNotification(key);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        //START_STICKY  to order the system to restart your service as soon as possible when it was killed.
        return START_STICKY;
    }
}
