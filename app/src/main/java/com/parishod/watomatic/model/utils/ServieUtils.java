package com.parishod.watomatic.model.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.parishod.watomatic.service.KeepAliveService;

public class ServieUtils {
    final private Context appContext;
    private static ServieUtils _INSTANCE;

    private ServieUtils(Context context){
        this.appContext = context;
    }

    public static ServieUtils getInstance (Context context) {
        if (_INSTANCE == null) {
            _INSTANCE = new ServieUtils(context);
        }
        return _INSTANCE;
    }

    public void startNotificationService(){
        if (!isMyServiceRunning(KeepAliveService.class)) {
            Intent mServiceIntent = new Intent(appContext, KeepAliveService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appContext.startForegroundService(mServiceIntent);
            }else{
                appContext.startService(mServiceIntent);
            }
        }
    }

    public void stopNotificationService(){
        Intent mServiceIntent = new Intent(appContext, KeepAliveService.class);
        appContext.stopService(mServiceIntent);
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i ("isMyServiceRunning?", true+"");
                return true;
            }
        }
        Log.i ("isMyServiceRunning?", false+"");
        return false;
    }
}
