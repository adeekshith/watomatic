package com.parishod.watomatic.model.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import androidx.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.parishod.watomatic.R;
import com.parishod.watomatic.model.App;
import com.parishod.watomatic.model.utils.AppUtils;
import com.parishod.watomatic.model.utils.Constants;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class PreferencesManager {
    private final String KEY_SERVICE_ENABLED = "pref_service_enabled";
    private final String KEY_GROUP_REPLY_ENABLED = "pref_group_reply_enabled";
    private final String KEY_AUTO_REPLY_THROTTLE_TIME_MS = "pref_auto_reply_throttle_time_ms";
    private final String KEY_SELECTED_APPS_ARR = "pref_selected_apps_arr";
    private final String KEY_IS_APPEND_WATOMATIC_ATTRIBUTION = "pref_is_append_watomatic_attribution";
    private final String KEY_GITHUB_RELEASE_NOTES_ID = "pref_github_release_notes_id";
    private final String KEY_PURGE_MESSAGE_LOGS_LAST_TIME = "pref_purge_message_logs_last_time";
    private final String KEY_PLAY_STORE_RATING_STATUS = "pref_play_store_rating_status";
    private final String KEY_PLAY_STORE_RATING_LAST_TIME = "pref_play_store_rating_last_time";
    private final String KEY_SHOW_FOREGROUND_SERVICE_NOTIFICATION = "pref_show_foreground_service_notification";
    private final String KEY_REPLY_CONTACTS = "pref_reply_contacts";
    private final String KEY_REPLY_CONTACTS_TYPE = "pref_reply_contacts_type";
    private final String KEY_REPLY_CUSTOM_NAMES = "pref_reply_custom_names";
    private final String KEY_SELECTED_CONTACT_NAMES = "pref_selected_contacts_names";
    private String KEY_IS_SHOW_NOTIFICATIONS_ENABLED;
    private String KEY_SELECTED_APP_LANGUAGE;
    private static PreferencesManager _instance;
    private SharedPreferences _sharedPrefs;
    private Context thisAppContext;

    private PreferencesManager(Context context) {
        thisAppContext = context;
        _sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        init();
    }

    public static PreferencesManager getPreferencesInstance(Context context){
        if(_instance == null){
            _instance = new PreferencesManager(context.getApplicationContext());
        }
        return _instance;
    }

    /**
     * Execute this code when the singleton is first created. All the tasks that needs to be done
     * when the instance is first created goes here. For example, set specific keys based on new install
     * or app upgrade, etc.
     */
    private void init () {
        // Use key from string resource
        KEY_SELECTED_APP_LANGUAGE = thisAppContext.getString(R.string.key_pref_app_language);
        KEY_IS_SHOW_NOTIFICATIONS_ENABLED = thisAppContext.getString(R.string.pref_show_notification_replied_msg);

        // For new installs, enable all the supported apps
        boolean newInstall = !_sharedPrefs.contains(KEY_SERVICE_ENABLED)
                && !_sharedPrefs.contains(KEY_SELECTED_APPS_ARR);
        if (newInstall) {
            // Enable all supported apps for new install
            setAppsAsEnabled(Constants.SUPPORTED_APPS);

            // Set notifications ON for new installs
            setShowNotificationPref(true);
        }

        if (isFirstInstall(thisAppContext)) {
            // Set Append Watomatic attribution checked for new installs
            if (!_sharedPrefs.contains(KEY_IS_APPEND_WATOMATIC_ATTRIBUTION)) {
                setAppendWatomaticAttribution(true);
            }
        }
        else {
            //If it's first install, language preference is not set, so we don't have to worry
            //Otherwise, check if language settings contains r, migrate to new language settings key
            updateLegacyLanguageKey();
        }
    }

    public boolean isServiceEnabled(){
        return _sharedPrefs.getBoolean(KEY_SERVICE_ENABLED,false);
    }

    public void setServicePref(boolean enabled){
        SharedPreferences.Editor editor = _sharedPrefs.edit();
        editor.putBoolean(KEY_SERVICE_ENABLED, enabled);
        editor.apply();
    }

    public boolean isGroupReplyEnabled(){
        return _sharedPrefs.getBoolean(KEY_GROUP_REPLY_ENABLED,false);
    }

    public void setGroupReplyPref(boolean enabled){
        SharedPreferences.Editor editor = _sharedPrefs.edit();
        editor.putBoolean(KEY_GROUP_REPLY_ENABLED, enabled);
        editor.apply();
    }

    public long getAutoReplyDelay(){
        return _sharedPrefs.getLong(KEY_AUTO_REPLY_THROTTLE_TIME_MS,0);
    }

    public void setAutoReplyDelay(long delay){
        SharedPreferences.Editor editor = _sharedPrefs.edit();
        editor.putLong(KEY_AUTO_REPLY_THROTTLE_TIME_MS, delay);
        editor.apply();
    }

    public Set<String> getEnabledApps(){
        String enabledAppsJsonStr = _sharedPrefs.getString(KEY_SELECTED_APPS_ARR, null);

        // Users upgrading from v1.7 and before
        // For upgrading users, preserve functionality by enabling only WhatsApp
        //   (remove this when time most users would have updated. May be in 3 weeks after deploying this?)
        if (enabledAppsJsonStr == null || enabledAppsJsonStr.equals("[]")) {
            enabledAppsJsonStr = setAppsAsEnabled(Collections.singleton(new App("WhatsApp", "com.whatsapp")));
        }

        Type type = new TypeToken<Set<String>>(){}.getType();
        return new Gson().fromJson(enabledAppsJsonStr, type);
    }

    public boolean isAppEnabled (App thisApp) {
        return getEnabledApps().contains(thisApp.getPackageName());
    }

    private String serializeAndSetEnabledPackageList (Collection<String> packageList) {
        String jsonStr = new Gson().toJson(packageList);
        SharedPreferences.Editor editor = _sharedPrefs.edit();
        editor.putString(KEY_SELECTED_APPS_ARR, jsonStr);
        editor.apply();
        return jsonStr;
    }

    public String setAppsAsEnabled (Collection<App> apps) {
        AppUtils appUtils = AppUtils.getInstance(thisAppContext);
        Set<String> packageNames = new HashSet<>();
        for (App app: apps) {
            //check if the app is installed only then add it to enabled list
            if(appUtils.isPackageInstalled(app.getPackageName())) {
                packageNames.add(app.getPackageName());
            }
        }
        return serializeAndSetEnabledPackageList(packageNames);
    }

    public String saveEnabledApps(App app, boolean isSelected){
        Set<String> enabledPackages = getEnabledApps();
        if(!isSelected) {
            //remove the given platform
            enabledPackages.remove(app.getPackageName());
        }else{
            //add the given platform
            enabledPackages.add(app.getPackageName());
        }
        return serializeAndSetEnabledPackageList(enabledPackages);
    }

    public void setAppendWatomaticAttribution(boolean enabled) {
        SharedPreferences.Editor editor = _sharedPrefs.edit();
        editor.putBoolean(KEY_IS_APPEND_WATOMATIC_ATTRIBUTION, enabled);
        editor.apply();
    }

    public boolean isAppendWatomaticAttributionEnabled() {
        return _sharedPrefs.getBoolean(KEY_IS_APPEND_WATOMATIC_ATTRIBUTION,false);
    }

    /**
     * Check if it is first install on this device.
     * ref: https://stackoverflow.com/a/34194960 
     * @param context
     * @return true if first install or else false if it is installed from an update
     */
    public static boolean isFirstInstall(Context context) {
        try {
            long firstInstallTime = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).firstInstallTime;
            long lastUpdateTime = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).lastUpdateTime;
            return firstInstallTime == lastUpdateTime;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return true;
        }
    }

    public String getSelectedLanguageStr(String defaultLangStr){
        return _sharedPrefs.getString(KEY_SELECTED_APP_LANGUAGE, defaultLangStr);
    }

    public void setLanguageStr(String languageStr) {
        SharedPreferences.Editor editor = _sharedPrefs.edit();
        editor.putString(KEY_SELECTED_APP_LANGUAGE, languageStr);
        editor.apply();
    }

    public Locale getSelectedLocale () {
        String thisLangStr = getSelectedLanguageStr(null);
        if (thisLangStr == null || thisLangStr.isEmpty()) {
            return Locale.getDefault();
        }
        String[] languageSplit = thisLangStr.split("-");
        return (languageSplit.length == 2)
                ? new Locale(languageSplit[0], languageSplit[1])
                : new Locale(languageSplit[0]);
    }

    public void updateLegacyLanguageKey() {
        String thisLangStr = getSelectedLanguageStr(null);
        if (thisLangStr == null || thisLangStr.isEmpty()) {
            return;
        }
        String[] languageSplit = thisLangStr.split("-");
        if (languageSplit.length == 2) {
            if (languageSplit[1].length() == 3) {
                String newLangStr = thisLangStr.replace("-r", "-");
                setLanguageStr(newLangStr);
            }
        }
    }

    public boolean isShowNotificationEnabled(){
        return _sharedPrefs.getBoolean(KEY_IS_SHOW_NOTIFICATIONS_ENABLED,false);
    }

    public void setShowNotificationPref(boolean enabled){
        SharedPreferences.Editor editor = _sharedPrefs.edit();
        editor.putBoolean(KEY_IS_SHOW_NOTIFICATIONS_ENABLED, enabled);
        editor.apply();
    }

    public int getGithubReleaseNotesId(){
        return _sharedPrefs.getInt(KEY_GITHUB_RELEASE_NOTES_ID,0);
    }

    public void setGithubReleaseNotesId(int id){
        SharedPreferences.Editor editor = _sharedPrefs.edit();
        editor.putInt(KEY_GITHUB_RELEASE_NOTES_ID, id);
        editor.apply();
    }

    public long getLastPurgedTime(){
        return _sharedPrefs.getLong(KEY_PURGE_MESSAGE_LOGS_LAST_TIME,0);
    }

    public void setPurgeMessageTime(long purgeMessageTime){
        SharedPreferences.Editor editor = _sharedPrefs.edit();
        editor.putLong(KEY_PURGE_MESSAGE_LOGS_LAST_TIME, purgeMessageTime);
        editor.apply();
    }

    public String getPlayStoreRatingStatus(){
        return _sharedPrefs.getString(KEY_PLAY_STORE_RATING_STATUS, "");
    }

    public void setPlayStoreRatingStatus(String status){
        SharedPreferences.Editor editor = _sharedPrefs.edit();
        editor.putString(KEY_PLAY_STORE_RATING_STATUS, status);
        editor.apply();
    }

    public long getPlayStoreRatingLastTime(){
        return _sharedPrefs.getLong(KEY_PLAY_STORE_RATING_LAST_TIME,0);
    }

    public void setPlayStoreRatingLastTime(long purgeMessageTime){
        SharedPreferences.Editor editor = _sharedPrefs.edit();
        editor.putLong(KEY_PLAY_STORE_RATING_LAST_TIME, purgeMessageTime);
        editor.apply();
    }

    public void setShowForegroundServiceNotification(boolean enabled) {
        SharedPreferences.Editor editor = _sharedPrefs.edit();
        editor.putBoolean(KEY_SHOW_FOREGROUND_SERVICE_NOTIFICATION, enabled);
        editor.apply();
    }

    public boolean isForegroundServiceNotificationEnabled() {
        return _sharedPrefs.getBoolean(KEY_SHOW_FOREGROUND_SERVICE_NOTIFICATION,false);
    }

    public void setReplyToNames(Set<String> names) {
        SharedPreferences.Editor editor = _sharedPrefs.edit();
        editor.putStringSet(KEY_SELECTED_CONTACT_NAMES, names);
        editor.apply();
    }

    public Set<String> getReplyToNames() {
        return _sharedPrefs.getStringSet(KEY_SELECTED_CONTACT_NAMES, new HashSet<>());
    }

    public Set<String> getCustomReplyNames() {
        return _sharedPrefs.getStringSet(KEY_REPLY_CUSTOM_NAMES, new HashSet<>());
    }

    public void setCustomReplyNames(Set<String> names) {
        SharedPreferences.Editor editor = _sharedPrefs.edit();
        editor.putStringSet(KEY_REPLY_CUSTOM_NAMES, names);
        editor.apply();
    }

    public boolean isContactReplyEnabled() {
        return _sharedPrefs.getBoolean(KEY_REPLY_CONTACTS, false);
    }

    public Boolean isContactReplyBlacklistMode() {
        return _sharedPrefs.getString(KEY_REPLY_CONTACTS_TYPE, "pref_blacklist").equals("pref_blacklist");
    }

}
