package com.parishod.watomatic.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase

/**
 * Stub implementation of BillingManager for Default flavor (non-Google Play)
 * This flavor doesn't support in-app purchases
 */
class BillingManagerImpl(private val context: Context) : BillingManager {

    override fun startConnection(onConnected: (() -> Unit)?, onDisconnected: (() -> Unit)?) {
        // No-op for default flavor
        onDisconnected?.invoke()
    }

    override fun queryProductDetails(
        onSuccess: (Map<String, ProductDetails>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        onFailure("Billing not supported in this build")
    }

    override fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails) {
        // No-op for default flavor
    }

    override fun queryPurchases(
        onSuccess: (List<Purchase>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        onSuccess(emptyList())
    }

    override fun acknowledgePurchase(
        purchase: Purchase,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        onFailure("Billing not supported in this build")
    }

    override fun setPurchaseListener(listener: PurchaseUpdateListener?) {
        // No-op for default flavor
    }

    override fun isReady(): Boolean = false

    override fun destroy() {
        // No-op for default flavor
    }
}
