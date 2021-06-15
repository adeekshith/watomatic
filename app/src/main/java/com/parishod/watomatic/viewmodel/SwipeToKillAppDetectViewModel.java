package com.parishod.watomatic.viewmodel;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.parishod.watomatic.model.preferences.PreferencesManager;
import com.parishod.watomatic.receivers.NotificationServiceRestartReceiver;

import org.jetbrains.annotations.NotNull;

//Ref: https://stackoverflow.com/questions/19568315/how-to-handle-running-service-when-app-is-killed-by-swiping-in-android
public class SwipeToKillAppDetectViewModel extends AndroidViewModel {
    private Context context;
    public SwipeToKillAppDetectViewModel(@NonNull @NotNull Application application) {
        super(application);
        this.context = application;
    }

    @Override
    protected void onCleared() {
        // Do your task here
        Log.d("DEBUG", "OnCleared mainViewModel");
        super.onCleared();
        tryReconnectService();
    }

    public void tryReconnectService() {
        if(PreferencesManager.getPreferencesInstance(context).isServiceEnabled()
            && PreferencesManager.getPreferencesInstance(context).isForegroundServiceNotificationEnabled()) {
            Log.d("DEBUG", "viewmodel tryReconnectService");
            //Send broadcast to restart service
            Intent broadcastIntent = new Intent(context, NotificationServiceRestartReceiver.class);
            broadcastIntent.setAction("Watomatic-RestartService-Broadcast");
            context.sendBroadcast(broadcastIntent);
        }
    }
}
