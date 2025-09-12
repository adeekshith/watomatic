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
}
