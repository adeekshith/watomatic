package com.parishod.watomatic.activity;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

import com.parishod.watomatic.model.preferences.PreferencesManager;
import com.parishod.watomatic.model.utils.ContextWrapper;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        String lang = PreferencesManager.getPreferencesInstance(newBase).getSelectedLanguage();
        ContextWrapper contextWrapper = ContextWrapper.wrap(newBase, lang);
        super.attachBaseContext(contextWrapper);
    }
}
