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

// import com.parishod.watomatic.billing.BillingManagerImpl // Removed to avoid compile error
import com.parishod.watomatic.billing.PurchaseUpdateListener
import com.parishod.watomatic.model.preferences.PreferencesManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SubscriptionInfoActivity : BaseActivity() {
    private var preferencesManager: PreferencesManager? = null
    private var billingManager: BillingManager? = null
    private var subscriptionManager: com.parishod.watomatic.model.subscription.SubscriptionManager? = null
    
    // UI Components
    private var monthlyPlanCard: MaterialCardView? = null
    private var annualPlanCard: MaterialCardView? = null
    private var subscribeButton: Button? = null
    private var restoreButton: Button? = null
    private var statusTextView: TextView? = null
    private var loadingIndicator: ProgressBar? = null
    
    // Pricing TextViews
    private var monthlyPriceText: TextView? = null
    private var annualPriceText: TextView? = null
    
    // State
    private var selectedPlanType: String? = null
    private val productDetailsMap = mutableMapOf<String, ProductDetails>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        preferencesManager = PreferencesManager.getPreferencesInstance(this)
        try {
            billingManager = BillingManagerImpl(this) as BillingManager
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Billing service not available", Toast.LENGTH_SHORT).show()
        }
        
        // Initialize subscription manager first
        preferencesManager?.let {
            Log.d("TAG", "Init Subscription manager")
            subscriptionManager = com.parishod.watomatic.model.subscription.SubscriptionManagerImpl(this, it)
        }
        
        // Check if subscription is active and load appropriate layout
        val isActive = preferencesManager?.isSubscriptionActive() ?: false
        if (isActive) {
            setContentView(R.layout.activity_subscription_active)
            setupActiveSubscriptionUI()
        } else {
            setContentView(R.layout.activity_subscription_info)
            setupInactiveSubscriptionUI()
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.subscription_info_title)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.subscription_scroll_view)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    
    private fun setupActiveSubscriptionUI() {
        val manageButton = findViewById<Button>(R.id.manage_subscription_button)
        val planTypeText = findViewById<TextView>(R.id.subscription_plan_type)
        val renewalDateText = findViewById<TextView>(R.id.subscription_renewal_date)
        val helpLink = findViewById<TextView>(R.id.help_learn_more)
        
        // Load subscription details
        val planType = preferencesManager?.getSubscriptionPlanType()?.let { type ->
            when {
                type.contains("monthly", ignoreCase = true) -> "Premium Monthly"
                type.contains("annual", ignoreCase = true) -> "Premium Annual"
                else -> "Premium Plan"
            }
        } ?: "Premium Plan"
        
        val expiryTime = preferencesManager?.getSubscriptionExpiryTime() ?: 0L
        val renewalDate = if (expiryTime > 0) {
            val dateFormat = java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault())
            dateFormat.format(java.util.Date(expiryTime))
        } else {
            "N/A"
        }
        
        planTypeText.text = planType
        renewalDateText.text = renewalDate
        
        manageButton.setOnClickListener {
            // Open Google Play subscription management
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                intent.data = android.net.Uri.parse("https://play.google.com/store/account/subscriptions")
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Unable to open subscription management", Toast.LENGTH_SHORT).show()
            }
        }
        
        helpLink.setOnClickListener {
            // Open help/support page
            Toast.makeText(this, "Opening help center...", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupInactiveSubscriptionUI() {
        initializeViews()
        setupClickListeners()
        setupBillingListener()
        
        // Check for pre-selected plan from login flow
        val preselectedPlan = intent.getStringExtra("PRESELECTED_PLAN")
        preselectedPlan?.let { sku ->
            when {
                sku.contains("monthly") -> selectPlan("monthly")
                sku.contains("annual") -> selectPlan("annual")
            }
        }
        
        // Initialize billing connection
        initializeBilling()
    }

    private fun initializeViews() {
        monthlyPlanCard = findViewById(R.id.monthly_plan_card)
        annualPlanCard = findViewById(R.id.annual_plan_card)
        subscribeButton = findViewById(R.id.subscribe_button)
        restoreButton = findViewById(R.id.restore_button)
        statusTextView = findViewById(R.id.subscription_status)
        loadingIndicator = findViewById(R.id.loading_indicator)
        monthlyPriceText = findViewById(R.id.monthly_price)
        annualPriceText = findViewById(R.id.annual_price)
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
        //showLoading()
        billingManager?.startConnection(
            onConnected = {
                queryProductDetails()
                // Ensure we're on the main thread when calling loadSubscriptionStatus
                // because LiveData observers must be registered on the main thread
                runOnUiThread {
                    loadSubscriptionStatus()
                }
            },
            onDisconnected = {
                hideLoading()
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
                    hideLoading()
                }
            },
            onFailure = { error ->
                runOnUiThread {
                    hideLoading()
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

    private fun loadSubscriptionStatus() {
        Log.d("TAG", "loadSubscriptionStatus - subscriptionManager is ${if (subscriptionManager == null) "NULL" else "NOT NULL"}")
        
        if (subscriptionManager == null) {
            Log.e("TAG", "subscriptionManager is NULL! Cannot load subscription status")
            updateStatusText("Error: Subscription manager not initialized")
            return
        }
        
        // Observe LiveData from SubscriptionManager
        subscriptionManager?.subscriptionStatus?.observe(this) { state ->
            Log.d("TAG", "Observer triggered with state: ${state.toString()}")
            if (state.isActive) {
                val planType = state.planType?.capitalize() ?: "Active"
                updateStatusText("Active: $planType Plan")
                subscribeButton?.text = "Manage Subscription"
                // Possibly hide subscribe button or change to "Manage" depending on requirements
            } else if (state.error != null) {
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
        
        // Trigger a refresh using lifecycleScope
        Log.d("TAG", "About to launch coroutine for refreshSubscriptionStatus")
        lifecycleScope.launch {
            Log.d("TAG", "Inside coroutine - about to call refreshSubscriptionStatus")
            try {
                subscriptionManager?.refreshSubscriptionStatus()
                Log.d("TAG", "refreshSubscriptionStatus completed")
            } catch (e: Exception) {
                Log.e("TAG", "Error calling refreshSubscriptionStatus", e)
            }
        }
        Log.d("TAG", "Coroutine launched")
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
        
        // Update button text
        subscribeButton?.text = "Subscribe to ${planType.capitalize()} Plan"
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
        billingManager?.queryPurchases(
            onSuccess = { purchases ->
                if (purchases.isEmpty()) {
                    hideLoading()
                    Toast.makeText(
                        this,
                        "No purchases to restore",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // Process each restored purchase with backend
                    var successCount = 0
                    var failCount = 0
                    val totalToRestore = purchases.size
                    
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
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
                            hideLoading()
                            if (successCount > 0) {
                                Toast.makeText(
                                    this@SubscriptionInfoActivity,
                                    "Successfully restored subscription!",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(
                                    this@SubscriptionInfoActivity,
                                    "Found purchases but failed to verify with backend.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            },
            onFailure = { error ->
                hideLoading()
                Toast.makeText(
                    this,
                    "Failed to restore: $error",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        showLoading()
        billingManager?.acknowledgePurchase(
            purchase = purchase,
            onSuccess = {
                hideLoading()
                Toast.makeText(
                    this,
                    "Subscription activated!",
                    Toast.LENGTH_SHORT
                ).show()
                loadSubscriptionStatus()
                // TODO: Sync to backend in Phase 2
            },
            onFailure = { error ->
                hideLoading()
                Toast.makeText(
                    this,
                    "Failed to activate subscription: $error",
                    Toast.LENGTH_LONG
                ).show()
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
