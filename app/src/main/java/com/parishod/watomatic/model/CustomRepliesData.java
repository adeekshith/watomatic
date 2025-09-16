package com.parishod.watomatic.model;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;

import com.parishod.watomatic.R;
import com.parishod.watomatic.model.preferences.PreferencesManager;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * Manages user entered custom auto reply text data.
 */
public class CustomRepliesData {
    public static final String KEY_CUSTOM_REPLY_ALL = "user_custom_reply_all";
    public static final int MAX_NUM_CUSTOM_REPLY = 10;
    public static final int MAX_STR_LENGTH_CUSTOM_REPLY = 500;
    public static final String RTL_ALIGN_INVISIBLE_CHAR = " \u200F\u200F\u200E "; // https://android.stackexchange.com/a/190024
    private static final String APP_SHARED_PREFS = CustomRepliesData.class.getSimpleName();
    private static SharedPreferences _sharedPrefs;
    private static CustomRepliesData _INSTANCE;
    private final Context thisAppContext;
    private final PreferencesManager preferencesManager;

    private CustomRepliesData(Context context) {
        thisAppContext = context.getApplicationContext();
        _sharedPrefs = context.getApplicationContext()
                .getSharedPreferences(APP_SHARED_PREFS, Activity.MODE_PRIVATE);
        preferencesManager = PreferencesManager.getPreferencesInstance(thisAppContext);
        init();
    }

    public static CustomRepliesData getInstance(Context context) {
        if (_INSTANCE == null) {
            _INSTANCE = new CustomRepliesData(context);
        }
        return _INSTANCE;
    }

    /**
     * Execute this code when the singleton is first created. All the tasks that needs to be done
     * when the instance is first created goes here. For example, set specific keys based on new install
     * or app upgrade, etc.
     */
    private void init() {
        // Set default auto reply message on first install
        if (!_sharedPrefs.contains(KEY_CUSTOM_REPLY_ALL)) {
            set(thisAppContext.getString(R.string.auto_reply_default_message));
        }
    }

    /**
     * Stores given auto reply text to the database and sets it as current
     *
     * @param customReply String that needs to be set as current auto reply
     * @return String that is stored in the database as current custom reply
     */
    public String set(String customReply) {
        if (!isValidCustomReply(customReply)) {
            return null;
        }
        JSONArray previousCustomReplies = getAll();
        previousCustomReplies.put(customReply);
        if (previousCustomReplies.length() > MAX_NUM_CUSTOM_REPLY) {
            previousCustomReplies.remove(0);
        }
        SharedPreferences.Editor editor = _sharedPrefs.edit();
        editor.putString(KEY_CUSTOM_REPLY_ALL, previousCustomReplies.toString());
        editor.apply();
        return customReply;
    }

    /**
     * Stores given auto reply text to the database and sets it as current
     *
     * @param customReply Editable that needs to be set as current auto reply
     * @return String that is stored in the database as current custom reply
     */
    public String set(Editable customReply) {
        return (customReply != null)
                ? set(customReply.toString())
                : null;
    }

    /**
     * Get last set auto reply text
     * Prefer using {@link CustomRepliesData::getOrElse} to avoid {@code null}
     *
     * @return Auto reply text or {@code null} if not set
     */
    public String get() {
        JSONArray allCustomReplies = getAll();
        try {
            return (allCustomReplies.length() > 0)
                    ? (String) allCustomReplies.get(allCustomReplies.length() - 1)
                    : null;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get last set auto reply text if present or else return {@param defaultText}
     *
     * @param defaultText default auto reply text
     * @return Return auto reply text if present or else return given {@param defaultText}
     */
    public String getOrElse(String defaultText) {
        String currentText = get();
        return (currentText != null)
                ? currentText
                : defaultText;
    }

    public String getTextToSendOrElse() {
        String currentText;
        if(preferencesManager.isOpenAIRepliesEnabled()){
            currentText = thisAppContext.getString(R.string.ai_auto_reply_default_message);
        }else {
            currentText = getOrElse(thisAppContext.getString(R.string.auto_reply_default_message));
        }
        if (preferencesManager.isAppendWatomaticAttributionEnabled()) {
            currentText += "\n\n" + RTL_ALIGN_INVISIBLE_CHAR + thisAppContext.getString(R.string.sent_using_Watomatic);
        }
        return currentText;
    }

    private JSONArray getAll() {
        JSONArray allCustomReplies = new JSONArray();
        try {
            allCustomReplies = new JSONArray(_sharedPrefs.getString(KEY_CUSTOM_REPLY_ALL, "[]"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return allCustomReplies;
    }

    public static boolean isValidCustomReply(String userInput) {
        return (userInput != null) &&
                !userInput.isEmpty() &&
                (userInput.length() <= MAX_STR_LENGTH_CUSTOM_REPLY);
    }

    public static boolean isValidCustomReply(Editable userInput) {
        return (userInput != null) &&
                isValidCustomReply(userInput.toString());
    }
}
