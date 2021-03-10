package com.parishod.watomatic.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.parishod.watomatic.R;
import com.parishod.watomatic.activity.main.MainActivity;
import com.parishod.watomatic.model.preferences.PreferencesManager;

import java.io.File;

public class SettingsFragment extends PreferenceFragmentCompat {
    private ListPreference languagePref;
    private Preference shareLogsPref;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.fragment_settings, rootKey);

        languagePref = findPreference(getString(R.string.key_pref_app_language));
        languagePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if(!PreferencesManager.getPreferencesInstance(getActivity()).getSelectedLanguage().equals(newValue)){
                    //switch app language here
                    //Should restart the app for language change to take into account
                    restartApp();
                }
                return true;
            }
        });

        shareLogsPref = findPreference(getString(R.string.pref_share_logs));
        shareLogsPref.setOnPreferenceClickListener(preference -> {
            shareAppLogs();
            return true;
        });
    }

    private void shareAppLogs() {
        Uri uri= FileProvider.getUriForFile(getActivity(),
                getActivity().getPackageName() + ".provider",
                new File(getActivity().getFilesDir(), getString(R.string.app_logs_file_name)));

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.putExtra(Intent.EXTRA_EMAIL  , new String[]{getString(R.string.share_email_id)});
        shareIntent.putExtra(Intent.EXTRA_SUBJECT,getString(R.string.share_email_subject));
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_email_body));
        if(shareIntent.resolveActivity(getActivity().getPackageManager()) != null){
            startActivity(Intent.createChooser(shareIntent, "Send Email"));
        }else{
            Toast.makeText(getActivity(), getString(R.string.share_email_err_msg), Toast.LENGTH_SHORT).show();
        }
    }

    private void restartApp() {
        Intent intent = new Intent(getActivity(), MainActivity.class);
        getActivity().startActivity(intent);
        getActivity().finishAffinity();
    }
}
