package com.parishod.watomatic.fragment;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.parishod.watomatic.R;
import com.parishod.watomatic.activity.contactselector.ContactSelectorActivity;
import com.parishod.watomatic.model.utils.ContactsHelper;

public class AdvancedSettingsFragment extends PreferenceFragmentCompat {

    private ContactsHelper contactsHelper;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.fragment_advanced_settings, rootKey);

        contactsHelper = ContactsHelper.Companion.getInstance(getContext());

        SwitchPreference enable_contact_replies_preference = findPreference(getString(R.string.pref_reply_contacts));
        if (enable_contact_replies_preference != null) {
            enable_contact_replies_preference.setOnPreferenceChangeListener((preference, newValue) -> {
                if ((Boolean) newValue && !contactsHelper.hasContactPermission() &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    contactsHelper.requestContactPermission(getActivity());
                }
                return true;
            });
        }

        Preference advancedPref = findPreference(getString(R.string.key_pref_select_contacts));
        if (advancedPref != null) {
            advancedPref.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(getActivity(), ContactSelectorActivity.class));
                return true;
            });
        }
    }
}
