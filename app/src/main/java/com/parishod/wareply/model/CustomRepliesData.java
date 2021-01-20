package com.parishod.wareply.model;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * Manages user entered custom auto reply text data.
 */
public class CustomRepliesData {
    public static final String KEY_CUSTOM_REPLY_ALL = "user_custom_reply_all";
    public static final int MAX_NUM_CUSTOM_REPLY = 10;
    private static final String APP_SHARED_PREFS = CustomRepliesData.class.getSimpleName();
    private final SharedPreferences _sharedPrefs;

    public CustomRepliesData(Context context) {
        this._sharedPrefs = context.getSharedPreferences(APP_SHARED_PREFS, Activity.MODE_PRIVATE);
    }

    /**
     * Stores given auto reply text to the database and sets it as current
     * @param customReply String that needs to be set as current auto reply
     * @return String that is set
     */
    public String set(String customReply) {
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
     * Get the current auto reply text
     * @return Auto reply text
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

    private JSONArray getAll() {
        JSONArray allCustomReplies = new JSONArray();
        try {
            allCustomReplies = new JSONArray(_sharedPrefs.getString(KEY_CUSTOM_REPLY_ALL, "[]"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return allCustomReplies;
    }
}
