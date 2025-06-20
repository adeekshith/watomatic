package com.parishod.watomatic.fragment;

import android.content.Intent;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast; // For feedback

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.parishod.watomatic.R;
import com.parishod.watomatic.activity.main.MainActivity;
import com.parishod.watomatic.model.preferences.PreferencesManager;
import com.parishod.watomatic.model.utils.OpenAIHelper;
import com.parishod.watomatic.network.model.openai.ModelData;

import java.util.ArrayList;
import java.util.List;

public class GeneralSettingsFragment extends PreferenceFragmentCompat {

    private ListPreference openAIModelPreference;
    private PreferencesManager preferencesManager; // Made into a field

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.fragment_general_settings, rootKey);

        preferencesManager = PreferencesManager.getPreferencesInstance(requireActivity()); // Initialize field

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

        // PreferencesManager preferencesManager = PreferencesManager.getPreferencesInstance(requireActivity()); // Now a field

        EditTextPreference openAIApiKeyPreference = findPreference("pref_openai_api_key");
        if (openAIApiKeyPreference != null) {
            updateOpenAIApiKeySummary(openAIApiKeyPreference, preferencesManager.getOpenAIApiKey());
            openAIApiKeyPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                String newApiKey = (String) newValue;
                preferencesManager.saveOpenAIApiKey(newApiKey); // Save to EncryptedSharedPreferences
                updateOpenAIApiKeySummary((EditTextPreference) preference, newApiKey);

                OpenAIHelper.invalidateCache(); // Invalidate model cache if API key changes
                if (openAIModelPreference != null) { // Ensure preference is found
                     openAIModelPreference.setSummary(getString(R.string.pref_openai_model_loading));
                     openAIModelPreference.setEnabled(false);
                }
                loadOpenAIModels(); // Reload models with new key
                return true; // True to update the state/summary of the preference
            });
        }

        SwitchPreferenceCompat enableOpenAIPreference = findPreference("pref_enable_openai_replies");
        if (enableOpenAIPreference != null) {
            enableOpenAIPreference.setChecked(preferencesManager.isOpenAIRepliesEnabled());
            enableOpenAIPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                preferencesManager.setEnableOpenAIReplies((Boolean) newValue);
                // Reload models when the main toggle changes
                if (openAIModelPreference != null) {
                     openAIModelPreference.setSummary(getString(R.string.pref_openai_model_loading));
                     openAIModelPreference.setEnabled(false);
                }
                loadOpenAIModels();
                return true; // True to update the state of the preference
            });
        }

        openAIModelPreference = findPreference("pref_openai_model");
        if (openAIModelPreference != null) {
            openAIModelPreference.setSummary(getString(R.string.pref_openai_model_loading));
            openAIModelPreference.setEnabled(false);
            openAIModelPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                String modelId = (String) newValue;
                // preferencesManager.saveSelectedOpenAIModel(modelId); // Need to add this method to PrefManager
                // For now, let's assume the ListPreference saves it to default SharedPreferences
                // and we'll handle saving to a specific key in PrefManager if needed later.
                // However, it's better to be explicit:
                // This line will be uncommented once saveSelectedOpenAIModel is added to PreferencesManager
                // preferencesManager.saveSelectedOpenAIModel(modelId);
                // The value is saved by default by ListPreference to its key "pref_openai_model"
                // in the default SharedPreferences. We need to read from there or save explicitly.
                // For now, let it save to default, and NotificationService will read from default.
                // Or, ensure PreferencesManager has a method for this specific key.
                // Let's add a placeholder for now to save it via PreferencesManager eventually.
                // This should be:
                preferencesManager.saveSelectedOpenAIModel(modelId); // Using dedicated method
                return true;
            });
            loadOpenAIModels();
        }
    }

    private void loadOpenAIModels() {
        if (openAIModelPreference == null || preferencesManager == null) return;

        if (!preferencesManager.isOpenAIRepliesEnabled() ||
            TextUtils.isEmpty(preferencesManager.getOpenAIApiKey())) {
            openAIModelPreference.setSummary(getString(R.string.pref_openai_model_summary_default));
            openAIModelPreference.setEnabled(false);
            openAIModelPreference.setEntries(new CharSequence[]{});
            openAIModelPreference.setEntryValues(new CharSequence[]{});
            return;
        }

        openAIModelPreference.setSummary(getString(R.string.pref_openai_model_loading));
        openAIModelPreference.setEnabled(false);

        OpenAIHelper.fetchModels(requireActivity(), new OpenAIHelper.FetchModelsCallback() {
            @Override
            public void onModelsFetched(List<ModelData> models) {
                if (getActivity() == null || preferencesManager == null) return;

                List<CharSequence> entries = new ArrayList<>();
                List<CharSequence> entryValues = new ArrayList<>();
                boolean foundSelected = false;
                // String selectedModelId = preferencesManager.getSelectedOpenAIModel(); // Needs to be added to PrefManager
                // Reading from default shared prefs for now for "pref_openai_model"
                String selectedModelId = preferencesManager.getSelectedOpenAIModel(); // Using dedicated method


                if (models != null && !models.isEmpty()) {
                    for (ModelData model : models) {
                        if (model.getId().contains("gpt")) {
                            entries.add(model.getId());
                            entryValues.add(model.getId());
                            if (model.getId().equals(selectedModelId)) {
                                foundSelected = true;
                            }
                        }
                    }

                    if (entries.isEmpty()) { // Fallback if no "gpt" models
                        for (ModelData model : models) {
                             entries.add(model.getId());
                             entryValues.add(model.getId());
                             if (model.getId().equals(selectedModelId)) {
                                foundSelected = true;
                            }
                        }
                    }

                    if (!entries.isEmpty()) {
                        openAIModelPreference.setEntries(entries.toArray(new CharSequence[0]));
                        openAIModelPreference.setEntryValues(entryValues.toArray(new CharSequence[0]));

                        String valueToSet = null;
                        if (foundSelected && selectedModelId != null) {
                            valueToSet = selectedModelId;
                        } else if (!entryValues.isEmpty()) {
                            valueToSet = entryValues.get(0).toString();
                        }

                        if (valueToSet != null) {
                            openAIModelPreference.setValue(valueToSet);
                            // preferencesManager.saveSelectedOpenAIModel(valueToSet); // Explicit save if needed
                            preferencesManager.saveSelectedOpenAIModel(valueToSet); // Using dedicated method
                            // Summary should be auto-updated by useSimpleSummaryProvider
                            // If not, manually set: openAIModelPreference.setSummary(openAIModelPreference.getEntry());
                        } else {
                             openAIModelPreference.setSummary(getString(R.string.pref_openai_model_not_set));
                        }
                         openAIModelPreference.setEnabled(true);
                    } else {
                        openAIModelPreference.setSummary(getString(R.string.pref_openai_model_no_compatible_found));
                        openAIModelPreference.setEntries(new CharSequence[0]);
                        openAIModelPreference.setEntryValues(new CharSequence[0]);
                        openAIModelPreference.setEnabled(false);
                    }
                } else { // models list is null or empty from callback
                    openAIModelPreference.setSummary(getString(R.string.pref_openai_model_error));
                    openAIModelPreference.setEntries(new CharSequence[0]);
                    openAIModelPreference.setEntryValues(new CharSequence[0]);
                    openAIModelPreference.setEnabled(false);
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (getActivity() == null) return;
                openAIModelPreference.setSummary(errorMessage);
                openAIModelPreference.setEnabled(false);
                openAIModelPreference.setEntries(new CharSequence[0]);
                openAIModelPreference.setEntryValues(new CharSequence[0]);
                // Toast.makeText(requireActivity(), "Error loading models: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
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
        if (getActivity() != null) { // Use getActivity() for direct activity access if needed for title
            getActivity().setTitle(R.string.preference_category_general_label);
        }

        // Refresh models list and state
        if (openAIModelPreference != null) { // Check if initialized
            loadOpenAIModels();
        }
    }

    private void restartApp() {
        Intent intent = new Intent(requireActivity(), MainActivity.class);
        requireActivity().startActivity(intent);
        requireActivity().finishAffinity();
    }
}
