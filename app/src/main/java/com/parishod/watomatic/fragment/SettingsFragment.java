package com.parishod.watomatic.fragment;

import static android.content.Context.ACTIVITY_SERVICE;

import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.parishod.watomatic.R;
import com.parishod.watomatic.model.utils.AutoStartHelper;
import com.parishod.watomatic.model.utils.Constants;
import com.parishod.watomatic.model.utils.CustomDialog;
import com.parishod.watomatic.model.utils.ServieUtils;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.fragment_settings, rootKey);

        SwitchPreference showNotificationPref = findPreference(getString(R.string.pref_show_notification_replied_msg));
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M && showNotificationPref != null) {
            showNotificationPref.setTitle(getString(R.string.show_notification_label) + "(Beta)");
        }

        Preference autoStartPref = findPreference(getString(R.string.pref_auto_start_permission));
        if (autoStartPref != null) {
            autoStartPref.setOnPreferenceClickListener(preference -> {
                checkAutoStartPermission();
                return true;
            });
        }

        SwitchPreference foregroundServiceNotifPref = findPreference(getString(R.string.pref_show_foreground_service_notification));
        if (foregroundServiceNotifPref != null) {
            foregroundServiceNotifPref.setOnPreferenceChangeListener((preference, newValue) -> {
                if (newValue.equals(true)) {
                    ServieUtils.getInstance(getActivity()).startNotificationService();
                } else {
                    ServieUtils.getInstance(getActivity()).stopNotificationService();
                }
                return true;
            });
        }

        Preference restoreAppDefaultPref = findPreference(getString(R.string.pref_restore_app_defaults));
        if (restoreAppDefaultPref != null) {
            restoreAppDefaultPref.setOnPreferenceClickListener(preference -> {
                showAlert(getActivity(), (dialog, which) -> {
                    try {
                        clearAppData();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    dialog.dismiss();
                });
                return true;
            });
        }
    }

    private void showAlert(Context context, DialogInterface.OnClickListener onClickListener) {
        CustomDialog customDialog = new CustomDialog(context);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.PERMISSION_DIALOG_TITLE, context.getString(R.string.clear_data_alert_title));
        bundle.putString(Constants.PERMISSION_DIALOG_MSG,
                context.getString(R.string.clear_data_alert_messge));
        customDialog.showDialog(bundle, "ClearData", (dialog, which) -> {
            if (which != -2) {
                //Decline
                onClickListener.onClick(dialog, which);
            }
        });
    }

    //REF: https://stackoverflow.com/questions/6134103/clear-applications-data-programmatically
    private void clearAppData() {
        try {
            // clearing app data
            ((ActivityManager)getActivity().getSystemService(ACTIVITY_SERVICE)).clearApplicationUserData(); // note: it has a return value!
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null)
            getActivity().setTitle(R.string.settings);
    }

    private void checkAutoStartPermission() {
        if (getActivity() != null) {
            AutoStartHelper.getInstance().getAutoStartPermission(getActivity());
        }
    }

}
