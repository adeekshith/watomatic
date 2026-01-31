package com.parishod.watomatic.billing

import android.app.Activity
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase

interface BillingManager {
    /**
     * Start connection to Google Play Billing
     * @param onConnected Callback when connection is successful
     * @param onDisconnected Callback when connection fails or disconnects
     */
    fun startConnection(onConnected: (() -> Unit)? = null, onDisconnected: (() -> Unit)? = null)
    
    /**
     * Query product details for subscription SKUs
     * @param onSuccess Callback with map of productId to ProductDetails
     * @param onFailure Callback with error message
     */
    fun queryProductDetails(
        onSuccess: (Map<String, ProductDetails>) -> Unit,
        onFailure: (String) -> Unit
    )
    
    /**
     * Launch purchase flow for a subscription
     * @param activity Activity context
     * @param productDetails ProductDetails for the subscription to purchase
     */
    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails)
    
    /**
     * Query existing purchases (for restoration)
     * @param onSuccess Callback with list of active purchases
     * @param onFailure Callback with error message
     */
    fun queryPurchases(
        onSuccess: (List<Purchase>) -> Unit,
        onFailure: (String) -> Unit
    )
    
    /**
     * Acknowledge a purchase
     * @param purchase Purchase to acknowledge
     * @param onSuccess Callback when acknowledgment succeeds
     * @param onFailure Callback when acknowledgment fails
     */
    fun acknowledgePurchase(
        purchase: Purchase,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    )
    
    /**
     * Set listener for purchase updates
     */
    fun setPurchaseListener(listener: PurchaseUpdateListener?)
    
    /**
     * Clean up resources
     */
    fun destroy()
    
    /**
     * Check if billing client is ready
     */
    fun isReady(): Boolean

    companion object {
        const val SKU_MONTHLY = "automatic_ai_pro_monthly"
        const val SKU_ANNUAL = "automatic_ai_pro_annual"
    }
}

/**
 * Listener for purchase updates
 */
interface PurchaseUpdateListener {
    fun onPurchaseSuccess(purchase: Purchase)
    fun onPurchasePending(purchase: Purchase)
    fun onPurchaseFailure(errorMessage: String)
    fun onPurchaseCancelled()
}
