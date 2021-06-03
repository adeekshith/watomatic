package com.parishod.watomatic.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.parishod.watomatic.R;
import com.parishod.watomatic.activity.main.MainActivity;
import com.parishod.watomatic.model.preferences.PreferencesManager;
import com.parishod.watomatic.model.utils.AutoStartHelper;

public class SettingsFragment extends PreferenceFragmentCompat {
    private ListPreference languagePref;
    private SwitchPreference showNotificationPref;
    private Preference autoStartPref;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.fragment_settings, rootKey);

        languagePref = findPreference(getString(R.string.key_pref_app_language));
        languagePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String thisLangStr = PreferencesManager.getPreferencesInstance(getActivity()).getSelectedLanguageStr(null);
                if(thisLangStr == null || !thisLangStr.equals(newValue)){
                    //switch app language here
                    //Should restart the app for language change to take into account
                    restartApp();
                }
                return true;
            }
        });

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
    }

    private void checkAutoStartPermission() {
        if(getActivity() != null) {
            AutoStartHelper.getInstance().getAutoStartPermission(getActivity());
        }
    }

    private void restartApp() {
        Intent intent = new Intent(getActivity(), MainActivity.class);
        getActivity().startActivity(intent);
        getActivity().finishAffinity();
    }
}
