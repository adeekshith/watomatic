package com.parishod.watomatic.activity.advancedsettings;

import android.os.Bundle;

import com.parishod.watomatic.R;
import com.parishod.watomatic.activity.BaseActivity;

public class AdvancedSettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_settings);

        setTitle(R.string.advanced_settings);
    }
}
