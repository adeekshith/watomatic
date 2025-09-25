package com.parishod.watomatic.model.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

public class ServieUtils {
    final private Context appContext;
    private static ServieUtils _INSTANCE;

    private ServieUtils(Context context) {
        this.appContext = context;
    }

    public static ServieUtils getInstance(Context context) {
        if (_INSTANCE == null) {
            _INSTANCE = new ServieUtils(context);
        }
        return _INSTANCE;
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i("isMyServiceRunning?", true + "");
                return true;
            }
        }
        Log.i("isMyServiceRunning?", false + "");
        return false;
    }
}
