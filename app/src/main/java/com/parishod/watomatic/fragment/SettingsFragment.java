package com.parishod.watomatic.fragment;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.parishod.watomatic.BuildConfig;
import com.parishod.watomatic.R;
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
