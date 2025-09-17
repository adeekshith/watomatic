package com.parishod.watomatic.flavor

import android.app.Activity
import com.parishod.watomatic.model.preferences.PreferencesManager

object FlavorNavigator {
    /**
     * Default flavor: no login flow. Returns false to indicate no navigation was performed.
     */
    fun navigateToLoginIfNeeded(activity: Activity, preferencesManager: PreferencesManager): Boolean {
        // Ensure login is never triggered in Default flavor
        return false
    }

    fun startLogin(activity: Activity) {
        // No-op in Default flavor
    }

    fun logout(activity: Activity, preferencesManager: PreferencesManager) {
        // Clear any login flags if set inadvertently
        preferencesManager.setLoggedIn(false)
        preferencesManager.setGuestMode(false)
        preferencesManager.setUserEmail("")
    }
}
