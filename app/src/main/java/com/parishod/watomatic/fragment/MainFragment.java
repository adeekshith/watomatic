package com.parishod.watomatic.fragment;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.parishod.watomatic.NotificationService;
import com.parishod.watomatic.R;
import com.parishod.watomatic.activity.about.AboutActivity;
import com.parishod.watomatic.activity.customreplyeditor.CustomReplyEditorActivity;
import com.parishod.watomatic.activity.settings.SettingsActivity;
import com.parishod.watomatic.model.App;
import com.parishod.watomatic.model.CustomRepliesData;
import com.parishod.watomatic.model.preferences.PreferencesManager;
import com.parishod.watomatic.model.utils.Constants;
import com.parishod.watomatic.model.utils.CustomDialog;

import java.util.ArrayList;
import java.util.List;

import static android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS;
import static com.parishod.watomatic.model.utils.Constants.MAX_DAYS;
import static com.parishod.watomatic.model.utils.Constants.MIN_DAYS;

public class MainFragment extends Fragment {

    private static final int REQ_NOTIFICATION_LISTENER = 100;
    private final int MINUTE_FACTOR = 60;
    CardView autoReplyTextPreviewCard, timePickerCard;
    TextView autoReplyTextPreview, timeSelectedTextPreview, timePickerSubTitleTextPreview;
    CustomRepliesData customRepliesData;
    String autoReplyTextPlaceholder;
    SwitchMaterial mainAutoReplySwitch, groupReplySwitch;
    CardView supportedAppsCard;
    private PreferencesManager preferencesManager;
    private int days = 0;
    private ImageView imgMinus, imgPlus;
    private LinearLayout supportedAppsLayout;
    private List<MaterialCheckBox> supportedAppsCheckboxes = new ArrayList<>();
    private List<View> supportedAppsDummyViews = new ArrayList<>();
    private Activity mActivity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        setHasOptionsMenu(true);

        mActivity = getActivity();

        customRepliesData = CustomRepliesData.getInstance(mActivity);
        preferencesManager = PreferencesManager.getPreferencesInstance(mActivity);

        // Assign Views
        mainAutoReplySwitch = view.findViewById(R.id.mainAutoReplySwitch);
        groupReplySwitch = view.findViewById(R.id.groupReplySwitch);
        autoReplyTextPreviewCard = view.findViewById(R.id.mainAutoReplyTextCardView);
        autoReplyTextPreview = view.findViewById(R.id.textView4);
        supportedAppsLayout = view.findViewById(R.id.supportedPlatformsLayout);
        supportedAppsCard = view.findViewById(R.id.supportedAppsSelectorCardView);

        autoReplyTextPlaceholder = getResources().getString(R.string.mainAutoReplyTextPlaceholder);

        timePickerCard = view.findViewById(R.id.replyFrequencyTimePickerCardView);
        timePickerSubTitleTextPreview = view.findViewById(R.id.timePickerSubTitle);

        timeSelectedTextPreview = view.findViewById(R.id.timeSelectedText);

        imgMinus = view.findViewById(R.id.imgMinus);
        imgPlus = view.findViewById(R.id.imgPlus);

        autoReplyTextPreviewCard.setOnClickListener(this::openCustomReplyEditorActivity);
        autoReplyTextPreview.setText(customRepliesData.getTextToSendOrElse(autoReplyTextPlaceholder));
        // Enable group chat switch only if main switch id ON
        groupReplySwitch.setEnabled(mainAutoReplySwitch.isChecked());

        mainAutoReplySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked && !isListenerEnabled(mActivity, NotificationService.class)){
//                launchNotificationAccessSettings();
                showPermissionsDialog();
            }else {
                preferencesManager.setServicePref(isChecked);
                enableService(isChecked);
                mainAutoReplySwitch.setText(
                        isChecked
                                ? R.string.mainAutoReplySwitchOnLabel
                                : R.string.mainAutoReplySwitchOffLabel
                );

                setSwitchState();

                // Enable group chat switch only if main switch id ON
                groupReplySwitch.setEnabled(isChecked);
            }
        });

        groupReplySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Ignore if this is not triggered by user action but just UI update in onResume() #62
            if (preferencesManager.isGroupReplyEnabled() == isChecked) { return;}

            if(isChecked){
                Toast.makeText(mActivity, R.string.group_reply_on_info_message, Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(mActivity, R.string.group_reply_off_info_message, Toast.LENGTH_SHORT).show();
            }
            preferencesManager.setGroupReplyPref(isChecked);
        });

        imgMinus.setOnClickListener(v -> {
            if(days > MIN_DAYS){
                days--;
                saveNumDays();
            }
        });

        imgPlus.setOnClickListener(v -> {
            if(days < MAX_DAYS){
                days++;
                saveNumDays();
            }
        });

        setNumDays();

        createSupportedAppCheckboxes();

        // TODO Hide incomplete facebook support issue #117
        if (!Constants.BETA_FACEBOOK_SUPPORT_ENABLED) {
            supportedAppsCard.setVisibility(View.GONE);
        }

        return view;
    }

    private void enableOrDisableEnabledAppsCheckboxes(boolean enabled){
        for (MaterialCheckBox checkbox: supportedAppsCheckboxes) {
            checkbox.setEnabled(enabled);
        }
        for (View dummyView: supportedAppsDummyViews) {
            dummyView.setVisibility(enabled ? View.GONE : View.VISIBLE);
        }
    }

    private void createSupportedAppCheckboxes() {
        supportedAppsLayout.removeAllViews();

        //inflate the views
        LayoutInflater inflater = getLayoutInflater();
        for (App supportedApp: Constants.SUPPORTED_APPS) {
            View view = inflater.inflate(R.layout.enable_app_main_layout, null);

            MaterialCheckBox checkBox = view.findViewById(R.id.platform_checkbox);
            checkBox.setText(supportedApp.getName());
            checkBox.setTag(supportedApp);
            checkBox.setChecked(preferencesManager.isAppEnabled(supportedApp));
            checkBox.setEnabled(mainAutoReplySwitch.isChecked());
            checkBox.setOnCheckedChangeListener(supportedAppsCheckboxListener);
            supportedAppsCheckboxes.add(checkBox);

            View platformDummyView = view.findViewById(R.id.platform_dummy_view);
            if(mainAutoReplySwitch.isChecked()){
                platformDummyView.setVisibility(View.GONE);
            }
            platformDummyView.setOnClickListener(v -> {
                if(!mainAutoReplySwitch.isChecked()){
                    Toast.makeText(mActivity, getResources().getString(R.string.enable_auto_reply_switch_msg), Toast.LENGTH_SHORT).show();
                }
            });
            supportedAppsDummyViews.add(platformDummyView);
            supportedAppsLayout.addView(view);
        }
    }

    private CompoundButton.OnCheckedChangeListener supportedAppsCheckboxListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            preferencesManager.saveEnabledApps((App) buttonView.getTag(), isChecked);
        }
    };

    private void saveNumDays(){
        preferencesManager.setAutoReplyDelay(days * 24 * 60 * 60 * 1000);//Save in Milliseconds
        setNumDays();
    }

    private void setNumDays(){
        long timeDelay = (preferencesManager.getAutoReplyDelay()/(60 * 1000));//convert back to minutes
        days = (int)timeDelay/(60 * 24);//convert back to days
        if(days == 0){
            timeSelectedTextPreview.setText("•");
            timePickerSubTitleTextPreview.setText(R.string.time_picker_sub_title_default);
        }else{
            timeSelectedTextPreview.setText("" + days);
            timePickerSubTitleTextPreview.setText(String.format(getResources().getString(R.string.time_picker_sub_title), days));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //If user directly goes to Settings and removes notifications permission
        //when app is launched check for permission and set appropriate app state
        if(!isListenerEnabled(mActivity, NotificationService.class)){
            preferencesManager.setServicePref(false);
        }

        if(!preferencesManager.isServiceEnabled()){
            enableService(false);
        }
        setSwitchState();

        // set group chat switch state
        groupReplySwitch.setChecked(preferencesManager.isGroupReplyEnabled());

        // Set user auto reply text
        autoReplyTextPreview.setText(customRepliesData.getTextToSendOrElse(autoReplyTextPlaceholder));
    }

    private void setSwitchState(){
        mainAutoReplySwitch.setChecked(preferencesManager.isServiceEnabled());
        groupReplySwitch.setEnabled(preferencesManager.isServiceEnabled());
        enableOrDisableEnabledAppsCheckboxes(mainAutoReplySwitch.isChecked());
    }

    //https://stackoverflow.com/questions/20141727/check-if-user-has-granted-notificationlistener-access-to-my-app/28160115
    //TODO: Use in UI to verify if it needs enabling or restarting
    public boolean isListenerEnabled(Context context, Class notificationListenerCls) {
        ComponentName cn = new ComponentName(context, notificationListenerCls);
        String flat = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(cn.flattenToString());
    }

    private void openCustomReplyEditorActivity(View v) {
        Intent intent = new Intent(mActivity, CustomReplyEditorActivity.class);
        startActivity(intent);
    }

    private void openAboutActivity() {
        Intent intent = new Intent(mActivity, AboutActivity.class);
        startActivity(intent);
    }

    private void showPermissionsDialog(){
        CustomDialog customDialog = new CustomDialog(mActivity);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.PERMISSION_DIALOG_TITLE, getString(R.string.permission_dialog_title));
        bundle.putString(Constants.PERMISSION_DIALOG_MSG, getString(R.string.permission_dialog_msg));
        customDialog.showDialog(bundle, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which == -2){
                    //Decline
                    showPermissionDeniedDialog();
                }else{
                    //Accept
                    launchNotificationAccessSettings();
                }
            }
        });
    }

    private void showPermissionDeniedDialog(){
        CustomDialog customDialog = new CustomDialog(mActivity);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.PERMISSION_DIALOG_DENIED_TITLE, getString(R.string.permission_dialog_denied_title));
        bundle.putString(Constants.PERMISSION_DIALOG_DENIED_MSG, getString(R.string.permission_dialog_denied_msg));
        bundle.putBoolean(Constants.PERMISSION_DIALOG_DENIED, true);
        customDialog.showDialog(bundle, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which == -2){
                    //Decline
                    setSwitchState();
                }else{
                    //Accept
                    launchNotificationAccessSettings();
                }
            }
        });
    }

    public void launchNotificationAccessSettings() {
        enableService(true);//we need to enable the service for it so show in settings

        final String NOTIFICATION_LISTENER_SETTINGS;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1){
            NOTIFICATION_LISTENER_SETTINGS = ACTION_NOTIFICATION_LISTENER_SETTINGS;
        }else{
            NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
        }
        Intent i = new Intent(NOTIFICATION_LISTENER_SETTINGS);
        startActivityForResult(i, REQ_NOTIFICATION_LISTENER);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQ_NOTIFICATION_LISTENER){
            if(isListenerEnabled(mActivity, NotificationService.class)){
                Toast.makeText(mActivity, "Permission Granted", Toast.LENGTH_LONG).show();
                preferencesManager.setServicePref(true);
                setSwitchState();
            } else {
                Toast.makeText(mActivity, "Permission Denied", Toast.LENGTH_LONG).show();
                preferencesManager.setServicePref(false);
                setSwitchState();
            }
        }
    }

    private void enableService(boolean enable) {
        PackageManager packageManager = mActivity.getPackageManager();
        ComponentName componentName = new ComponentName(mActivity, NotificationService.class);
        int settingCode = enable
                ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        // enable dummyActivity (as it is disabled in the manifest.xml)
        packageManager.setComponentEnabledSetting(componentName, settingCode, PackageManager.DONT_KILL_APP);

    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        mActivity.getMenuInflater().inflate(R.menu.main_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.about){
            openAboutActivity();
        }else if(item.getItemId() == R.id.setting){
            loadSettingsActivity();
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadSettingsActivity(){
        Intent intent = new Intent(mActivity, SettingsActivity.class);
        mActivity.startActivity(intent);
    }
}
