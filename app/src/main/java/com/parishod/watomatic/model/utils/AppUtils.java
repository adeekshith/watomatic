package com.parishod.watomatic.model.utils;

import android.content.Context;
import android.content.pm.PackageManager;

public class AppUtils {
    final private Context appContext;
    private static AppUtils _INSTANCE;

    private AppUtils(Context context){
        this.appContext = context;
    }

    public static AppUtils getInstance (Context context) {
        if (_INSTANCE == null) {
            _INSTANCE = new AppUtils(context);
        }
        return _INSTANCE;
    }

    public boolean isPackageInstalled(String packageName){
        try {
            //Just check if app's icon is present
            appContext.getPackageManager().getApplicationIcon(packageName);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }
}
