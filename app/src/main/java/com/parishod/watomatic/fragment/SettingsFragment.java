package com.parishod.watomatic.fragment;

import android.os.Bundle;
import android.util.Log;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.parishod.watomatic.R;
import com.parishod.watomatic.activity.main.MainActivity;
import com.parishod.watomatic.model.preferences.PreferencesManager;

public class SettingsFragment extends PreferenceFragmentCompat {
    private ListPreference languagePref;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.fragment_settings, rootKey);

        //Set fragment title
        getActivity().setTitle("Settings");

        ((MainActivity)getActivity()).showHideBackButton(true);

        languagePref = findPreference("pref_key_selected_app_language");
        languagePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Log.d("DEBUG", "newValue " + newValue);
                if(!PreferencesManager.getPreferencesInstance(getActivity()).getSelectedLanguage().equals(newValue)){
                    //TODO switch app language here
                    Log.d("DEBUG", "Switch App Language");
                }
                return true;
            }
        });
    }
}
