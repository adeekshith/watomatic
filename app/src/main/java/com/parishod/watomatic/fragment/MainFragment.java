package com.parishod.watomatic.fragment;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.parishod.watomatic.BuildConfig;
import com.parishod.watomatic.NotificationService;
import com.parishod.watomatic.R;
import com.parishod.watomatic.activity.about.AboutActivity;
import com.parishod.watomatic.activity.customreplyeditor.CustomReplyEditorActivity;
import com.parishod.watomatic.activity.enabledapps.EnabledAppsActivity;
import com.parishod.watomatic.activity.settings.SettingsActivity;
import com.parishod.watomatic.adapter.SupportedAppsAdapter;
import com.parishod.watomatic.model.logs.App;
import com.parishod.watomatic.model.CustomRepliesData;
import com.parishod.watomatic.model.preferences.PreferencesManager;
import com.parishod.watomatic.model.utils.Constants;
import com.parishod.watomatic.model.utils.CustomDialog;
import com.parishod.watomatic.model.utils.DbUtils;
import com.parishod.watomatic.model.utils.ServieUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static android.content.Intent.ACTION_VIEW;
import static android.content.Intent.CATEGORY_BROWSABLE;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_ACTIVITY_REQUIRE_DEFAULT;
import static android.content.Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER;
import static android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS;
import static com.parishod.watomatic.model.utils.Constants.MAX_DAYS;
import static com.parishod.watomatic.model.utils.Constants.MIN_DAYS;
import static com.parishod.watomatic.model.utils.Constants.MIN_REPLIES_TO_ASK_APP_RATING;

public class MainFragment extends Fragment {

    private static final int REQ_NOTIFICATION_LISTENER = 100;
    CardView autoReplyTextPreviewCard, timePickerCard;
    TextView autoReplyTextPreview, timeSelectedTextPreview, timePickerSubTitleTextPreview;
    CustomRepliesData customRepliesData;
    String autoReplyTextPlaceholder;
    SwitchMaterial mainAutoReplySwitch, groupReplySwitch;
    CardView supportedAppsCard;
    private PreferencesManager preferencesManager;
    private int days = 0;
    private final List<MaterialCheckBox> supportedAppsCheckboxes = new ArrayList<>();
    private final List<View> supportedAppsDummyViews = new ArrayList<>();
    private Activity mActivity;
    private SupportedAppsAdapter supportedAppsAdapter;
    private List<App> enabledApps = new ArrayList<>();
    private Set<App> supportedApps;
    private DbUtils dbUtils;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        setHasOptionsMenu(true);

        mActivity = getActivity();

        dbUtils = new DbUtils(mActivity);
        supportedApps = dbUtils.getSupportedApps();
        if(supportedApps.isEmpty()){
            supportedApps = Constants.SUPPORTED_APPS;
            for (App app: supportedApps
            ) {
                dbUtils.insertSupportedApp(app);
            }
        }

        customRepliesData = CustomRepliesData.getInstance(mActivity);
        preferencesManager = PreferencesManager.getPreferencesInstance(mActivity);

        // Assign Views
        mainAutoReplySwitch = view.findViewById(R.id.mainAutoReplySwitch);
        groupReplySwitch = view.findViewById(R.id.groupReplySwitch);
        autoReplyTextPreviewCard = view.findViewById(R.id.mainAutoReplyTextCardView);
        autoReplyTextPreview = view.findViewById(R.id.textView4);
        supportedAppsCard = view.findViewById(R.id.supportedAppsSelectorCardView);

        supportedAppsCard.setOnClickListener(v -> launchEnabledAppsActivity());

        RecyclerView enabledAppsList = view.findViewById(R.id.enabled_apps_list);
        GridLayoutManager layoutManager = new GridLayoutManager(mActivity, getSpanCount(mActivity));
        enabledAppsList.setLayoutManager(layoutManager);
        supportedAppsAdapter = new SupportedAppsAdapter(Constants.EnabledAppsDisplayType.HORIZONTAL, getEnabledApps(), v ->
                launchEnabledAppsActivity()
        );
        enabledAppsList.setAdapter(supportedAppsAdapter);

        autoReplyTextPlaceholder = getResources().getString(R.string.mainAutoReplyTextPlaceholder);

        timePickerCard = view.findViewById(R.id.replyFrequencyTimePickerCardView);
        timePickerSubTitleTextPreview = view.findViewById(R.id.timePickerSubTitle);

