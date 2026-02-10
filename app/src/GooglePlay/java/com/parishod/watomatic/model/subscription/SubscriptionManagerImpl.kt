package com.parishod.watomatic.model.subscription

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.parishod.watomatic.backend.BackendService
import com.parishod.watomatic.backend.FirebaseBackendService
import com.parishod.watomatic.model.preferences.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Implementation of SubscriptionManager.
 * Orchestrates local cache and backend verification.
 */
class SubscriptionManagerImpl(
    private val context: Context,
    private val preferencesManager: PreferencesManager
) : SubscriptionManager {

    private val _subscriptionStatus = MutableLiveData<SubscriptionState>()
    override val subscriptionStatus: LiveData<SubscriptionState> = _subscriptionStatus

    private val backendService: BackendService = FirebaseBackendService(context)

    init {
        // Initialize state from local preferences
        updateStateFromPrefs()
    }

    override fun isProUser(): Boolean {
        return preferencesManager.isSubscriptionActive
    }

    override fun hasFeatureAccess(feature: SubscriptionManager.ProFeature): Boolean {
        // Currently all Pro features are gated by the same single subscription
        return isProUser()
    }

    override suspend fun refreshSubscriptionStatus() {
        // Set loading state on main thread
        withContext(Dispatchers.Main) {
            _subscriptionStatus.value = _subscriptionStatus.value?.copy(isLoading = true)
        }

        try {
            val userId = preferencesManager.userEmail // Using email as ID for now, should use Firebase UID ideally
            Log.d("TAG", "subscription email ${userId.toString()}")
            // However, BackendService expects a userId. 
            // If user refers to Firebase Auth UID, we should get it from FirebaseAuth.
            // But here let's assume we pass the email or whatever ID strategy we use.
            // Actually, FirebaseBackendService uses context.auth to get UID internally if not passed, 
            // but the interface takes userId. Let's fix this slightly or pass empty string if the backend service handles it.
            // Looking at FirebaseBackendService implementation: it checks context.auth.uid.
            // If we pass an ID, it checks if it matches auth.uid.
            // So we need the real UID.
            
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            Log.d("TAG", "subscription uid ${uid.toString()}")
            if (uid == null) {
                // Not logged in
                withContext(Dispatchers.Main) {
                    _subscriptionStatus.value = SubscriptionState(
                        isActive = false,
                        isLoading = false,
                        error = "User not logged in"
                    )
                }
                return
            }

            // Call backend
            val status = backendService.getSubscriptionStatus(uid)
            Log.d("TAG", "subscription status ${status.toString()}")
            // Update local cache
            withContext(Dispatchers.Main) {
                preferencesManager.setSubscriptionActive(status.isActive)
                preferencesManager.setSubscriptionExpiryTime(status.expiryTime)
                preferencesManager.setSubscriptionPlanType(status.planType ?: "")
                preferencesManager.setSubscriptionProductName(status.productName ?: "")
                preferencesManager.setSubscriptionAutoRenewing(status.autoRenewing)
                preferencesManager.setSubscriptionProductId(status.productId ?: "")
                preferencesManager.setLastVerifiedTime(System.currentTimeMillis())

                updateStateFromPrefs()
            }
        } catch (e: Exception) {
            Log.e("TAG", "refreshSubscriptionStatus error", e)
            withContext(Dispatchers.Main) {
                _subscriptionStatus.value = _subscriptionStatus.value?.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    override suspend fun restorePurchase(purchaseToken: String, productId: String, orderId: String, productName: String?): Boolean {
        return try {
            withContext(Dispatchers.Main) {
                _subscriptionStatus.value = _subscriptionStatus.value?.copy(isLoading = true)
            }
            
            // Use BackendService to verify and register this purchase, including productName
            val result = backendService.verifyPurchase(purchaseToken, productId, orderId, productName)

            if (result.isValid) {
                withContext(Dispatchers.Main) {
                    preferencesManager.setSubscriptionActive(true)
                    preferencesManager.setSubscriptionExpiryTime(result.expiryTime)
                    preferencesManager.setSubscriptionPlanType(result.planType)
                    preferencesManager.setSubscriptionAutoRenewing(result.autoRenewing)
                    preferencesManager.setSubscriptionProductId(productId)
                    preferencesManager.setLastVerifiedTime(System.currentTimeMillis())
                    
                    updateStateFromPrefs()
                }
                true
            } else {
                withContext(Dispatchers.Main) {
                    _subscriptionStatus.value = _subscriptionStatus.value?.copy(
                        isLoading = false,
                        error = result.error ?: "Restoration failed"
                    )
                }
                false
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _subscriptionStatus.value = _subscriptionStatus.value?.copy(
                    isLoading = false,
                    error = e.message
                )
            }
            false
        }
    }

    override suspend fun activateFreePlan(): Boolean {
        return try {
            withContext(Dispatchers.Main) {
                _subscriptionStatus.value = _subscriptionStatus.value?.copy(isLoading = true)
            }

            Log.d("SubscriptionManager", "Activating FREE plan...")

            // Generate simulated purchase
            val simulatedPurchase = SimulatedPurchase.generate()
            Log.d("SubscriptionManager", "Generated simulated purchase: orderId=${simulatedPurchase.orderId}")

            // Validate simulated purchase integrity
            if (!simulatedPurchase.isValid()) {
                Log.e("SubscriptionManager", "Simulated purchase validation failed")
                withContext(Dispatchers.Main) {
                    _subscriptionStatus.value = _subscriptionStatus.value?.copy(
                        isLoading = false,
                        error = "Failed to generate valid FREE plan purchase"
                    )
                }
                return false
            }

            // Send to backend for verification
            val payload = simulatedPurchase.toBackendPayload(
                packageName = context.packageName,
                productName = "Free Plan"
            )

            val result = backendService.verifySimulatedPurchase(payload)

            if (result.isValid) {
                Log.d("SubscriptionManager", "FREE plan activated successfully")
                withContext(Dispatchers.Main) {
                    preferencesManager.setSubscriptionActive(true)
                    preferencesManager.setSubscriptionExpiryTime(result.expiryTime)
                    preferencesManager.setSubscriptionPlanType("free")
                    preferencesManager.setSubscriptionProductName("Free Plan")
                    preferencesManager.setSubscriptionAutoRenewing(false)
                    preferencesManager.setSubscriptionProductId(simulatedPurchase.productId)
                    preferencesManager.setLastVerifiedTime(System.currentTimeMillis())

                    updateStateFromPrefs()
                }
                true
            } else {
                Log.e("SubscriptionManager", "FREE plan activation failed: ${result.error}")
                withContext(Dispatchers.Main) {
                    _subscriptionStatus.value = _subscriptionStatus.value?.copy(
                        isLoading = false,
                        error = result.error ?: "FREE plan activation failed"
                    )
                }
                false
            }
        } catch (e: Exception) {
            Log.e("SubscriptionManager", "FREE plan activation error", e)
            withContext(Dispatchers.Main) {
                _subscriptionStatus.value = _subscriptionStatus.value?.copy(
                    isLoading = false,
                    error = e.message
                )
            }
            false
        }
    }

    private fun updateStateFromPrefs() {
        val state = SubscriptionState(
            isActive = preferencesManager.isSubscriptionActive,
            planType = preferencesManager.subscriptionPlanType,
            productName = preferencesManager.subscriptionProductName,
            expiryDate = preferencesManager.subscriptionExpiryTime,
            isAutoRenewing = preferencesManager.isSubscriptionAutoRenewing,
            isLoading = false,
            error = null
        )
        _subscriptionStatus.value = state
    }
}
