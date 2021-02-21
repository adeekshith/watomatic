package com.parishod.watomatic.model.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PreferencesManager {
    private final String KEY_SERVICE_ENABLED = "pref_service_enabled";
    private final String KEY_GROUP_REPLY_ENABLED = "pref_group_reply_enabled";
    private final String KEY_AUTO_REPLY_THROTTLE_TIME_MS = "pref_auto_reply_throttle_time_ms";
    private final String KEY_SELECTED_APPS_ARR = "pref_selected_apps_arr";
    private static PreferencesManager _instance;
    private SharedPreferences _sharedPrefs;
    private PreferencesManager(Context context) {
        _sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static PreferencesManager getPreferencesInstance(Context context){
        if(_instance == null){
            _instance = new PreferencesManager(context.getApplicationContext());
        }
        return _instance;
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
        String enabledApps = _sharedPrefs.getString(KEY_SELECTED_APPS_ARR, "");
        //string to list is adding [ & ] so remove them
        enabledApps = enabledApps.replace("[", "");
        enabledApps = enabledApps.replace("]", "");
        if(enabledApps.isEmpty()) {
            return new HashSet<>();
        }else {
            return new HashSet<>(Arrays.asList(enabledApps.split(",")));
        }
    }

    public void saveEnabledApps(String packageName, boolean isSelected){
        Set<String> selectedPlatforms = getEnabledApps();
        if(!isSelected) {
            //remove the given platform
            selectedPlatforms.remove(packageName);
        }else{
            //add the given platform
            selectedPlatforms.add(packageName);
        }
        SharedPreferences.Editor editor = _sharedPrefs.edit();
        //list tostring is adding empty space so removing them before saving
        editor.putString(KEY_SELECTED_APPS_ARR, selectedPlatforms.toString().replace(" ", ""));
        editor.apply();
    }
}
