package com.parishod.watomatic.activity.subscription

/**
 * Defines the mode in which SubscriptionInfoActivity operates.
 * The mode is passed via intent extra and determines which UI is shown.
 */
enum class SubscriptionMode {
    /**
     * Show active subscription details only.
     * Hides plan list / selection UI.
     * Used when user taps "Manage Plan".
     */
    MANAGE,

    /**
     * Show plans list UI for upgrading.
     * Marks the user's current plan as "Current Plan" and disables it
     * (along with all lower tiers). Only higher plans remain selectable.
     * Used when user taps "Upgrade Plan".
     */
    UPGRADE;

    companion object {
        const val EXTRA_KEY = "subscription_mode"

        fun fromIntent(intent: android.content.Intent?): SubscriptionMode? {
            val modeName = intent?.getStringExtra(EXTRA_KEY) ?: return null
            return try {
                valueOf(modeName)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}
