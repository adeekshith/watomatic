package com.parishod.watomatic.activity.enabledapps

import android.os.Bundle
import com.parishod.watomatic.R
import com.parishod.watomatic.activity.BaseActivity

import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class EnabledAppsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_enabled_apps)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.enabled_apps_root_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}