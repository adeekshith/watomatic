package com.parishod.watomatic.model.subscription

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.parishod.watomatic.model.preferences.PreferencesManager

class SubscriptionManagerImpl(
    private val context: Context,
    private val preferencesManager: PreferencesManager
) : SubscriptionManager {

    private val _subscriptionStatus = MutableLiveData<SubscriptionState>()
    override val subscriptionStatus: LiveData<SubscriptionState> = _subscriptionStatus

    init {
        _subscriptionStatus.postValue(SubscriptionState(isActive = false))
    }

    override fun isProUser(): Boolean = false

    override fun hasFeatureAccess(feature: SubscriptionManager.ProFeature): Boolean = false

    override suspend fun refreshSubscriptionStatus() {
        // No-op for default flavor
        _subscriptionStatus.postValue(SubscriptionState(isActive = false))
    }
    
    override suspend fun restorePurchase(purchaseToken: String, productId: String, orderId: String): Boolean {
        return false
    }
}
