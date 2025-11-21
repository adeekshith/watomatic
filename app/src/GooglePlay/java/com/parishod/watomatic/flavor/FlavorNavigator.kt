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

    fun startLogin(activity: Activity) {
        activity.startActivity(Intent(activity, LoginActivity::class.java))
        activity.finish()
    }

    fun logout(activity: Activity, preferencesManager: PreferencesManager) {
        try {
            // Sign out of Firebase if available in this flavor
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        } catch (e: Throwable) {
            // ignore
        }
        preferencesManager.setLoggedIn(false)
        preferencesManager.setGuestMode(false)
        preferencesManager.setUserEmail("")
    }
}
