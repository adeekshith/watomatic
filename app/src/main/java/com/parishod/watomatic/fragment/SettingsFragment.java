package com.parishod.watomatic.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.parishod.watomatic.R;
import com.parishod.watomatic.activity.advancedsettings.AdvancedSettingsActivity;
import com.parishod.watomatic.model.utils.AutoStartHelper;
import com.parishod.watomatic.model.utils.ServieUtils;

public class SettingsFragment extends PreferenceFragmentCompat {
    private SwitchPreference showNotificationPref, foregroundServiceNotifPref;
    private Preference advancedPref;
    private Preference autoStartPref;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.fragment_settings, rootKey);


        showNotificationPref = findPreference(getString(R.string.pref_show_notification_replied_msg));
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            showNotificationPref.setTitle(getString(R.string.show_notification_label) + "(Beta)");
        }

        autoStartPref = findPreference(getString(R.string.pref_auto_start_permission));
        autoStartPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                checkAutoStartPermission();
                return true;
            }
        });

        advancedPref = findPreference(getString(R.string.key_pref_advanced_settings));
        advancedPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent advancedSettings = new Intent(getActivity(), AdvancedSettingsActivity.class);
                getActivity().startActivity(advancedSettings);
                return false;
            }
        });

        foregroundServiceNotifPref = findPreference(getString(R.string.pref_show_foreground_service_notification));
        foregroundServiceNotifPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if(newValue.equals(true)){
                    ServieUtils.getInstance(getActivity()).startNotificationService();
                }else{
                    ServieUtils.getInstance(getActivity()).stopNotificationService();
                }
                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.settings);
    }

    private void checkAutoStartPermission() {
        if(getActivity() != null) {
            AutoStartHelper.getInstance().getAutoStartPermission(getActivity());
        }
    }

}
