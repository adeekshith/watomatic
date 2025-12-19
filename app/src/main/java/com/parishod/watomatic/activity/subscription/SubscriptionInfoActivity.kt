package com.parishod.watomatic.activity.subscription

import android.os.Bundle
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

class SubscriptionInfoActivity : BaseActivity() {
    private var preferencesManager: PreferencesManager? = null
    private var billingManager: BillingManager? = null
    
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
        setContentView(R.layout.activity_subscription_info)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.subscription_info_title)

        preferencesManager = PreferencesManager.getPreferencesInstance(this)
        billingManager = BillingManagerImpl(this)

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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.subscription_scroll_view)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
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
        showLoading()
        billingManager?.startConnection(
            onConnected = {
                queryProductDetails()
                loadSubscriptionStatus()
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
                productDetailsMap.clear()
                productDetailsMap.putAll(products)
                updatePricing()
                hideLoading()
            },
            onFailure = { error ->
                hideLoading()
                Toast.makeText(
                    this,
                    "Failed to load subscription plans: $error",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun updatePricing() {
        // Update monthly price
        productDetailsMap[BillingManagerImpl.SKU_MONTHLY]?.let { details ->
            val priceInfo = details.subscriptionOfferDetails?.firstOrNull()
                ?.pricingPhases?.pricingPhaseList?.firstOrNull()
            monthlyPriceText?.text = priceInfo?.formattedPrice ?: "$1.99/month"
        }

        // Update annual price
        productDetailsMap[BillingManagerImpl.SKU_ANNUAL]?.let { details ->
            val priceInfo = details.subscriptionOfferDetails?.firstOrNull()
                ?.pricingPhases?.pricingPhaseList?.firstOrNull()
            annualPriceText?.text = priceInfo?.formattedPrice ?: "$10.99/year"
        }
    }

    private fun loadSubscriptionStatus() {
        billingManager?.queryPurchases(
            onSuccess = { purchases ->
                if (purchases.isNotEmpty()) {
                    val purchase = purchases.first()
                    val productId = purchase.products.firstOrNull() ?: ""
                    val planType = when {
                        productId.contains("monthly") -> "Monthly"
                        productId.contains("annual") -> "Annual"
                        else -> "Unknown"
                    }
                    
                    updateStatusText("Active: $planType Plan")
                    subscribeButton?.text = "Manage Subscription"
                } else {
                    val userEmail = preferencesManager?.userEmail ?: ""
                    if (userEmail.isNotEmpty()) {
                        updateStatusText("Logged in as $userEmail")
                    } else {
                        updateStatusText("No active subscription")
                    }
                }
            },
            onFailure = { error ->
                updateStatusText("Failed to load subscription status")
            }
        )
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
            "monthly" -> BillingManagerImpl.SKU_MONTHLY
            "annual" -> BillingManagerImpl.SKU_ANNUAL
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
                hideLoading()
                if (purchases.isEmpty()) {
                    Toast.makeText(
                        this,
                        "No purchases to restore",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Restored ${purchases.size} purchase(s)",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadSubscriptionStatus()
                }
            },
            onFailure = { error ->
                hideLoading()
                Toast.makeText(
                    this,
                    "Failed to restore purchases: $error",
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
