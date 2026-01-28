package com.parishod.watomatic.model.subscription

import androidx.lifecycle.LiveData

/**
 * Manages subscription state and Pro feature access.
 * Implements the three-tier verification strategy (Cache -> Firestore -> Backend).
 */
interface SubscriptionManager {
    /**
     * Check if the user has an active Pro subscription.
     * This is a fast, synchronous check against local cache (Preferences).
     */
    fun isProUser(): Boolean

    /**
     * Check if a specific feature requires Pro and if the user has access.
     */
    fun hasFeatureAccess(feature: ProFeature): Boolean

    /**
     * Observable subscription status for UI updates.
     */
    val subscriptionStatus: LiveData<SubscriptionState>

    /**
     * Force a refresh of the subscription status from backend.
     */
    suspend fun refreshSubscriptionStatus()

    /**
     * Verify and restore a specific purchase found from Google Play.
     * This links the purchase to the current user account on the backend.
     */
    suspend fun restorePurchase(purchaseToken: String, productId: String, orderId: String): Boolean

    /**
     * Enum defining Pro features
     */
    enum class ProFeature {
        AI_REPLIES,
        CUSTOM_REPLY_DELAY,
        UNLIMITED_CONTACTS,
        PRIORITY_SUPPORT
    }
}

/**
 * Data class representing the current subscription state for UI
 */
data class SubscriptionState(
    val isActive: Boolean,
    val planType: String? = null, // "monthly" or "annual"
    val expiryDate: Long = 0,
    val isAutoRenewing: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)
