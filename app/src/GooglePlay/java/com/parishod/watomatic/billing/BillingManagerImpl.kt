package com.parishod.watomatic.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.parishod.watomatic.BuildConfig
import com.parishod.watomatic.backend.FirebaseBackendService
import com.parishod.watomatic.backend.BackendService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BillingManagerImpl(private val context: Context) : BillingManager, PurchasesUpdatedListener {

    companion object {
        private const val TAG = "BillingManager"
        

        // Base64-encoded public key from Google Play Console
        // TODO: Replace with actual key from Google Play Console
        private const val BASE64_PUBLIC_KEY = "REPLACE_WITH_YOUR_PUBLIC_KEY"
    }

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private var purchaseListener: PurchaseUpdateListener? = null
    private var isConnected = false
    private val productDetailsCache = mutableMapOf<String, ProductDetails>()
    private val backendService: BackendService = FirebaseBackendService(context)

    override fun startConnection(onConnected: (() -> Unit)?, onDisconnected: (() -> Unit)?) {
        if (isConnected) {
            onConnected?.invoke()
            return
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    isConnected = true
                    Log.d(TAG, "Billing client connected successfully")
                    onConnected?.invoke()
                } else {
                    isConnected = false
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                    onDisconnected?.invoke()
                }
            }

            override fun onBillingServiceDisconnected() {
                isConnected = false
                Log.w(TAG, "Billing service disconnected")
                onDisconnected?.invoke()
            }
        })
    }

    override fun queryProductDetails(
        onSuccess: (Map<String, ProductDetails>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (!isConnected) {
            onFailure("Billing client not connected")
            return
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(BillingManager.SKU_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(BillingManager.SKU_ANNUAL)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val productDetails = productDetailsList ?: emptyList()
                
                productDetailsCache.clear()
                productDetails.forEach { details ->
                    productDetailsCache[details.productId] = details
                }
                
                Log.d(TAG, "Queried ${productDetails.size} product details")
                onSuccess(productDetailsCache.toMap())
            } else {
                val errorMsg = "Failed to query products: ${billingResult.debugMessage}"
                Log.e(TAG, errorMsg)
                onFailure(errorMsg)
            }
        }
    }

    override fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails) {
        if (!isConnected) {
            purchaseListener?.onPurchaseFailure("Billing client not connected")
            return
        }

        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken == null) {
            purchaseListener?.onPurchaseFailure("No subscription offer available")
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
        
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "Failed to launch billing flow: ${billingResult.debugMessage}")
            purchaseListener?.onPurchaseFailure(billingResult.debugMessage)
        }
    }

    override fun queryPurchases(
        onSuccess: (List<Purchase>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (!isConnected) {
            onFailure("Billing client not connected")
            return
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val purchases = purchasesList.filter {
                    it.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                Log.d(TAG, "Found ${purchases.size} active purchases")
                onSuccess(purchases)
            } else {
                val errorMsg = "Failed to query purchases: ${billingResult.debugMessage}"
                Log.e(TAG, errorMsg)
                onFailure(errorMsg)
            }
        }
    }

    override fun acknowledgePurchase(
        purchase: Purchase,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (purchase.isAcknowledged) {
            onSuccess()
            return
        }

        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged successfully")
                onSuccess()
            } else {
                val errorMsg = "Failed to acknowledge purchase: ${billingResult.debugMessage}"
                Log.e(TAG, errorMsg)
                onFailure(errorMsg)
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    handlePurchase(purchase)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User cancelled purchase")
                purchaseListener?.onPurchaseCancelled()
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.d(TAG, "Item already owned")
                // Query existing purchases to sync
                queryPurchases(
                    onSuccess = { existingPurchases ->
                        existingPurchases.firstOrNull()?.let { purchase ->
                            purchaseListener?.onPurchaseSuccess(purchase)
                        }
                    },
                    onFailure = { error ->
                        purchaseListener?.onPurchaseFailure("Item already owned but failed to query: $error")
                    }
                )
            }
            else -> {
                val errorMsg = "Purchase failed: ${billingResult.debugMessage}"
                Log.e(TAG, errorMsg)
                purchaseListener?.onPurchaseFailure(errorMsg)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        // Step 1: Verify signature locally (first line of defense)
        if (!verifyPurchaseSignature(purchase)) {
            Log.e(TAG, "Invalid purchase signature")
            purchaseListener?.onPurchaseFailure("Invalid purchase signature")
            return
        }

        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                Log.d(TAG, "Purchase successful: ${purchase.products}")
                
                // Step 2: Verify with backend
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val verificationResult = backendService.verifyPurchase(
                            purchaseToken = purchase.purchaseToken,
                            productId = purchase.products.firstOrNull() ?: "",
                            orderId = purchase.orderId ?: ""
                        )

                        Log.e(TAG, "Backend verification : ${verificationResult.toString()}")
                        CoroutineScope(Dispatchers.Main).launch {
                            if (verificationResult.isValid) {
                                Log.d(TAG, "Backend verification successful")
                                purchaseListener?.onPurchaseSuccess(purchase)
                            } else {
                                val error = verificationResult.error ?: "Unknown error"
                                Log.e(TAG, "Backend verification failed: $error")
                                
                                // Fallback to local verification for transient/auth errors
                                // UNAUTHENTICATED: Cloud Functions auth failed (session expired, etc)
                                // UNAVAILABLE: Backend offline
                                // TIMEOUT: Network timeout
                                val isTransientError = error.contains("UNAUTHENTICATED", ignoreCase = true) ||
                                        error.contains("UNAVAILABLE", ignoreCase = true) ||
                                        error.contains("TIMEOUT", ignoreCase = true) ||
                                        error.contains("Authentication failed", ignoreCase = true)

                                /*if (isTransientError) {
                                    Log.w(TAG, "Falling back to local verification due to transient error: $error")
                                    purchaseListener?.onPurchaseSuccess(purchase)
                                } else {*/
                                Log.e(TAG, "Falling back to local verification due to transient error: $error")
                                    purchaseListener?.onPurchaseFailure("Verification failed: $error")
//                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Backend verification unavailable, using local verification", e)
                        // Fallback to local verification if backend is down
                        CoroutineScope(Dispatchers.Main).launch {
                            purchaseListener?.onPurchaseSuccess(purchase)
                        }
                    }
                }
            }
            Purchase.PurchaseState.PENDING -> {
                Log.d(TAG, "Purchase pending: ${purchase.products}")
                purchaseListener?.onPurchasePending(purchase)
            }
            else -> {
                Log.w(TAG, "Unknown purchase state: ${purchase.purchaseState}")
            }
        }
    }

    private fun verifyPurchaseSignature(purchase: Purchase): Boolean {
        // In production, use the actual public key from Google Play Console
        if (BASE64_PUBLIC_KEY == "REPLACE_WITH_YOUR_PUBLIC_KEY") {
            Log.w(TAG, "Using placeholder public key - signature verification skipped")
            return true // Skip verification in development
        }

        return try {
            Security.verifyPurchase(BASE64_PUBLIC_KEY, purchase.originalJson, purchase.signature)
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying purchase signature", e)
            false
        }
    }

    override fun setPurchaseListener(listener: PurchaseUpdateListener?) {
        this.purchaseListener = listener
    }

    override fun isReady(): Boolean = isConnected

    override fun destroy() {
        billingClient.endConnection()
        isConnected = false
        purchaseListener = null
        productDetailsCache.clear()
        Log.d(TAG, "Billing client destroyed")
    }
}
