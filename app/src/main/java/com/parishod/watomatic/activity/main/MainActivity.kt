package com.parishod.watomatic.activity.main

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.parishod.watomatic.R
import com.parishod.watomatic.activity.BaseActivity
import com.parishod.watomatic.activity.login.LoginActivity
import com.parishod.watomatic.model.preferences.PreferencesManager
import com.parishod.watomatic.viewmodel.SwipeToKillAppDetectViewModel

import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : BaseActivity() {
    private lateinit var viewModel: SwipeToKillAppDetectViewModel
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferencesManager = PreferencesManager.getPreferencesInstance(this)

        // Check if we need to show login screen
        if (preferencesManager.shouldShowLogin()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)
        setTitle(R.string.app_name)

        viewModel = ViewModelProvider(this)[SwipeToKillAppDetectViewModel::class.java]

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_frame_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}