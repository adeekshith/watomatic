package com.parishod.watomatic.model.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.service.notification.StatusBarNotification;

import androidx.core.app.NotificationCompat;

import com.parishod.watomatic.BuildConfig;
import com.parishod.watomatic.R;
import com.parishod.watomatic.activity.notification.NotificationIntentActivity;
import com.parishod.watomatic.model.App;

import org.json.JSONException;
import org.json.JSONObject;

public class NotificationHelper {
    final private Context appContext;
    private static NotificationHelper _INSTANCE;
    private static NotificationManager notificationManager;
    private static final JSONObject appsList = new JSONObject();

    private NotificationHelper(Context appContext) {
        this.appContext = appContext;
        init();
    }

    private void init() {
        notificationManager = (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(Constants.NOTIFICATION_CHANNEL_ID, Constants.NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        for (App supportedApp : Constants.SUPPORTED_APPS) {
            try {
                appsList.put(supportedApp.getPackageName(), false);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public static NotificationHelper getInstance(Context context) {
        if (_INSTANCE == null) {
            _INSTANCE = new NotificationHelper(context);
        }
        return _INSTANCE;
    }

    public void sendNotification(String title, String message, String packageName) {
        for (App supportedApp : Constants.SUPPORTED_APPS) {
            if (supportedApp.getPackageName().equalsIgnoreCase(packageName)) {
                title = supportedApp.getName() + ":" + title;
                break;
            }
        }

        Intent intent = new Intent(appContext, NotificationIntentActivity.class);
        intent.putExtra("package", packageName);
        intent.setAction(Long.toString(System.currentTimeMillis()));//This is needed to generate unique pending intents, else when we create multiple pending intents they will be overwritten by last one
        PendingIntent pIntent = PendingIntent.getActivity(appContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(appContext, Constants.NOTIFICATION_CHANNEL_ID)
                .setGroup("watomatic-" + packageName)
                .setGroupSummary(false)
                .setSmallIcon(R.drawable.ic_logo_full)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(pIntent);

        //logic to detect if notifications exists else generate summary notification
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            StatusBarNotification[] notifications = notificationManager.getActiveNotifications();
            for (App supportedApp : Constants.SUPPORTED_APPS) {
                try {
                    appsList.put(supportedApp.getPackageName(), false);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            for (StatusBarNotification notification : notifications) {
                if (notification.getPackageName().equalsIgnoreCase(BuildConfig.APPLICATION_ID)) {
                    setNotificationSummaryShown(notification.getNotification().getGroup());
                }
            }
        }

        int notifId = (int) System.currentTimeMillis();
        notificationManager.notify(notifId, notificationBuilder.build());
        try {
            if (!appsList.getBoolean(packageName)) {
                appsList.put(packageName, true);
                //Need to create one summary notification, this will help group all individual notifications
                NotificationCompat.Builder summaryNotificationBuilder = new NotificationCompat.Builder(appContext, Constants.NOTIFICATION_CHANNEL_ID)
                        .setGroup("watomatic-" + packageName)
                        .setGroupSummary(true)
                        .setSmallIcon(R.drawable.ic_logo_full)
                        .setAutoCancel(true)
                        .setContentIntent(pIntent);
                notificationManager.notify(notifId + 1, summaryNotificationBuilder.build());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void setNotificationSummaryShown(String packageName) {
        if (packageName != null) {
            packageName = packageName.replace("watomatic-", "");
            try {
                appsList.put(packageName, true);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void markNotificationDismissed(String packageName) {
        packageName = packageName.replace("watomatic-", "");
        try {
            appsList.put(packageName, false);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public NotificationCompat.Builder getForegroundServiceNotification(Service service) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel notificationChannel = new NotificationChannel(Constants.NOTIFICATION_CHANNEL_ID, Constants.NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        Intent intent = new Intent(appContext, NotificationIntentActivity.class);
        intent.setAction(Long.toString(System.currentTimeMillis()));//This is needed to generate unique pending intents, else when we create multiple pending intents they will be overwritten by last one
        PendingIntent pIntent = PendingIntent.getActivity(appContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder notificationBuilder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            notificationBuilder = new NotificationCompat.Builder(service, Constants.NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_logo_full)
                    .setContentTitle(appContext.getString(R.string.app_name))
                    .setContentText(appContext.getString(R.string.running_in_the_background))
                    .setPriority(NotificationManager.IMPORTANCE_HIGH)
                    .setContentIntent(pIntent);
        } else {
            notificationBuilder = new NotificationCompat.Builder(service, Constants.NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_logo_full)
                    .setContentTitle(appContext.getString(R.string.app_name))
                    .setContentText(appContext.getString(R.string.running_in_the_background))
                    .setContentIntent(pIntent);
        }

        return notificationBuilder;
    }
}
