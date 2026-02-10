package com.parishod.watomatic.model.subscription

import java.util.UUID

/**
 * Represents a simulated Google In-App Purchase for FREE plan.
 *
 * This simulates a real Google IAP without calling Google Play Billing APIs.
 * Used exclusively for FREE plan subscriptions.
 *
 * IMPORTANT: All simulated purchases must be flagged and handled differently
 * on the backend to prevent abuse.
 */
data class SimulatedPurchase(
    val orderId: String,
    val purchaseToken: String,
    val productId: String,
    val purchaseTime: Long,
    val expiryTime: Long,
    val purchaseState: Int = PURCHASE_STATE_PURCHASED,
    val acknowledged: Boolean = true,
    val autoRenewing: Boolean = false,
    val purchaseSource: String = PURCHASE_SOURCE_SIMULATED
) {
    companion object {
        const val PURCHASE_STATE_PURCHASED = 1
        const val PURCHASE_SOURCE_SIMULATED = "SIMULATED_GOOGLE_IAP"
        const val ORDER_ID_PREFIX = "GPA.FREE."
        const val FREE_PLAN_PRODUCT_ID = "atomatic_ai_free_monthly"
        const val FREE_PLAN_DURATION_DAYS = 30L

        /**
         * Generate a simulated purchase for FREE plan.
         * This creates a payload identical to real Google IAP purchases.
         *
         * @param productId The SKU/product ID for the free plan
         * @return SimulatedPurchase object with all required fields
         */
        fun generate(productId: String = FREE_PLAN_PRODUCT_ID): SimulatedPurchase {
            val currentTimeMillis = System.currentTimeMillis()
            val expiryTimeMillis = currentTimeMillis + (FREE_PLAN_DURATION_DAYS * 24 * 60 * 60 * 1000)

            return SimulatedPurchase(
                orderId = ORDER_ID_PREFIX + currentTimeMillis,
                purchaseToken = UUID.randomUUID().toString(),
                productId = productId,
                purchaseTime = currentTimeMillis,
                expiryTime = expiryTimeMillis,
                purchaseState = PURCHASE_STATE_PURCHASED,
                acknowledged = true,
                autoRenewing = false,
                purchaseSource = PURCHASE_SOURCE_SIMULATED
            )
        }

        /**
         * Check if an orderId indicates a simulated purchase.
         */
        fun isSimulatedPurchase(orderId: String?): Boolean {
            return orderId?.startsWith(ORDER_ID_PREFIX) == true
        }

        /**
         * Check if a purchase source indicates simulation.
         */
        fun isSimulatedSource(purchaseSource: String?): Boolean {
            return purchaseSource == PURCHASE_SOURCE_SIMULATED
        }
    }

    /**
     * Convert to a map for backend API call.
     * This format matches the real Google IAP verification payload.
     */
    fun toBackendPayload(packageName: String, productName: String = "Free Plan"): Map<String, Any> {
        return hashMapOf(
            "purchaseToken" to purchaseToken,
            "productId" to productId,
            "orderId" to orderId,
            "packageName" to packageName,
            "productName" to productName,
            "purchaseTime" to purchaseTime,
            "expiryTime" to expiryTime,
            "purchaseState" to purchaseState,
            "acknowledged" to acknowledged,
            "autoRenewing" to autoRenewing,
            "purchaseSource" to purchaseSource
        )
    }

    /**
     * Validate the simulated purchase integrity.
     * Ensures the purchase hasn't been tampered with.
     */
    fun isValid(): Boolean {
        return orderId.startsWith(ORDER_ID_PREFIX) &&
                purchaseToken.isNotEmpty() &&
                productId.isNotEmpty() &&
                expiryTime == purchaseTime + (FREE_PLAN_DURATION_DAYS * 24 * 60 * 60 * 1000) &&
                purchaseSource == PURCHASE_SOURCE_SIMULATED
    }
}

