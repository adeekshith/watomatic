package com.parishod.watomatic.service;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.parishod.watomatic.NotificationService;
import com.parishod.watomatic.model.preferences.PreferencesManager;
import com.parishod.watomatic.model.utils.NotificationHelper;
import com.parishod.watomatic.receivers.NotificationServiceRestartReceiver;

public class KeepAliveService extends Service {
    private static final int FOREGROUND_NOTIFICATION_ID = 10;

    @Override
    public void onCreate() {
        Log.d("DEBUG", "KeepAliveService onCreate");
        super.onCreate();
        startForeground(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        super.onStartCommand(intent, flags, startId);
        Log.d("DEBUG", "KeepAliveService onStartCommand");
        startNotificationService();
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d("DEBUG", "KeepAliveService onBind");
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("DEBUG", "KeepAliveService onUnbind");
        tryReconnectService();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("DEBUG", "KeepAliveService onDestroy");
        tryReconnectService();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.d("DEBUG", "KeepAliveService onTaskRemoved");
        tryReconnectService();
    }

    public void tryReconnectService() {
        if (PreferencesManager.getPreferencesInstance(getApplicationContext()).isServiceEnabled()
            && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S || PreferencesManager.getPreferencesInstance(getApplicationContext()).isForegroundServiceNotificationEnabled())) {
            Log.d("DEBUG", "KeepAliveService tryReconnectService");
            //Send broadcast to restart service
            Intent broadcastIntent = new Intent(getApplicationContext(), NotificationServiceRestartReceiver.class);
            broadcastIntent.setAction("Watomatic-RestartService-Broadcast");
            sendBroadcast(broadcastIntent);
        }
    }

    private void startNotificationService() {
        if (!isMyServiceRunning()) {
            Log.d("DEBUG", "KeepAliveService startNotificationService");
            Intent mServiceIntent = new Intent(this, NotificationService.class);
            startService(mServiceIntent);
        }
    }

    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (NotificationService.class.equals(service.service.getClassName())) {
                Log.i("isMyServiceRunning?", true + "");
                return true;
            }
        }
        Log.i("isMyServiceRunning?", false + "");
        return false;
    }

    private void startForeground(Service service) {
        Log.e("DEBUG", "startForeground");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder notificationBuilder = NotificationHelper.getInstance(getApplicationContext()).getForegroundServiceNotification(service);
            service.startForeground(FOREGROUND_NOTIFICATION_ID, notificationBuilder.build());
        }
    }
}
