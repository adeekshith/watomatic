package com.parishod.watomatic.model.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

public class PreferencesManager {
    private final String KEY_SERVICE_ENABLED = "pref_service_enabled";
    private final String KEY_GROUP_REPLY_ENABLED = "pref_group_reply_enabled";
    private final String KEY_AUTO_REPLY_THROTTLE_TIME_MS = "pref_auto_reply_throttle_time_ms";
    private final String KEY_WHATSAPP_ENABLED = "pref_whatsapp_enabled";
    private final String KEY_FACEBOOK_MESSENGER_ENABLED = "pref_facebook_enabled";
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

    public boolean isWhatsAppEnabled(){
        return _sharedPrefs.getBoolean(KEY_WHATSAPP_ENABLED,false);
    }

    public void setWhatsAppPref(boolean enabled){
        SharedPreferences.Editor editor = _sharedPrefs.edit();
        editor.putBoolean(KEY_WHATSAPP_ENABLED, enabled);
        editor.apply();
    }

    public boolean isFacebookMessengerEnabled(){
        return _sharedPrefs.getBoolean(KEY_FACEBOOK_MESSENGER_ENABLED,false);
    }

    public void setFacebookMessengerPref(boolean enabled){
        SharedPreferences.Editor editor = _sharedPrefs.edit();
        editor.putBoolean(KEY_FACEBOOK_MESSENGER_ENABLED, enabled);
        editor.apply();
    }
}
