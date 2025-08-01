package com.parishod.watomatic.activity.advancedsettings;

import android.os.Bundle;

import com.parishod.watomatic.R;
import com.parishod.watomatic.activity.BaseActivity;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class AdvancedSettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_advanced_settings);

        setTitle(R.string.advanced_settings);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.setting_fragment_container), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
