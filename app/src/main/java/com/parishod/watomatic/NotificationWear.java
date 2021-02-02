package com.parishod.watomatic;

import android.app.Notification;
import android.app.PendingIntent;
import android.os.Bundle;

import androidx.core.app.RemoteInput;

import java.util.ArrayList;
import java.util.UUID;

public class NotificationWear {
    public String id = UUID.randomUUID().toString();
    public String packageName = "";
    public PendingIntent pendingIntent;
    public ArrayList<RemoteInput> remoteInputs = new ArrayList<>();
    public ArrayList<Notification> pages = new ArrayList<>();
    public Bundle bundle;
    public String tag;
}
