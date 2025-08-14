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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.parishod.watomatic.BuildConfig;
import com.parishod.watomatic.NotificationService;
import com.parishod.watomatic.R;
import com.parishod.watomatic.activity.about.AboutActivity;
import com.parishod.watomatic.activity.contactselector.ContactSelectorActivity;
import com.parishod.watomatic.activity.customreplyeditor.CustomReplyEditorActivity;
import com.parishod.watomatic.activity.enabledapps.EnabledAppsActivity;
import com.parishod.watomatic.activity.main.MainActivity;
import com.parishod.watomatic.activity.settings.SettingsActivity;
import com.parishod.watomatic.model.App;
import com.parishod.watomatic.model.CustomRepliesData;
import com.parishod.watomatic.model.data.AppItem;
import com.parishod.watomatic.model.data.CooldownItem;
import com.parishod.watomatic.model.data.DialogConfig;
import com.parishod.watomatic.model.data.MessageTypeItem;
import com.parishod.watomatic.model.enums.DialogType;
import com.parishod.watomatic.model.interfaces.DialogActionListener;
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
import static com.parishod.watomatic.model.utils.Constants.MIN_REPLIES_TO_ASK_APP_RATING;

public class MainFragment extends Fragment implements DialogActionListener {

    private static final int REQ_NOTIFICATION_LISTENER = 100;
    private static final int NOTIFICATION_REQUEST_CODE = 101;
    private PreferencesManager preferencesManager;
    private Activity mActivity;
    private CustomRepliesData customRepliesData;
    private SwitchMaterial autoRepliesSwitch;
    private TextView aiReplyText;
    private View view;
    BottomNavigationView bottomNav;
    Button editButton;
    private int gitHubReleaseNotesId = -1;
    private int selectedCooldownTime;
    private TextView replyCooldownDescription;
    private LinearLayout contactsFilterLL, messagesTypeLL, supportedAppsLL, replyCooldownLL;
    private final List<String> communityUrls = Arrays.asList("https://t.me/WatomaticApp",
            "https://fosstodon.org/@watomatic",
            "https://twitter.com/watomatic",
            "https://www.reddit.com/r/watomatic");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_main_redesigned, container, false);

        setHasOptionsMenu(true);

        mActivity = getActivity();

        customRepliesData = CustomRepliesData.getInstance(mActivity);
        preferencesManager = PreferencesManager.getPreferencesInstance(mActivity);

        // Assign Views
        aiReplyText = view.findViewById(R.id.ai_reply_text);
        autoRepliesSwitch = view.findViewById(R.id.switch_auto_replies);
        editButton = view.findViewById(R.id.btn_edit);
        bottomNav = view.findViewById(R.id.bottom_nav);

        // Setup AI Reply
        aiReplyText.setText(customRepliesData.getTextToSendOrElse());

        //Filters Layout views
        replyCooldownDescription = view.findViewById(R.id.reply_cooldown_description);
        contactsFilterLL = view.findViewById(R.id.filter_contacts);
        contactsFilterLL.setOnClickListener(view -> {
            startActivity(new Intent(mActivity, ContactSelectorActivity.class));
        });

        messagesTypeLL = view.findViewById(R.id.filter_message_type);
        messagesTypeLL.setOnClickListener(view -> {
            showMessageTypeDialog();
        });

        supportedAppsLL = view.findViewById(R.id.filter_apps);
        supportedAppsLL.setOnClickListener(view -> {
            showAppsDialog();
        });

        replyCooldownLL = view.findViewById(R.id.filter_reply_cooldown);
        replyCooldownLL.setOnClickListener(view -> {
            showCooldownDialog();
        });

        // Setup Auto-replies switch
        autoRepliesSwitch.setChecked(preferencesManager.isServiceEnabled());
        autoRepliesSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !isListenerEnabled(mActivity, NotificationService.class)) {
                showPermissionsDialog();
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (!isPostNotificationPermissionGranted()) {
                        checkNotificationPermission();
                        return;
                    }
                }
                preferencesManager.setServicePref(isChecked);
                if (isChecked) {
                    startNotificationService();
                } else {
                    stopNotificationService();
                }
                setSwitchState();
            }
        });

        // Setup Edit button
        editButton.setOnClickListener(v -> openCustomReplyEditorActivity(v));

        // Setup Bottom Navigation
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_atomatic) {
                // Already on this screen
                return true;
            } else if (itemId == R.id.navigation_community) {
                // Handle community navigation
                launchApp(communityUrls, getString(R.string.watomatic_subreddit_url));
                return true;
            } else if (itemId == R.id.navigation_settings) {
                // Handle settings navigation
                loadSettingsActivity();
                return true;
            }
            return false;
        });

        if (!isPostNotificationPermissionGranted()) {
            checkNotificationPermission();
        }

        return view;
    }

    private void launchApp(List<String> urls, String fallbackUrl) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            launchAppLegacy(urls, fallbackUrl);
            return;
        }
        boolean isLaunched = false;
        for (String eachReleaseUrl : urls) {
            if (isLaunched) {
                break;
            }
            try {
                // In order for this intent to be invoked, the system must directly launch a non-browser app.
                // Ref: https://developer.android.com/training/package-visibility/use-cases#avoid-a-disambiguation-dialog
                Intent intent = new Intent(ACTION_VIEW, Uri.parse(eachReleaseUrl))
                        .setFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_REQUIRE_NON_BROWSER |
                                FLAG_ACTIVITY_REQUIRE_DEFAULT);
                startActivity(intent);
                isLaunched = true;
            } catch (ActivityNotFoundException e) {
                // This code executes in one of the following cases:
                // 1. Only browser apps can handle the intent.
                // 2. The user has set a browser app as the default app.
                // 3. The user hasn't set any app as the default for handling this URL.
                isLaunched = false;
            }
        }
        if (!isLaunched) { // Open Github latest release url in browser if everything else fails
            startActivity(new Intent(ACTION_VIEW).setData(Uri.parse(fallbackUrl)));
        }
    }

    private void launchAppLegacy(List<String> urls, String fallbackUrl) {
        boolean isLaunched = false;
        for (String url : urls) {
            Intent intent = new Intent(ACTION_VIEW, Uri.parse(url));
            List<ResolveInfo> list = getActivity() != null ?
                    getActivity().getPackageManager().queryIntentActivities(intent, 0) :
                    null;
            List<ResolveInfo> possibleBrowserIntents = getActivity() != null ?
                    getActivity().getPackageManager()
                            .queryIntentActivities(new Intent(ACTION_VIEW, Uri.parse("http://www.deekshith.in/")), 0) :
                    null;

            Set<String> excludeIntents = new HashSet<>();
            if (possibleBrowserIntents != null) {
                for (ResolveInfo eachPossibleBrowserIntent : possibleBrowserIntents) {
                    excludeIntents.add(eachPossibleBrowserIntent.activityInfo.name);
                }
            }

            //Check for non browser application
            if (list != null) {
                for (ResolveInfo resolveInfo : list) {
                    if (!excludeIntents.contains(resolveInfo.activityInfo.name)) {
                        intent.setPackage(resolveInfo.activityInfo.packageName);
                        startActivity(intent);
                        isLaunched = true;
                        break;
                    }
                }
            }

            if (isLaunched) {
                break;
            }
        }
        if (!isLaunched) { // Open Github latest release url in browser if everything else fails
            startActivity(new Intent(ACTION_VIEW).setData(Uri.parse(fallbackUrl)));
        }
    }


    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(mActivity, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_REQUEST_CODE);
        }
    }

    private boolean isPostNotificationPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(mActivity, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void showPostNotificationPermissionDeniedSnackbar(View view) {
        Snackbar.make(view, mActivity.getResources().getString(R.string.post_notification_permission_snackbar_text), Snackbar.LENGTH_INDEFINITE)
                .setAction(mActivity.getResources().getString(R.string.post_notification_permission_snackbar_setting), view1 -> {
                    // Open app settings
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", view1.getContext().getPackageName(), null);
                    intent.setData(uri);
                    view1.getContext().startActivity(intent);
                }).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == NOTIFICATION_REQUEST_CODE) {
            // If permission is granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Displaying a toast
            } else {
                // Displaying another toast if permission is not granted
                showPostNotificationPermissionDeniedSnackbar(autoRepliesSwitch);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(bottomNav != null){
            bottomNav.setSelectedItemId(R.id.navigation_atomatic);
        }
        //If user directly goes to Settings and removes notifications permission
        //when app is launched check for permission and set appropriate app state
        if (!isListenerEnabled(mActivity, NotificationService.class)) {
            preferencesManager.setServicePref(false);
        }

        setSwitchState();

        // Set user auto reply text
        aiReplyText.setText(customRepliesData.getTextToSendOrElse());

        updateCooldownFilterDisplay();
        showAppRatingPopup();
    }

    private void updateCooldownFilterDisplay() {
        long cooldownInMillis = preferencesManager.getAutoReplyDelay();
        long minutes = cooldownInMillis / (60 * 1000);
        if (minutes == 0) {
            replyCooldownDescription.setText(R.string.no_cooldown);
            return;
        }
        long hours = minutes / 60;
        minutes = minutes % 60;

        StringBuilder cooldownText = new StringBuilder();
        if (hours > 0) {
            cooldownText.append(hours).append(" ").append(getResources().getString(hours > 1 ? R.string.hours : R.string.hour)).append(" ");
        }
        if (minutes > 0) {
            cooldownText.append(minutes).append(" ").append(getResources().getString(minutes > 1 ? R.string.minutes : R.string.minute));
        }
        replyCooldownDescription.setText(cooldownText.toString().trim());
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

        try {
            // The package name of the app that has installed your app
            final String installer = context.getPackageManager().getInstallerPackageName(context.getPackageName());

            // true if your app has been downloaded from Play Store
            return installer != null && validInstallers.contains(installer);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isAppUsedSufficientlyToAskRating() {
        DbUtils dbUtils = new DbUtils(mActivity);
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
        if (getActivity() != null) {
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
        autoRepliesSwitch.setChecked(preferencesManager.isServiceEnabled());
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S || preferencesManager.isForegroundServiceNotificationEnabled()) {
            ServieUtils.getInstance(mActivity).startNotificationService();
        }
    }


    private void stopNotificationService() {
        ServieUtils.getInstance(mActivity).stopNotificationService();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        // We are using a Toolbar in the layout, so we don't need to inflate a menu here.
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

    @Override
    public void onDestroy() {
        stopNotificationService();
        super.onDestroy();
    }

    // Dialog 1: Apps with toggles and search
    private void showAppsDialog() {

        Set<App> supportedApps = Constants.SUPPORTED_APPS;
        List<AppItem> appItems = new ArrayList<>();

        for (App app : supportedApps) {
            AppItem item = new AppItem(
                    R.drawable.ic_android_default_round,
                    app.getName(),
                    app.getPackageName(),
                    "Auto-reply disabled", // or make this dynamic
                    app.getPackageName().equals("com.whatsapp") ? true : false
            );
            appItems.add(item);
        }

        DialogConfig config = new DialogConfig(
                DialogType.APPS,
                "Apps",
                "", // description not needed for this dialog
                true, // showSearch
                "Search",
                "Search apps",
                appItems
        );

        UniversalDialogFragment dialog = UniversalDialogFragment.Companion.newInstance(config);
        dialog.setActionListener(this);
        dialog.show(((MainActivity) mActivity).getSupportFragmentManager(), "apps_dialog");
    }

    // Dialog 2: Message Type with radio buttons
    private void showMessageTypeDialog() {
        List<MessageTypeItem> messageTypes = Arrays.asList(
                new MessageTypeItem("Personal Messages", true),  // Pre-selected
                new MessageTypeItem("Group Messages", false),
                new MessageTypeItem("Messages from Unknown Senders", false)
        );

        DialogConfig config = new DialogConfig(
                DialogType.MESSAGE_TYPE,
                "Message Type",
                "Select message types",
                false, // showSearch not needed
                "Search",
                "Save",  // searchHint not needed
                messageTypes
        );

        UniversalDialogFragment dialog = UniversalDialogFragment.Companion.newInstance(config);
        dialog.setActionListener(this);
        dialog.show(((MainActivity) mActivity).getSupportFragmentManager(), "message_type_dialog");
    }

    // Dialog 3: Cooldown with selection boxes
    private void showCooldownDialog() {
        List<CooldownItem> cooldownOptions = new ArrayList<>();
        long cooldownInMinutes = preferencesManager.getAutoReplyDelay() / (60 * 1000);
        cooldownOptions.add(new CooldownItem((int) cooldownInMinutes));

        DialogConfig config = new DialogConfig(
                DialogType.COOLDOWN,
                "Reply Cooldown",
                "Set a minimum time interval between automatic replies to the same contact. " +
                        "This prevents sending multiple replies in quick succession.",
                false, // showSearch not needed
                "",    // searchHint not needed
                "Start",
                cooldownOptions
        );

        UniversalDialogFragment dialog = UniversalDialogFragment.Companion.newInstance(config);
        dialog.setActionListener(this);
        dialog.show(((MainActivity) mActivity).getSupportFragmentManager(), "cooldown_dialog");
    }

    @Override
    public void onSaveClicked(DialogType dialogType) {
        if (dialogType == DialogType.COOLDOWN) {
            long cooldownInMillis = selectedCooldownTime * 60 * 1000L;
            preferencesManager.setAutoReplyDelay(cooldownInMillis);
            Toast.makeText(mActivity, "Cooldown settings saved", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onItemToggled(int position, boolean isChecked) {
        // Handle toggle switches (for Apps and Contacts)
        Log.d("Dialog", "Item at position " + position + " toggled: " + isChecked);
    }

    @Override
    public void onItemSelected(int position, boolean isSelected) {
        // Handle radio button selections (for Message Type and Cooldown)
        Log.d("Dialog", "Item at position " + position + " selected: " + isSelected);
    }

    @Override
    public void onSearchQuery(String query) {
        // Handle search queries
        Log.d("Dialog", "Search query: " + query);
        // You can filter your data here and update the adapter
    }

    @Override
    public void onCooldownChanged(int totalMinutes) {
        selectedCooldownTime = totalMinutes;
        Log.d("Dialog", "Total cooldown time: " + totalMinutes);
    }
}
