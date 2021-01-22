package com.parishod.wareply;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.parishod.wareply.model.CustomRepliesData;

import static android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS;

public class MainActivity extends AppCompatActivity {
    private static final int REQ_NOTIFICATION_LISTENER = 100;
    CardView autoReplyTextPreviewCard;
    TextView autoReplyTextPreview;
    CustomRepliesData customRepliesData;
    String autoReplyTextPlaceholder;
    Switch mainAutoReplySwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        customRepliesData = CustomRepliesData.getInstance(this);

        mainAutoReplySwitch = findViewById(R.id.mainAutoReplySwitch);
        mainAutoReplySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    if(!isListenerEnabled(MainActivity.this, NotificationService.class)){
                        launchNotificationAccessSettings();
                    }
                }
            }
        });

        autoReplyTextPreviewCard = findViewById(R.id.mainAutoReplyTextCardView);
        autoReplyTextPreview = findViewById(R.id.textView4);

        autoReplyTextPlaceholder = getResources().getString(R.string.mainAutoReplyTextPlaceholder);

        autoReplyTextPreviewCard.setOnClickListener(this::openCustomReplyEditorActivity);
        autoReplyTextPreview.setText(customRepliesData.getOrElse(autoReplyTextPlaceholder));
    }

    //https://stackoverflow.com/questions/20141727/check-if-user-has-granted-notificationlistener-access-to-my-app/28160115
    //TODO: Use in UI to verify if it needs enabling or restarting
    public boolean isListenerEnabled(Context context, Class notificationListenerCls) {
        ComponentName cn = new ComponentName(context, notificationListenerCls);
        String flat = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(cn.flattenToString());
    }

    private void openCustomReplyEditorActivity(View v) {
        Intent intent = new Intent(this, CustomReplyEditorActivity.class);
        startActivity(intent);
    }

    public void launchNotificationAccessSettings() {
        Intent i = new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS);
        startActivityForResult(i, REQ_NOTIFICATION_LISTENER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQ_NOTIFICATION_LISTENER){
            if(isListenerEnabled(this, NotificationService.class)){
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_LONG).show();
            }else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_LONG).show();
                mainAutoReplySwitch.setChecked(false);
            }
        }
    }
}