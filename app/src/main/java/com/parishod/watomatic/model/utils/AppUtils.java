package com.parishod.watomatic.model.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.app.ShareCompat;
import com.parishod.watomatic.R;
import com.parishod.watomatic.activity.settings.SettingsActivity;

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

    public boolean isPackageInstalled(String packageName) {
        try {
            //Just check if app's icon is present
            appContext.getPackageManager().getApplicationIcon(packageName);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void launchEmailCompose(String subject, String body) {
        try {
            ShareCompat.IntentBuilder intentBuilder = new ShareCompat.IntentBuilder(appContext);
            intentBuilder
                    .setType("text/plain")
                    .addEmailTo(Constants.EMAIL_ADDRESS)
                    .setSubject(subject)
                    .setText(body)
                    //.setHtmlText(body) //If you are using HTML in your body text
                    .setChooserTitle(appContext.getResources().getString(R.string.send_app_logs))
                    .startChooser();
        } catch (Exception e){
            e.printStackTrace();
        }

    }
}
