package com.parishod.watomatic.activity.settings;

import android.os.Bundle;


import androidx.lifecycle.ViewModelProvider;

import com.parishod.watomatic.R;
import com.parishod.watomatic.activity.BaseActivity;
import com.parishod.watomatic.viewmodel.SwipeToKillAppDetectViewModel;

public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        SwipeToKillAppDetectViewModel viewModel = new ViewModelProvider(this).get(SwipeToKillAppDetectViewModel.class);
    }
}