        timeSelectedTextPreview = view.findViewById(R.id.timeSelectedText);

        ImageView imgMinus = view.findViewById(R.id.imgMinus);
        ImageView imgPlus = view.findViewById(R.id.imgPlus);

        autoReplyTextPreviewCard.setOnClickListener(this::openCustomReplyEditorActivity);
        autoReplyTextPreview.setText(customRepliesData.getTextToSendOrElse());
        // Enable group chat switch only if main switch id ON
        groupReplySwitch.setEnabled(mainAutoReplySwitch.isChecked());

        mainAutoReplySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !isListenerEnabled(mActivity, NotificationService.class)) {
//                launchNotificationAccessSettings();
                showPermissionsDialog();
            } else {
                preferencesManager.setServicePref(isChecked);
                if (isChecked) {
                    startNotificationService();
                } else {
                    stopNotificationService();
                }
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
            if (preferencesManager.isGroupReplyEnabled() == isChecked) {
                return;
            }

            if (isChecked) {
                Toast.makeText(mActivity, R.string.group_reply_on_info_message, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mActivity, R.string.group_reply_off_info_message, Toast.LENGTH_SHORT).show();
            }
            preferencesManager.setGroupReplyPref(isChecked);
        });

        imgMinus.setOnClickListener(v -> {
            if (days > MIN_DAYS) {
                days--;
                saveNumDays();
            }
        });

        imgPlus.setOnClickListener(v -> {
            if (days < MAX_DAYS) {
                days++;
                saveNumDays();
            }
        });

        setNumDays();

        return view;
    }

    private List<App> getEnabledApps() {
        if (enabledApps != null) {
            enabledApps.clear();
        }
        enabledApps = new ArrayList<>();
        for (App app : supportedApps) {
            if(preferencesManager.isAppEnabled(app)){
                enabledApps.add(app);
            }
        }
        return enabledApps;
    }

    public static int getSpanCount(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        int scalingFactor = 35; // You can vary the value held by the scalingFactor
        return (int) (dpWidth / scalingFactor);
    }

    private void enableOrDisableEnabledAppsCheckboxes(boolean enabled) {
        for (MaterialCheckBox checkbox : supportedAppsCheckboxes) {
            checkbox.setEnabled(enabled);
        }
        for (View dummyView : supportedAppsDummyViews) {
            dummyView.setVisibility(enabled ? View.GONE : View.VISIBLE);
        }
    }


    private void saveNumDays() {
        preferencesManager.setAutoReplyDelay((long) days * 24 * 60 * 60 * 1000);//Save in Milliseconds
        setNumDays();
    }

    private void setNumDays() {
        long timeDelay = (preferencesManager.getAutoReplyDelay() / (60 * 1000));//convert back to minutes
        days = (int) timeDelay / (60 * 24);//convert back to days
        if (days == 0) {
            timeSelectedTextPreview.setText("•");
            timePickerSubTitleTextPreview.setText(R.string.time_picker_sub_title_default);
        } else {
            timeSelectedTextPreview.setText(String.valueOf(days));
            timePickerSubTitleTextPreview.setText(String.format(getResources().getString(R.string.time_picker_sub_title), days));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //If user directly goes to Settings and removes notifications permission
        //when app is launched check for permission and set appropriate app state
        if (!isListenerEnabled(mActivity, NotificationService.class)) {
            preferencesManager.setServicePref(false);
        }

        setSwitchState();

        // set group chat switch state
        groupReplySwitch.setChecked(preferencesManager.isGroupReplyEnabled());

        // Set user auto reply text
        autoReplyTextPreview.setText(customRepliesData.getTextToSendOrElse());

        // Update enabled apps list
        if (supportedAppsAdapter != null) {
            supportedAppsAdapter.updateList(getEnabledApps());
        }

        showAppRatingPopup();

    }

    private void showAppRatingPopup() {
        boolean isFromStore = isAppInstalledFromStore(mActivity);
        String status = preferencesManager.getPlayStoreRatingStatus();
        long ratingLastTime = preferencesManager.getPlayStoreRatingLastTime();
        if (isFromStore && !status.equals("Not Interested") && !status.equals("DONE") && ((System.currentTimeMillis() - ratingLastTime) > (10 * 24 * 60 * 60 * 1000L))) {
            if (isAppUsedSufficientlyToAskRating()) {
                CustomDialog customDialog = new CustomDialog(mActivity);
                customDialog.showAppLocalRatingDialog(v -> showFeedbackPopup((int) v.getTag()));
                preferencesManager.setPlayStoreRatingLastTime(System.currentTimeMillis());
            }
        }
    }

    //REF: https://stackoverflow.com/questions/37539949/detect-if-an-app-is-installed-from-play-store
    public static boolean isAppInstalledFromStore(Context context) {
        // A list with valid installers package name
        List<String> validInstallers = new ArrayList<>(Arrays.asList("com.android.vending", "com.google.android.feedback"));

        // The package name of the app that has installed your app
        final String installer = context.getPackageManager().getInstallerPackageName(context.getPackageName());

        // true if your app has been downloaded from Play Store
        return installer != null && validInstallers.contains(installer);
    }

    private boolean isAppUsedSufficientlyToAskRating() {
        long firstRepliedTime = dbUtils.getFirstRepliedTime();
        return firstRepliedTime > 0 && System.currentTimeMillis() - firstRepliedTime > 2 * 24 * 60 * 60 * 1000L && dbUtils.getNunReplies() >= MIN_REPLIES_TO_ASK_APP_RATING;
    }

    private void showFeedbackPopup(int rating) {
        CustomDialog customDialog = new CustomDialog(mActivity);
        customDialog.showAppRatingDialog(rating, v -> {
            String tag = (String) v.getTag();
            if (tag.equals(mActivity.getResources().getString(R.string.app_rating_goto_store_dialog_button1_title))) {
                //not interested
                preferencesManager.setPlayStoreRatingStatus("Not Interested");
            } else if (tag.equals(mActivity.getResources().getString(R.string.app_rating_goto_store_dialog_button2_title))) {
                //Launch playstore rating page
                rateApp();
            } else if (tag.equals(mActivity.getResources().getString(R.string.app_rating_feedback_dialog_mail_button_title))) {
                launchEmailCompose();
            } else if (tag.equals(mActivity.getResources().getString(R.string.app_rating_feedback_dialog_telegram_button_title))) {
                launchFeedbackApp();
            }
        });
    }

    private void launchEmailCompose() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setDataAndType(Uri.parse("mailto:"), "plain/text"); // only email apps should handle this
        intent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{Constants.EMAIL_ADDRESS});
        intent.putExtra(Intent.EXTRA_SUBJECT, Constants.EMAIL_SUBJECT);
        if (intent.resolveActivity(mActivity.getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    private void launchFeedbackApp() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            launchAppLegacy();
            return;
        }
        boolean isLaunched;
        try {
            // In order for this intent to be invoked, the system must directly launch a non-browser app.
            // Ref: https://developer.android.com/training/package-visibility/use-cases#avoid-a-disambiguation-dialog
            Intent intent = new Intent(ACTION_VIEW, Uri.parse(Constants.TELEGRAM_URL))
                    .addCategory(CATEGORY_BROWSABLE)
                    .setFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_REQUIRE_NON_BROWSER |
                            FLAG_ACTIVITY_REQUIRE_DEFAULT);
            mActivity.startActivity(intent);
            isLaunched = true;
        } catch (ActivityNotFoundException e) {
            // This code executes in one of the following cases:
            // 1. Only browser apps can handle the intent.
            // 2. The user has set a browser app as the default app.
            // 3. The user hasn't set any app as the default for handling this URL.
            isLaunched = false;
        }
        if (!isLaunched) { // Open Github latest release url in browser if everything else fails
            String url = getString(R.string.watomatic_github_latest_release_url);
            mActivity.startActivity(new Intent(ACTION_VIEW).setData(Uri.parse(url)));
        }
    }

    private void launchAppLegacy() {
        Intent intent = new Intent(ACTION_VIEW, Uri.parse(Constants.TELEGRAM_URL));
        List<ResolveInfo> list = getActivity().getPackageManager()
                .queryIntentActivities(intent, 0);
        List<ResolveInfo> possibleBrowserIntents = getActivity().getPackageManager()
                .queryIntentActivities(new Intent(ACTION_VIEW, Uri.parse("http://www.deekshith.in/")), 0);
        Set<String> excludeIntents = new HashSet<>();
        for (ResolveInfo eachPossibleBrowserIntent : possibleBrowserIntents) {
            excludeIntents.add(eachPossibleBrowserIntent.activityInfo.name);
        }
        //Check for non browser application
        for (ResolveInfo resolveInfo : list) {
            if (!excludeIntents.contains(resolveInfo.activityInfo.name)) {
                intent.setPackage(resolveInfo.activityInfo.packageName);
                mActivity.startActivity(intent);
                break;
            }
        }
    }

    /*
     * Start with rating the app
     * Determine if the Play Store is installed on the device
     *
     * */
    public void rateApp() {
        try {
            Intent rateIntent = rateIntentForUrl("market://details");
            startActivity(rateIntent);
        } catch (ActivityNotFoundException e) {
            Intent rateIntent = rateIntentForUrl("https://play.google.com/store/apps/details");
            startActivity(rateIntent);
        }
    }

    private Intent rateIntentForUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format("%s?id=%s", url, BuildConfig.APPLICATION_ID)));
        int flags = Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_ACTIVITY_NEW_DOCUMENT;
        intent.addFlags(flags);
        return intent;
    }

    private void setSwitchState() {
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

    private void showPermissionsDialog() {
        CustomDialog customDialog = new CustomDialog(mActivity);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.PERMISSION_DIALOG_TITLE, getString(R.string.permission_dialog_title));
        bundle.putString(Constants.PERMISSION_DIALOG_MSG, getString(R.string.permission_dialog_msg));
        customDialog.showDialog(bundle, null, (dialog, which) -> {
            if (which == -2) {
                //Decline
                showPermissionDeniedDialog();
            } else {
                //Accept
                launchNotificationAccessSettings();
            }
        });
    }

    private void showPermissionDeniedDialog() {
        CustomDialog customDialog = new CustomDialog(mActivity);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.PERMISSION_DIALOG_DENIED_TITLE, getString(R.string.permission_dialog_denied_title));
        bundle.putString(Constants.PERMISSION_DIALOG_DENIED_MSG, getString(R.string.permission_dialog_denied_msg));
        bundle.putBoolean(Constants.PERMISSION_DIALOG_DENIED, true);
        customDialog.showDialog(bundle, null, (dialog, which) -> {
            if (which == -2) {
                //Decline
                setSwitchState();
            } else {
                //Accept
                launchNotificationAccessSettings();
            }
        });
    }

    public void launchNotificationAccessSettings() {
        //We should remove it few versions later
        enableService(true);//we need to enable the service for it so show in settings

        final String NOTIFICATION_LISTENER_SETTINGS;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            NOTIFICATION_LISTENER_SETTINGS = ACTION_NOTIFICATION_LISTENER_SETTINGS;
        } else {
            NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
        }
        Intent i = new Intent(NOTIFICATION_LISTENER_SETTINGS);
        startActivityForResult(i, REQ_NOTIFICATION_LISTENER);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_NOTIFICATION_LISTENER) {
            if (isListenerEnabled(mActivity, NotificationService.class)) {
                Toast.makeText(mActivity, "Permission Granted", Toast.LENGTH_LONG).show();
                startNotificationService();
                preferencesManager.setServicePref(true);
            } else {
                Toast.makeText(mActivity, "Permission Denied", Toast.LENGTH_LONG).show();
                preferencesManager.setServicePref(false);
            }
            setSwitchState();
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

    private void startNotificationService() {
        if (preferencesManager.isForegroundServiceNotificationEnabled()) {
            ServieUtils.getInstance(mActivity).startNotificationService();
        }
    }

    private void stopNotificationService() {
        ServieUtils.getInstance(mActivity).stopNotificationService();
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) mActivity.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i("isMyServiceRunning?", true + "");
                return true;
            }
        }
        Log.i("isMyServiceRunning?", false + "");
        return false;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        mActivity.getMenuInflater().inflate(R.menu.main_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.about) {
            openAboutActivity();
        } else if (item.getItemId() == R.id.setting) {
            loadSettingsActivity();
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadSettingsActivity() {
        Intent intent = new Intent(mActivity, SettingsActivity.class);
        mActivity.startActivity(intent);
    }

    private void launchEnabledAppsActivity() {
        Intent intent = new Intent(mActivity, EnabledAppsActivity.class);
        mActivity.startActivity(intent);
    }

    @Override
    public void onDestroy() {
        stopNotificationService();
        super.onDestroy();
    }

}
