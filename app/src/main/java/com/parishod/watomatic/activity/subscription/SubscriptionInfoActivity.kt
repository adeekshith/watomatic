package com.parishod.watomatic.activity.subscription

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.parishod.watomatic.R
import com.parishod.watomatic.activity.BaseActivity
import com.parishod.watomatic.billing.BillingManager
import com.parishod.watomatic.billing.BillingManagerImpl
import com.parishod.watomatic.billing.PurchaseUpdateListener
import com.parishod.watomatic.model.preferences.PreferencesManager
import com.parishod.watomatic.model.subscription.SubscriptionState
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SubscriptionInfoActivity : BaseActivity() {
    private var preferencesManager: PreferencesManager? = null
    private var billingManager: BillingManager? = null
    private var subscriptionManager: com.parishod.watomatic.model.subscription.SubscriptionManager? = null
    
    // State Views
    private var loadingStateView: View? = null
    private var activeStateView: View? = null
    private var inactiveStateView: View? = null

    // UI Components (Inactive State)
    private var monthlyPlanCard: MaterialCardView? = null
    private var annualPlanCard: MaterialCardView? = null
    private var subscribeButton: Button? = null
    private var restoreButton: Button? = null
    private var statusTextView: TextView? = null
    private var loadingIndicator: ProgressBar? = null
    private var monthlyPriceText: TextView? = null
    private var annualPriceText: TextView? = null
    
    // UI Components (Active State)
    private var planTypeText: TextView? = null
    private var renewalDateText: TextView? = null
    private var manageButton: Button? = null
    private var helpLink: TextView? = null

    // State
    private var selectedPlanType: String? = null
    private val productDetailsMap = mutableMapOf<String, ProductDetails>()

    private enum class UIState {
        LOADING, ACTIVE, INACTIVE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Always use the unified layout
        setContentView(R.layout.activity_subscription_unified)

        // Setup toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.subscription_info_title)

        // Apply window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.subscription_scroll_view)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize managers
        preferencesManager = PreferencesManager.getPreferencesInstance(this)
        try {
            billingManager = BillingManagerImpl(this) as BillingManager
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Billing service not available", Toast.LENGTH_SHORT).show()
        }

        // Initialize subscription manager
        preferencesManager?.let {
            Log.d("SubscriptionInfo", "Initializing subscription manager")
            subscriptionManager = com.parishod.watomatic.model.subscription.SubscriptionManagerImpl(this, it)
        }

        // Initialize views
        initializeViews()

        // Start with loading state
        showUIState(UIState.LOADING)

        // Setup billing and subscription status
        setupBillingAndSubscription()
    }

    private fun initializeViews() {
        // Get state view containers
        loadingStateView = findViewById(R.id.loading_state)
        activeStateView = findViewById(R.id.active_state)
        inactiveStateView = findViewById(R.id.inactive_state)

        // Initialize inactive state views
        monthlyPlanCard = findViewById(R.id.monthly_plan_card)
        annualPlanCard = findViewById(R.id.annual_plan_card)
        subscribeButton = findViewById(R.id.subscribe_button)
        restoreButton = findViewById(R.id.restore_button)
        statusTextView = findViewById(R.id.subscription_status)
        loadingIndicator = findViewById(R.id.loading_indicator)
        monthlyPriceText = findViewById(R.id.monthly_price)
        annualPriceText = findViewById(R.id.annual_price)

        // Initialize active state views
        planTypeText = findViewById(R.id.subscription_plan_type)
        renewalDateText = findViewById(R.id.subscription_renewal_date)
        manageButton = findViewById(R.id.manage_subscription_button)
        helpLink = findViewById(R.id.help_learn_more)
    }

    private fun showUIState(state: UIState) {
        Log.d("SubscriptionInfo", "Showing UI state: $state")

        // Hide all states first
        loadingStateView?.visibility = View.GONE
        activeStateView?.visibility = View.GONE
        inactiveStateView?.visibility = View.GONE

        // Show the appropriate state
        when (state) {
            UIState.LOADING -> {
                loadingStateView?.visibility = View.VISIBLE
            }
            UIState.ACTIVE -> {
                activeStateView?.visibility = View.VISIBLE
                setupActiveStateUI()
            }
            UIState.INACTIVE -> {
                inactiveStateView?.visibility = View.VISIBLE
                setupInactiveStateUI()
            }
        }
    }

    private fun setupBillingAndSubscription() {
        // Setup billing listener
        setupBillingListener()

        // Initialize billing connection
        initializeBilling()

        // Observe subscription status from SubscriptionManager
        observeSubscriptionStatus()
    }

    private fun observeSubscriptionStatus() {
        if (subscriptionManager == null) {
            Log.e("SubscriptionInfo", "subscriptionManager is NULL! Cannot observe subscription status")
            showUIState(UIState.INACTIVE)
            return
        }

        // Observe LiveData from SubscriptionManager
        subscriptionManager?.subscriptionStatus?.observe(this) { state ->
            Log.d("SubscriptionInfo", "Subscription state changed: isActive=${state.isActive}, isLoading=${state.isLoading}, error=${state.error}")

            when {
                state.isLoading -> {
                    // Keep showing loading state
                    Log.d("SubscriptionInfo", "Still loading subscription status...")
                }
                state.isActive -> {
                    Log.d("SubscriptionInfo", "Subscription is ACTIVE - showing active state")
                    showUIState(UIState.ACTIVE)
                    updateActiveSubscriptionDetails(state)
                }
                else -> {
                    Log.d("SubscriptionInfo", "Subscription is INACTIVE - showing inactive state")
                    showUIState(UIState.INACTIVE)
                    if (state.error != null) {
                        updateStatusText("Status: ${state.error}")
                    } else {
                        val userEmail = preferencesManager?.userEmail ?: ""
                        if (userEmail.isNotEmpty()) {
                            updateStatusText("Logged in as $userEmail")
                        } else {
                            updateStatusText("No active subscription")
                        }
                    }
                }
            }
        }
        
        // Trigger a refresh of subscription status from backend
        lifecycleScope.launch {
            try {
                Log.d("SubscriptionInfo", "Refreshing subscription status from backend...")
                subscriptionManager?.refreshSubscriptionStatus()
                Log.d("SubscriptionInfo", "Subscription status refresh completed")
            } catch (e: Exception) {
                Log.e("SubscriptionInfo", "Error refreshing subscription status", e)
                // On error, fall back to showing inactive state
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    showUIState(UIState.INACTIVE)
                }
            }
        }
    }

    private fun setupActiveStateUI() {
        manageButton?.setOnClickListener {
            // Open Google Play subscription management
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                intent.data = android.net.Uri.parse("https://play.google.com/store/account/subscriptions")
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Unable to open subscription management", Toast.LENGTH_SHORT).show()
            }
        }
        
        helpLink?.setOnClickListener {
            // Open help/support page
            Toast.makeText(this, "Opening help center...", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateActiveSubscriptionDetails(state: SubscriptionState) {
        // Update plan type
        val planType = state.planType?.let { type ->
            when {
                type.contains("monthly", ignoreCase = true) -> "Premium Monthly"
                type.contains("annual", ignoreCase = true) -> "Premium Annual"
                else -> "Premium Plan"
            }
        } ?: "Premium Plan"

        planTypeText?.text = planType

        // Update renewal date
        val renewalDate = if (state.expiryDate > 0) {
            val dateFormat = java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault())
            dateFormat.format(java.util.Date(state.expiryDate))
        } else {
            "N/A"
        }

        renewalDateText?.text = renewalDate
    }

    private fun setupInactiveStateUI() {
        setupClickListeners()

        // Check for pre-selected plan from login flow
        val preselectedPlan = intent.getStringExtra("PRESELECTED_PLAN")
        preselectedPlan?.let { sku ->
            when {
                sku.contains("monthly") -> selectPlan("monthly")
                sku.contains("annual") -> selectPlan("annual")
            }
        }
    }

    private fun setupClickListeners() {
        monthlyPlanCard?.setOnClickListener {
            selectPlan("monthly")
        }

        annualPlanCard?.setOnClickListener {
            selectPlan("annual")
        }

        subscribeButton?.setOnClickListener {
            handleSubscription()
        }
        
        restoreButton?.setOnClickListener {
            restorePurchases()
        }
    }

    private fun setupBillingListener() {
        billingManager?.setPurchaseListener(object : PurchaseUpdateListener {
            override fun onPurchaseSuccess(purchase: Purchase) {
                hideLoading()
                Toast.makeText(
                    this@SubscriptionInfoActivity,
                    "Purchase successful! Processing...",
                    Toast.LENGTH_SHORT
                ).show()
                
                // Acknowledge purchase
                acknowledgePurchase(purchase)
            }

            override fun onPurchasePending(purchase: Purchase) {
                hideLoading()
                Toast.makeText(
                    this@SubscriptionInfoActivity,
                    "Purchase pending - waiting for payment confirmation",
                    Toast.LENGTH_LONG
                ).show()
                updateStatusText("Purchase pending...")
            }

            override fun onPurchaseFailure(errorMessage: String) {
                hideLoading()
                Toast.makeText(
                    this@SubscriptionInfoActivity,
                    "Purchase failed: $errorMessage",
                    Toast.LENGTH_LONG
                ).show()
            }

            override fun onPurchaseCancelled() {
                hideLoading()
                Toast.makeText(
                    this@SubscriptionInfoActivity,
                    "Purchase cancelled",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun initializeBilling() {
        billingManager?.startConnection(
            onConnected = {
                queryProductDetails()
                // Sync purchases with backend to ensure up-to-date status
                syncActivePurchases(isInteractive = false)
            },
            onDisconnected = {
                Toast.makeText(
                    this,
                    "Failed to connect to billing service",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun queryProductDetails() {
        billingManager?.queryProductDetails(
            onSuccess = { products ->
                runOnUiThread {
                    productDetailsMap.clear()
                    productDetailsMap.putAll(products)
                    updatePricing()
                }
            },
            onFailure = { error ->
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Failed to load subscription plans: $error",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    private fun updatePricing() {
        // Update monthly price
        productDetailsMap[BillingManager.SKU_MONTHLY]?.let { details ->
            val priceInfo = details.subscriptionOfferDetails?.firstOrNull()
                ?.pricingPhases?.pricingPhaseList?.firstOrNull()
            monthlyPriceText?.text = priceInfo?.formattedPrice ?: "$1.99/month"
        }

        // Update annual price
        productDetailsMap[BillingManager.SKU_ANNUAL]?.let { details ->
            val priceInfo = details.subscriptionOfferDetails?.firstOrNull()
                ?.pricingPhases?.pricingPhaseList?.firstOrNull()
            annualPriceText?.text = priceInfo?.formattedPrice ?: "$10.99/year"
        }
    }


    private fun selectPlan(planType: String) {
        selectedPlanType = planType
        
        // Reset both cards
        monthlyPlanCard?.strokeWidth = 0
        annualPlanCard?.strokeWidth = 0

        // Highlight selected plan
        val selectedCard = if (planType == "monthly") monthlyPlanCard else annualPlanCard
        selectedCard?.strokeWidth = 4
        selectedCard?.strokeColor = getColor(R.color.primary)
        
        // Update button text based on selected plan
        val planName = if (planType == "monthly") "Monthly" else "Annual"
        subscribeButton?.text = "Subscribe to $planName Plan"
    }

    private fun handleSubscription() {
        if (selectedPlanType == null) {
            Toast.makeText(this, "Please select a plan", Toast.LENGTH_SHORT).show()
            return
        }

        val productId = when (selectedPlanType) {
            "monthly" -> BillingManager.SKU_MONTHLY
            "annual" -> BillingManager.SKU_ANNUAL
            else -> {
                Toast.makeText(this, "Invalid plan selected", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val productDetails = productDetailsMap[productId]
        if (productDetails == null) {
            Toast.makeText(
                this,
                "Product details not available. Please try again.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        showLoading()
        billingManager?.launchPurchaseFlow(this, productDetails)
    }

    private fun restorePurchases() {
        showLoading()
        syncActivePurchases(isInteractive = true)
    }

    private fun syncActivePurchases(isInteractive: Boolean) {
        billingManager?.queryPurchases(
            onSuccess = { purchases ->
                if (purchases.isEmpty()) {
                    if (isInteractive) {
                        runOnUiThread {
                            hideLoading()
                            Toast.makeText(
                                this,
                                "No purchases to restore",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    // Process each restored purchase with backend
                    var successCount = 0
                    var failCount = 0

                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
                        .launch {
                            purchases.forEach { purchase ->
                                val productId = purchase.products.firstOrNull() ?: ""
                                val result = subscriptionManager?.restorePurchase(
                                    purchase.purchaseToken,
                                    productId,
                                    purchase.orderId ?: ""
                                )

                                if (result == true) successCount++ else failCount++
                            }

                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                if (isInteractive) hideLoading()
                                
                                if (successCount > 0) {
                                    if (isInteractive) {
                                        Toast.makeText(
                                            this@SubscriptionInfoActivity,
                                            "Successfully restored subscription!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }

                                    // Refresh subscription status and recreate activity
                                    lifecycleScope.launch {
                                        try {
                                            Log.d(
                                                "SubscriptionInfo",
                                                "Starting status refresh loop (restore)..."
                                            )

                                            // Trigger refresh
                                            subscriptionManager?.refreshSubscriptionStatus()

                                            // Check status directly
                                            val currentState =
                                                subscriptionManager?.subscriptionStatus?.value
                                            if (currentState?.isActive == true) {
                                                Log.d(
                                                    "SubscriptionInfo",
                                                    "Status is active! Ready to update UI."
                                                )
                                                // Recreate the activity
                                                withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                    Log.d(
                                                        "SubscriptionInfo",
                                                        "Reloading activity."
                                                    )
                                                    recreate()
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Log.e(
                                                "SubscriptionInfo",
                                                "Error refreshing subscription status",
                                                e
                                            )
                                            // Still try to recreate to show updated UI
                                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                recreate()
                                            }
                                        }
                                    }
                                } else {
                                    if (isInteractive) {
                                        Toast.makeText(
                                            this@SubscriptionInfoActivity,
                                            "Found purchases but failed to verify with backend.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        }
                }
            },
            onFailure = { error ->
                if (isInteractive) {
                    runOnUiThread {
                        hideLoading()
                        Toast.makeText(
                            this,
                            "Failed to restore: $error",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        Log.d("SubscriptionInfo", "Purchase acknowledgement started for ${purchase.orderId}")
        showLoading()
        billingManager?.acknowledgePurchase(
            purchase = purchase,
            onSuccess = {
                runOnUiThread {
                    hideLoading()
                    Toast.makeText(
                        this,
                        "Subscription activated!",
                        Toast.LENGTH_SHORT
                    ).show()

                    Log.d("SubscriptionInfo", "Subscription activated! Check and refresh subscription status...")
                    // Refresh subscription status from backend with retries
                    lifecycleScope.launch {
                        try {
                            Log.d("SubscriptionInfo", "Starting status refresh loop...")
                            // Trigger refresh - this is a suspend function that waits for backend response
                            subscriptionManager?.refreshSubscriptionStatus()

                            // Check current status directly
                            val currentState = subscriptionManager?.subscriptionStatus?.value
                            if (currentState?.isActive == true) {
                                Log.d("SubscriptionInfo", "Status is active! Ready to update UI.")
                                // Recreate the activity
                                withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    Log.d("SubscriptionInfo", "Reloading activity. Active status confirmed.")
                                    recreate()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("SubscriptionInfo", "Error refreshing subscription status", e)
                            // Still try to recreate to show updated UI
                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                recreate()
                            }
                        }
                    }
                }
            },
            onFailure = { error ->
                runOnUiThread {
                    hideLoading()
                    Toast.makeText(
                        this,
                        "Failed to activate subscription: $error",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    private fun showLoading() {
        loadingIndicator?.visibility = View.VISIBLE
        subscribeButton?.isEnabled = false
        restoreButton?.isEnabled = false
    }

    private fun hideLoading() {
        loadingIndicator?.visibility = View.GONE
        subscribeButton?.isEnabled = true
        restoreButton?.isEnabled = true
    }

    private fun updateStatusText(text: String) {
        statusTextView?.text = text
        statusTextView?.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager?.destroy()
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
