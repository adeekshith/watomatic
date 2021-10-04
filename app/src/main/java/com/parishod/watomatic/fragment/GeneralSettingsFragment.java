package com.parishod.watomatic.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import com.parishod.watomatic.R;
import com.parishod.watomatic.activity.main.MainActivity;
import com.parishod.watomatic.model.preferences.PreferencesManager;

public class GeneralSettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.fragment_general_settings, rootKey);

        ListPreference languagePref = findPreference(getString(R.string.key_pref_app_language));
        if (languagePref != null) {
            languagePref.setOnPreferenceChangeListener((preference, newValue) -> {
                String thisLangStr = PreferencesManager.getPreferencesInstance(getActivity()).getSelectedLanguageStr(null);
                if (thisLangStr == null || !thisLangStr.equals(newValue)) {
                    //switch app language here
                    //Should restart the app for language change to take into account
                    restartApp();
                }
                return true;
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null)
            getActivity().setTitle(R.string.preference_category_general_label);
    }

    private void restartApp() {
        Intent intent = new Intent(getActivity(), MainActivity.class);
        if (getActivity() != null) {
            getActivity().startActivity(intent);
            getActivity().finishAffinity();
        }
    }
}
