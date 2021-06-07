package com.parishod.watomatic.fragment;

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.parishod.watomatic.R;
import com.parishod.watomatic.model.utils.ContactsHelper;

public class AdvancedSettingsFragment extends PreferenceFragmentCompat {

    private Preference advancedPref;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.fragment_advanced_settings, rootKey);

        advancedPref = findPreference(getString(R.string.key_pref_select_contacts));

        advancedPref.setOnPreferenceClickListener(preference -> {
            ContactsHelper.getInstance(getContext()).showContactPicker();
            return true;
        });
    }
}
