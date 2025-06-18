package com.parishod.watomatic.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

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
                String thisLangStr = PreferencesManager.getPreferencesInstance(requireActivity()).getSelectedLanguageStr(null);
                if (thisLangStr == null || !thisLangStr.equals(newValue)) {
                    //switch app language here
                    //Should restart the app for language change to take into account
                    restartApp();
                }
                return true;
            });
        }

        PreferencesManager preferencesManager = PreferencesManager.getPreferencesInstance(requireActivity());

        EditTextPreference openAIApiKeyPreference = findPreference("pref_openai_api_key");
        if (openAIApiKeyPreference != null) {
            updateOpenAIApiKeySummary(openAIApiKeyPreference, preferencesManager.getOpenAIApiKey());
            openAIApiKeyPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                String newApiKey = (String) newValue;
                preferencesManager.saveOpenAIApiKey(newApiKey); // Save to EncryptedSharedPreferences
                updateOpenAIApiKeySummary((EditTextPreference) preference, newApiKey);
                return true; // True to update the state/summary of the preference
            });
        }

        SwitchPreferenceCompat enableOpenAIPreference = findPreference("pref_enable_openai_replies");
        if (enableOpenAIPreference != null) {
            enableOpenAIPreference.setChecked(preferencesManager.isOpenAIRepliesEnabled());
            enableOpenAIPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                preferencesManager.setEnableOpenAIReplies((Boolean) newValue);
                return true; // True to update the state of the preference
            });
        }
    }

    private void updateOpenAIApiKeySummary(EditTextPreference preference, String keyValue) {
        if (preference == null) return;
        if (keyValue != null && !keyValue.isEmpty()) {
            // To avoid showing the full key, you could mask it, e.g.:
            // String maskedKey = keyValue.length() > 8 ? keyValue.substring(0, 4) + "..." + keyValue.substring(keyValue.length() - 4) : "Set";
            // preference.setSummary(getString(R.string.pref_openai_api_key_summary_set) + " (" + maskedKey + ")");
            // For now, using the simpler string:
            preference.setSummary(getString(R.string.pref_openai_api_key_summary_set));
        } else {
            preference.setSummary(getString(R.string.pref_openai_api_key_summary_not_set));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null)
            getActivity().setTitle(R.string.preference_category_general_label);
    }

    private void restartApp() {
        Intent intent = new Intent(requireActivity(), MainActivity.class);
        requireActivity().startActivity(intent);
        requireActivity().finishAffinity();
    }
}
