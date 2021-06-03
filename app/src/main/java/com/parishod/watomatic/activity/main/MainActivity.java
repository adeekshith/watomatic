package com.parishod.watomatic.activity.main;

import android.os.Bundle;

import androidx.lifecycle.ViewModelProvider;

import com.parishod.watomatic.R;
import com.parishod.watomatic.activity.BaseActivity;
import com.parishod.watomatic.viewmodel.SwipeToKillAppDetectViewModel;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SwipeToKillAppDetectViewModel viewModel = new ViewModelProvider(this).get(SwipeToKillAppDetectViewModel.class);
    }
}