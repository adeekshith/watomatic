package com.parishod.watomatic.backend

import android.content.Context

/**
 * Stub implementation of BackendService for Default flavor (non-Google Play)
 */
class FirebaseBackendService(private val context: Context) : BackendService {
    
    override suspend fun verifyPurchase(
        purchaseToken: String,
        productId: String,
        orderId: String
    ): VerificationResult {
        return VerificationResult(
            isValid = false,
            expiryTime = 0,
            autoRenewing = false,
            planType = "",
            error = "Backend not supported in this build"
        )
    }
    
    override suspend fun getSubscriptionStatus(userId: String): SubscriptionStatus {
        return SubscriptionStatus(
            isActive = false,
            expiryTime = 0,
            productId = null,
            planType = null,
            autoRenewing = false
        )
    }
    
    override suspend fun registerDevice(
        userId: String,
        deviceId: String,
        deviceName: String
    ): DeviceRegistrationResult {
        return DeviceRegistrationResult(
            success = false,
            deviceCount = 0,
            deviceLimit = 0,
            error = "Backend not supported in this build"
        )
    }
    
    override suspend fun removeDevice(userId: String, deviceId: String): Boolean {
        return false
    }
    
    override suspend fun getDevices(userId: String): List<DeviceInfo> {
        return emptyList()
    }
}
