package com.parishod.watomatic.model.utils;

import android.content.Context;
import android.content.pm.PackageManager;

public class AppUtils {
    final private Context appContext;
    private static AppUtils _INSTANCE;

    private AppUtils(Context context) {
        this.appContext = context;
    }

    public static AppUtils getInstance(Context context) {
        if (_INSTANCE == null) {
            _INSTANCE = new AppUtils(context);
        }
        return _INSTANCE;
    }

    // Method to reset the singleton instance for testing purposes
    public static void resetInstance() {
        _INSTANCE = null;
    }

    public boolean isPackageInstalled(String packageName) {
        try {
            // Just check if app's icon is present.
            // We might have done this instead of using appContext.getPackageManager().getPackageInfo
            // as a workaround to check if the app is installed even if we did not declare it in the manifest.
            appContext.getPackageManager().getApplicationIcon(packageName);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }
}
