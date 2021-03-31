package com.parishod.watomatic.model.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import com.parishod.watomatic.R;
import com.parishod.watomatic.model.App;
import org.json.JSONException;
import org.json.JSONObject;

public class NotificationHelper {
    final private Context appContext;
    private static NotificationHelper _INSTANCE;
    private static NotificationManager notificationManager;
    private static JSONObject appsList = new JSONObject();

    private NotificationHelper(Context appContext){
        this.appContext = appContext;
        init();
    }

    private void init() {
        notificationManager = (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(Constants.NOTIFICATION_CHANNEL_ID, Constants.NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        for (App supportedApp: Constants.SUPPORTED_APPS) {
            try {
                appsList.put(supportedApp.getPackageName(), false);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public static NotificationHelper getInstance (Context context) {
        if (_INSTANCE == null) {
            _INSTANCE = new NotificationHelper(context);
        }
        return _INSTANCE;
    }

    public void sendNotification(String title, String message, String packageName){
        for (App supportedApp: Constants.SUPPORTED_APPS) {
            if(supportedApp.getPackageName().equalsIgnoreCase(packageName)){
                title = supportedApp.getName() + ":" + title;
                break;
            }
        }
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(appContext, Constants.NOTIFICATION_CHANNEL_ID)
                .setGroup("watomatic-" + packageName)
                .setGroupSummary(false)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true);

        int notifId = (int)System.currentTimeMillis();
        notificationManager.notify(notifId, notificationBuilder.build());
        try {
            if(!appsList.getBoolean(packageName)) {
                appsList.put(packageName, true);
                //Need to create one summary notification, this will help group all individual notifications
                NotificationCompat.Builder summaryNotificationBuilder = new NotificationCompat.Builder(appContext, Constants.NOTIFICATION_CHANNEL_ID)
                        .setGroup("watomatic-" + packageName)
                        .setGroupSummary(true)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setAutoCancel(true);
                notificationManager.notify(notifId + 1, summaryNotificationBuilder.build());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void markNotificationDismissed(String packageName){
        packageName = packageName.replace("watomatic-", "");
        try {
            appsList.put(packageName, true);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
