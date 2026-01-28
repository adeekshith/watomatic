package com.parishod.watomatic.backend

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await

/**
 * Firebase Cloud Functions implementation of BackendService
 */
class FirebaseBackendService(private val context: Context) : BackendService {
    
    companion object {
        private const val TAG = "FirebaseBackend"
    }
    
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1")
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    
    override suspend fun verifyPurchase(
        purchaseToken: String,
        productId: String,
        orderId: String
    ): VerificationResult {
        return try {
            val user = auth.currentUser
            if (user == null) {
                Log.e(TAG, "verifyPurchase failed: User not logged in (currentUser is null)")
                return VerificationResult(
                    isValid = false,
                    expiryTime = 0,
                    autoRenewing = false,
                    planType = "",
                    error = "User not logged in"
                )
            }
            
            // CRITICAL: Force refresh the ID token before calling Cloud Function
            // This ensures the token is fresh and not expired (tokens expire after 1 hour)
            // The Firebase SDK automatically attaches this token to callable requests
            try {
                val tokenResult = user.getIdToken(true).await()
                Log.d(TAG, "Token refreshed successfully, expiry: ${tokenResult.expirationTimestamp}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh token before verification", e)
                return VerificationResult(
                    isValid = false,
                    expiryTime = 0,
                    autoRenewing = false,
                    planType = "",
                    error = "Authentication failed: Unable to refresh token - ${e.message}"
                )
            }

            Log.d(TAG, "Verifying purchase for user: ${user.uid}")

            // Note: awaitAuthReady() is no longer needed here since we already verified user exists
            // and refreshed the token above

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

    /**
     * Ensures the user is authenticated with a fresh ID token.
     * This is CRITICAL for Firebase HTTPS Callable functions to work properly.
     * 
     * @param forceRefresh If true, forces a network request to refresh the token even if cached.
     * @return true if authentication is ready with a fresh token, false otherwise.
     */
    private suspend fun ensureAuthenticatedWithFreshToken(forceRefresh: Boolean = true): Boolean {
        // Wait for auth state if not ready
        if (auth.currentUser == null) {
            try {
                suspendCancellableCoroutine<Unit> { cont ->
                    val listener = object : FirebaseAuth.AuthStateListener {
                        override fun onAuthStateChanged(firebaseAuth: FirebaseAuth) {
                            if (firebaseAuth.currentUser != null && cont.isActive) {
                                auth.removeAuthStateListener(this)
                                cont.resume(Unit) {}
                            }
                        }
                    }
                    auth.addAuthStateListener(listener)
                    cont.invokeOnCancellation { auth.removeAuthStateListener(listener) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed waiting for auth state", e)
                return false
            }
        }
        
        // Force refresh the token to ensure it's valid
        val user = auth.currentUser ?: return false
        return try {
            val tokenResult = user.getIdToken(forceRefresh).await()
            Log.d(TAG, "Token ready, expiry: ${tokenResult.expirationTimestamp}, issuedAt: ${tokenResult.authTimestamp}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh token", e)
            false
        }
    }
    
    @Deprecated("Use ensureAuthenticatedWithFreshToken instead")
    suspend fun awaitAuthReady() {
        if (auth.currentUser != null) return

        suspendCancellableCoroutine<Unit> { cont ->
            val listener = FirebaseAuth.AuthStateListener {
                if (auth.currentUser != null && cont.isActive) {
                    cont.resume(Unit) {}
                }
            }
            auth.addAuthStateListener(listener)
            cont.invokeOnCancellation { auth.removeAuthStateListener(listener) }
        }
    }

    override suspend fun getSubscriptionStatus(userId: String): SubscriptionStatus {
        return try {
            // Ensure fresh token before callable request
            if (!ensureAuthenticatedWithFreshToken()) {
                Log.e(TAG, "getSubscriptionStatus: Authentication not ready")
                return SubscriptionStatus(false, 0, null, null, false)
            }
            
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
            // Ensure fresh token before callable request
            if (!ensureAuthenticatedWithFreshToken()) {
                Log.e(TAG, "registerDevice: Authentication not ready")
                return DeviceRegistrationResult(false, 0, 0, "Authentication not ready")
            }
            
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
            // Ensure fresh token before callable request
            if (!ensureAuthenticatedWithFreshToken()) {
                Log.e(TAG, "removeDevice: Authentication not ready")
                return false
            }
            
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
            // Ensure fresh token before callable request
            if (!ensureAuthenticatedWithFreshToken()) {
                Log.e(TAG, "getDevices: Authentication not ready")
                return emptyList()
            }
            
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
