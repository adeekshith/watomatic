package com.parishod.watomatic.activity.settings

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.addCallback
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.parishod.watomatic.R
import com.parishod.watomatic.activity.BaseActivity
import com.parishod.watomatic.fragment.GeneralSettingsFragment
import com.parishod.watomatic.fragment.SettingsFragment
import com.parishod.watomatic.viewmodel.SwipeToKillAppDetectViewModel

class SettingsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        ViewModelProvider(this)[SwipeToKillAppDetectViewModel::class.java]

        onBackPressedDispatcher.addCallback(this){

            val fragmentManager = supportFragmentManager
            if (fragmentManager.backStackEntryCount > 1) {
                fragmentManager.popBackStack()
            } else {
               finish()
            }

        }

        supportFragmentManager.addOnBackStackChangedListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.setting_fragment_container)
            when (currentFragment) {
                is SettingsFragment -> {
                    supportActionBar?.title = getString(R.string.settings)
                }
                is GeneralSettingsFragment -> {
                    supportActionBar?.title = getString(R.string.preference_category_general_label)
                }
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}
