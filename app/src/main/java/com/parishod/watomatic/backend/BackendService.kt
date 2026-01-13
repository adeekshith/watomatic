package com.parishod.watomatic.backend

/**
 * Backend service interface for subscription verification and management.
 * This abstraction allows easy switching between Firebase Cloud Functions and AWS Lambda.
 */
interface BackendService {
    /**
     * Verify a purchase with Google Play Developer API
     * @return VerificationResult with validity, expiry time, and subscription details
     */
    suspend fun verifyPurchase(
        purchaseToken: String,
        productId: String,
        orderId: String
    ): VerificationResult
    
    /**
     * Get current subscription status for user
     * @return SubscriptionStatus with active state and details
     */
    suspend fun getSubscriptionStatus(userId: String): SubscriptionStatus
    
    /**
     * Register a new device for the user
     * @return DeviceRegistrationResult with success/failure and device limit info
     */
    suspend fun registerDevice(
        userId: String,
        deviceId: String,
        deviceName: String
    ): DeviceRegistrationResult
    
    /**
     * Remove a device from user's account
     */
    suspend fun removeDevice(userId: String, deviceId: String): Boolean
    
    /**
     * Get list of registered devices
     */
    suspend fun getDevices(userId: String): List<DeviceInfo>
}

/**
 * Result of purchase verification
 */
data class VerificationResult(
    val isValid: Boolean,
    val expiryTime: Long,
    val autoRenewing: Boolean,
    val planType: String, // "monthly" or "annual"
    val error: String? = null
)

/**
 * Current subscription status
 */
data class SubscriptionStatus(
    val isActive: Boolean,
    val expiryTime: Long,
    val productId: String?,
    val planType: String?,
    val autoRenewing: Boolean
)

/**
 * Result of device registration
 */
data class DeviceRegistrationResult(
    val success: Boolean,
    val deviceCount: Int,
    val deviceLimit: Int,
    val error: String? = null
)

/**
 * Device information
 */
data class DeviceInfo(
    val deviceId: String,
    val deviceName: String,
    val lastActive: Long,
    val firstRegistered: Long
)
