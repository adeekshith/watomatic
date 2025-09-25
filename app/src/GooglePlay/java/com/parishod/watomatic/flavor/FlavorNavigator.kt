package com.parishod.watomatic.flavor

import android.app.Activity
import android.content.Intent
import com.parishod.watomatic.activity.login.LoginActivity
import com.parishod.watomatic.model.preferences.PreferencesManager

object FlavorNavigator {
    /**
     * Returns true if it handled navigation (started Login and finished current Activity).
     */
    fun navigateToLoginIfNeeded(activity: Activity, preferencesManager: PreferencesManager): Boolean {
        return if (preferencesManager.shouldShowLogin()) {
            activity.startActivity(Intent(activity, LoginActivity::class.java))
            activity.finish()
            true
        } else {
            false
        }
    }
}
