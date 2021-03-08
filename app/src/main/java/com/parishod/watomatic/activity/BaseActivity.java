package com.parishod.watomatic.activity;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

import com.parishod.watomatic.model.preferences.PreferencesManager;
import com.parishod.watomatic.model.utils.ContextWrapper;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        PreferencesManager prefs = PreferencesManager.getPreferencesInstance(newBase);
        ContextWrapper contextWrapper = ContextWrapper.wrap(newBase, prefs.getSelectedLocale());
        super.attachBaseContext(contextWrapper);
    }
}
