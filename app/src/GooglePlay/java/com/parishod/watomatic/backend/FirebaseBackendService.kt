package com.parishod.watomatic.backend

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

/**
 * Firebase Cloud Functions implementation of BackendService
 */
class FirebaseBackendService(private val context: Context) : BackendService {
    
    companion object {
        private const val TAG = "FirebaseBackend"
    }
    
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    
    override suspend fun verifyPurchase(
        purchaseToken: String,
        productId: String,
        orderId: String
    ): VerificationResult {
        return try {
            val data = hashMapOf(
                "purchaseToken" to purchaseToken,
                "productId" to productId,
                "orderId" to orderId,
                "packageName" to context.packageName
            )
            
            val result = functions
                .getHttpsCallable("verifyPurchase")
                .call(data)
                .await()
            
            val resultData = result.data as? Map<*, *> ?: run {
                Log.e(TAG, "Invalid response format from verifyPurchase")
                return VerificationResult(
                    isValid = false,
                    expiryTime = 0,
                    autoRenewing = false,
                    planType = "",
                    error = "Invalid response format"
                )
            }
            
            VerificationResult(
                isValid = resultData["isValid"] as? Boolean ?: false,
                expiryTime = (resultData["expiryTime"] as? Number)?.toLong() ?: 0,
                autoRenewing = resultData["autoRenewing"] as? Boolean ?: false,
                planType = resultData["planType"] as? String ?: "",
                error = resultData["error"] as? String
            )
        } catch (e: Exception) {
            Log.e(TAG, "Verification failed", e)
            VerificationResult(
                isValid = false,
                expiryTime = 0,
                autoRenewing = false,
                planType = "",
                error = e.message ?: "Unknown error"
            )
        }
    }
    
    override suspend fun getSubscriptionStatus(userId: String): SubscriptionStatus {
        return try {
            val data = hashMapOf("userId" to userId)
            val result = functions
                .getHttpsCallable("getSubscriptionStatus")
                .call(data)
                .await()
            
            val resultData = result.data as? Map<*, *> ?: run {
                Log.e(TAG, "Invalid response format from getSubscriptionStatus")
                return SubscriptionStatus(false, 0, null, null, false)
            }
            
            SubscriptionStatus(
                isActive = resultData["isActive"] as? Boolean ?: false,
                expiryTime = (resultData["expiryTime"] as? Number)?.toLong() ?: 0,
                productId = resultData["productId"] as? String,
                planType = resultData["planType"] as? String,
                autoRenewing = resultData["autoRenewing"] as? Boolean ?: false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Get status failed", e)
            SubscriptionStatus(false, 0, null, null, false)
        }
    }
    
    override suspend fun registerDevice(
        userId: String,
        deviceId: String,
        deviceName: String
    ): DeviceRegistrationResult {
        return try {
            val data = hashMapOf(
                "userId" to userId,
                "deviceId" to deviceId,
                "deviceName" to deviceName
            )
            val result = functions
                .getHttpsCallable("registerDevice")
                .call(data)
                .await()
            
            val resultData = result.data as? Map<*, *> ?: run {
                Log.e(TAG, "Invalid response format from registerDevice")
                return DeviceRegistrationResult(false, 0, 0, "Invalid response")
            }
            
            DeviceRegistrationResult(
                success = resultData["success"] as? Boolean ?: false,
                deviceCount = (resultData["deviceCount"] as? Number)?.toInt() ?: 0,
                deviceLimit = (resultData["deviceLimit"] as? Number)?.toInt() ?: 0,
                error = resultData["error"] as? String
            )
        } catch (e: Exception) {
            Log.e(TAG, "Register device failed", e)
            DeviceRegistrationResult(false, 0, 0, e.message)
        }
    }
    
    override suspend fun removeDevice(userId: String, deviceId: String): Boolean {
        return try {
            val data = hashMapOf("userId" to userId, "deviceId" to deviceId)
            val result = functions
                .getHttpsCallable("removeDevice")
                .call(data)
                .await()
            
            val resultData = result.data as? Map<*, *>
            resultData?.get("success") as? Boolean ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Remove device failed", e)
            false
        }
    }
    
    override suspend fun getDevices(userId: String): List<DeviceInfo> {
        return try {
            val data = hashMapOf("userId" to userId)
            val result = functions
                .getHttpsCallable("getDevices")
                .call(data)
                .await()
            
            val resultData = result.data as? Map<*, *> ?: run {
                Log.e(TAG, "Invalid response format from getDevices")
                return emptyList()
            }
            
            val devices = resultData["devices"] as? List<*> ?: return emptyList()
            
            devices.mapNotNull { device ->
                val deviceMap = device as? Map<*, *> ?: return@mapNotNull null
                DeviceInfo(
                    deviceId = deviceMap["deviceId"] as? String ?: return@mapNotNull null,
                    deviceName = deviceMap["deviceName"] as? String ?: "",
                    lastActive = (deviceMap["lastActive"] as? Number)?.toLong() ?: 0,
                    firstRegistered = (deviceMap["firstRegistered"] as? Number)?.toLong() ?: 0
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get devices failed", e)
            emptyList()
        }
    }
}
