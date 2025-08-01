package com.parishod.watomatic.activity.notification;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.parishod.watomatic.R;
import com.parishod.watomatic.activity.BaseActivity;
import com.parishod.watomatic.activity.main.MainActivity;
import com.parishod.watomatic.model.utils.NotificationHelper;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class NotificationIntentActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.notification_intent_activity); //dummy layout

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null && extras.getString("package") != null) {
                String packageName = extras.getString("package");
                NotificationHelper.getInstance(getApplicationContext()).markNotificationDismissed(packageName);
                launchApp(packageName);
            } else {
                launchHomeScreen();
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.notification_intent_root_layout), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void launchApp(String packageName) {
        Intent intent;
        PackageManager pm = getPackageManager();

        intent = pm.getLaunchIntentForPackage(packageName);

        // ToDo: Getting null intent sometimes when service restart is implemented #291 (Google Play report)
        if (intent == null) {
            // Toast.makeText("Unable to open application").show();
            return;
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        startActivity(intent);

        finish();
    }

    private void launchHomeScreen() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
