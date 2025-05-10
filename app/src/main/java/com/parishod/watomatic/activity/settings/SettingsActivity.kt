package com.parishod.watomatic.activity.settings

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.addCallback
import androidx.lifecycle.ViewModelProvider
import com.parishod.watomatic.R
import com.parishod.watomatic.activity.BaseActivity
import com.parishod.watomatic.viewmodel.SwipeToKillAppDetectViewModel

class SettingsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setTitle(R.string.settings)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        ViewModelProvider(this)[SwipeToKillAppDetectViewModel::class.java]

        onBackPressedDispatcher.addCallback(this){

            val fragmentManager = supportFragmentManager
            if (fragmentManager.backStackEntryCount > 1) {
                fragmentManager.popBackStack()
            } else {
               finish()
            }

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
