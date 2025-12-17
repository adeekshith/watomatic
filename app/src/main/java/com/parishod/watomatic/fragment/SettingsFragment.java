package com.parishod.watomatic.fragment;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.parishod.watomatic.BuildConfig;
import com.parishod.watomatic.R;
import com.parishod.watomatic.activity.main.MainActivity;
import com.parishod.watomatic.model.preferences.PreferencesManager;
import com.parishod.watomatic.flavor.FlavorNavigator;
import com.parishod.watomatic.model.utils.AutoStartHelper;
import com.parishod.watomatic.model.utils.ServieUtils;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.fragment_settings, rootKey);

        SwitchPreference showNotificationPref = findPreference(getString(R.string.pref_show_notification_replied_msg));
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M && showNotificationPref != null) {
            showNotificationPref.setTitle(getString(R.string.show_notification_label) + "(Beta)");
        }

        ListPreference languagePref = findPreference(getString(R.string.key_pref_app_language));
        if (languagePref != null) {
            languagePref.setOnPreferenceChangeListener((preference, newValue) -> {
                String thisLangStr = PreferencesManager.getPreferencesInstance(requireActivity()).getSelectedLanguageStr(null);
                if (thisLangStr == null || !thisLangStr.equals(newValue)) {
                    //switch app language here
                    //Should restart the app for language change to take into account
                    restartApp();
                }
                return true;
            });
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                foregroundServiceNotifPref.setVisible(false);
            }
            foregroundServiceNotifPref.setOnPreferenceChangeListener((preference, newValue) -> {
                /*if (newValue.equals(true)) {
                    ServieUtils.getInstance(getActivity()).startNotificationService();
                } else {
                    ServieUtils.getInstance(getActivity()).stopNotificationService();
                }*/
                return true;
            });
        }

        // Account/Login preference
        Preference accountPref = findPreference(getString(R.string.pref_account));
        if(BuildConfig.FLAVOR.equals("Default")){
            accountPref.setVisible(false);
        }
        if (accountPref != null) {
            updateAccountPreference(accountPref);
            accountPref.setOnPreferenceClickListener(pref -> {
                PreferencesManager pm = PreferencesManager.getPreferencesInstance(getContext());
                if (pm.isLoggedIn()) {
                    // Show logout confirmation dialog
                    if (getActivity() != null) {
                        showLogoutDialog(accountPref, pm);
                    }
                } else {
                    // Login
                    if (getActivity() != null) {
                        FlavorNavigator.INSTANCE.startLogin(getActivity());
                    }
                }
                return true;
            });
        }
    }

    private void restartApp() {
        Intent intent = new Intent(requireActivity(), MainActivity.class);
        requireActivity().startActivity(intent);
        requireActivity().finishAffinity();
    }

    private void showLogoutDialog(Preference accountPref, PreferencesManager pm){
        BottomSheetDialog dialog = new BottomSheetDialog(getActivity());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_logout, null);
        dialog.setContentView(view);

        view.findViewById(R.id.btnLogout).setOnClickListener(v -> {
            FlavorNavigator.INSTANCE.logout(getActivity(), pm);
            updateAccountPreference(accountPref);
            dialog.dismiss();
        });

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();

    }

    private void updateAccountPreference(Preference accountPref) {
        PreferencesManager pm = PreferencesManager.getPreferencesInstance(getContext());
        if (pm.isLoggedIn()) {
            String email = pm.getUserEmail();
            if (email == null || email.isEmpty()) {
                accountPref.setSummary("");
            } else {
                accountPref.setSummary(email);
            }
        } else {
            accountPref.setSummary(getString(R.string.guest));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null)
            getActivity().setTitle(R.string.settings);
        // Refresh account summary in case login state changed
        Preference accountPref = findPreference(getString(R.string.pref_account));
        if (accountPref != null) {
            updateAccountPreference(accountPref);
        }
    }

    private void checkAutoStartPermission() {
        if (getActivity() != null) {
            AutoStartHelper.getInstance().getAutoStartPermission(getActivity());
        }
    }

}
