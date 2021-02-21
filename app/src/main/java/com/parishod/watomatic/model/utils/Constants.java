package com.parishod.watomatic.model.utils;

import com.parishod.watomatic.model.App;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Constants {
    public static final String PERMISSION_DIALOG_TITLE = "permission_dialog_title";
    public static final String PERMISSION_DIALOG_MSG = "permission_dialog_msg";
    public static final String PERMISSION_DIALOG_DENIED_TITLE = "permission_dialog_denied_title";
    public static final String PERMISSION_DIALOG_DENIED_MSG = "permission_dialog_denied_msg";
    public static final String PERMISSION_DIALOG_DENIED = "permission_dialog_denied";
    public static final String WHATSAPP_LOGS_DB_NAME = "logs_whatsapp_autoreply_db";

    /**
     * Set of apps this app can auto reply
     */
    public static final Set<App> SUPPORTED_APPS = new HashSet<>(Arrays.asList(
            new App("WhatsApp", "com.whatsapp"),
            new App("Facebook Messenger", "com.facebook.orca")
            //new App("Facebook Messenger Lite", "com.facebook.mlite")
    ));

    public static final int MIN_DAYS = 0;
    public static final int MAX_DAYS = 7;
}
