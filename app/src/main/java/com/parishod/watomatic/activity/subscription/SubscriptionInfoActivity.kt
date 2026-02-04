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
import androidx.viewpager2.widget.ViewPager2
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
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

    // UI Components (Inactive State - ViewPager2)
    private var viewPager: ViewPager2? = null
    private var tabLayout: TabLayout? = null
    private var pagerAdapter: SubscriptionPagerAdapter? = null
    private var subscribeButton: Button? = null
    private var restoreButton: Button? = null
    private var statusTextView: TextView? = null
    private var loadingIndicator: ProgressBar? = null

    // UI Components (Active State)
    private var planTypeText: TextView? = null
    private var renewalDateText: TextView? = null
    private var manageButton: Button? = null
    private var helpLink: TextView? = null
    private var aiPromptInput: android.widget.EditText? = null
    private var fallbackMessageInput: android.widget.EditText? = null

    // State
    private var selectedProductDetails: ProductDetails? = null
    private var selectedPlanName: String? = null
    private var selectedPlanType: String? = null  // "monthly" or "annual"
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
        viewPager = findViewById(R.id.subscription_view_pager)
        tabLayout = findViewById(R.id.subscription_tabs)
        subscribeButton = findViewById(R.id.subscribe_button)
        restoreButton = findViewById(R.id.restore_button)
        statusTextView = findViewById(R.id.subscription_status)
        loadingIndicator = findViewById(R.id.loading_indicator)

        // Initialize active state views
        planTypeText = findViewById(R.id.subscription_plan_type)
        renewalDateText = findViewById(R.id.subscription_renewal_date)
        manageButton = findViewById(R.id.manage_subscription_button)
        helpLink = findViewById(R.id.help_learn_more)
        aiPromptInput = findViewById(R.id.ai_prompt_input)
        fallbackMessageInput = findViewById(R.id.fallback_message_input)
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
        // Load saved AI configuration from preferences
        aiPromptInput?.setText(preferencesManager?.getOpenAICustomPrompt() ?: "")
        fallbackMessageInput?.setText(preferencesManager?.getFallbackMessage() ?: "")

        // Save Configuration button (reusing manage_subscription_button ID)
        manageButton?.setOnClickListener {
            // Save AI configuration
            val aiPrompt = aiPromptInput?.text?.toString() ?: ""
            val fallbackMessage = fallbackMessageInput?.text?.toString() ?: ""

            preferencesManager?.saveOpenAICustomPrompt(aiPrompt)
            preferencesManager?.saveFallbackMessage(fallbackMessage)

            Toast.makeText(this, R.string.ai_config_saved, Toast.LENGTH_SHORT).show()
        }
        
        // Reset to Defaults link
        helpLink?.setOnClickListener {
            aiPromptInput?.setText("")
            fallbackMessageInput?.setText("")
            Toast.makeText(this, R.string.ai_config_reset, Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateActiveSubscriptionDetails(state: SubscriptionState) {
        // Update plan type - use actual product name if available
        val planType = if (!state.productName.isNullOrEmpty()) {
            // Use actual product name from Google Play Billing
            state.productName
        } else {
            // Fallback to inferred plan name
            state.planType?.let { type ->
                when {
                    type.contains("monthly", ignoreCase = true) -> "Premium Monthly"
                    type.contains("annual", ignoreCase = true) -> "Premium Annual"
                    else -> "Premium Plan"
                }
            } ?: "Premium Plan"
        }

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
        // Setup ViewPager2 with adapter
        pagerAdapter = SubscriptionPagerAdapter(this)
        viewPager?.adapter = pagerAdapter

        // Apply custom tab background selector to match 3.html design
        tabLayout?.let { tabs ->
            // Set tab background selector
            for (i in 0 until tabs.tabCount) {
                tabs.getTabAt(i)?.view?.setBackgroundResource(R.drawable.tab_selector)
            }

            // Setup with ViewPager2
            viewPager?.let { pager ->
                TabLayoutMediator(tabs, pager) { tab, position ->
                    val tabView = layoutInflater.inflate(R.layout.tab_annual_with_badge, null)
                    when (position) {
                        0 -> {tab.text = "Monthly"
                            /*tabView.findViewById<TextView>(R.id.tab_text).text = "Monthly"
                            tabView.findViewById<TextView>(R.id.save_badge).visibility = View.GONE
                            tab.customView = tabView*/
                        } 1 -> {
                            // Create custom view for Annual tab with SAVE badge
                            tab.customView = tabView
                        }
                        else -> tab.text = ""
                    }
                }.attach()
            }

            // Re-apply background after attach
            tabs.post {
                for (i in 0 until tabs.tabCount) {
                    tabs.getTabAt(i)?.view?.setBackgroundResource(R.drawable.tab_selector)
                }
            }
        }

        // Setup plan selection listener for both fragments
        setupFragmentCallbacks()

        // Setup button listeners
        setupClickListeners()

        // Check for pre-selected plan from login flow
        val preselectedPlan = intent.getStringExtra("PRESELECTED_PLAN")
        preselectedPlan?.let { sku ->
            when {
                sku.contains("annual") -> viewPager?.currentItem = 1
                else -> viewPager?.currentItem = 0
            }
        }
    }

    private fun setupFragmentCallbacks() {
        // Wait for fragments to be created and setup callbacks
        viewPager?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                // Update tab text colors for custom Annual tab
                updateTabTextColors(position)

                // Update selected plan type when tab changes
                selectedPlanType = if (position == 0) "monthly" else "annual"
                // Reset selection when switching tabs
                selectedProductDetails = null
                selectedPlanName = null
                updateSubscribeButtonText()

                // Update prices for the newly visible fragment
                viewPager?.postDelayed({
                    val fragment = supportFragmentManager.findFragmentByTag("f$position")
                    if (fragment is SubscriptionPlansFragment) {
                        fragment.updatePrices(productDetailsMap)
                        fragment.setOnPlanSelectedListener(object : SubscriptionPlansFragment.OnPlanSelectedListener {
                            override fun onPlanSelected(productDetails: ProductDetails?, planName: String, planType: String) {
                                this@SubscriptionInfoActivity.onPlanSelected(productDetails, planName, planType)
                            }
                        })
                    }
                }, 100)
            }
        })
    }

    private fun updateTabTextColors(selectedPosition: Int) {
        tabLayout?.let { tabs ->
            // Update Annual tab custom view text color
            tabs.getTabAt(1)?.customView?.let { customView ->
                val tabText = customView.findViewById<TextView>(R.id.tab_text)
                if (selectedPosition == 1) {
                    // Selected state - use colorOnSurface
                    val typedValue = android.util.TypedValue()
                    theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)
                    tabText?.setTextColor(typedValue.data)
                } else {
                    // Unselected state - use colorOnSurfaceVariant
                    val typedValue = android.util.TypedValue()
                    theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, typedValue, true)
                    tabText?.setTextColor(typedValue.data)
                }
            }
        }
    }

    fun onPlanSelected(productDetails: ProductDetails?, planName: String, planType: String) {
        selectedProductDetails = productDetails
        selectedPlanName = planName
        selectedPlanType = planType
        updateSubscribeButtonText()
    }

    private fun updateSubscribeButtonText() {
        val planName = selectedPlanName
        subscribeButton?.text = if (planName != null) {
            "Subscribe to ${planName.capitalize()} Plan"
        } else {
            getString(R.string.subscription_subscribe_continue)
        }
    }

    private fun setupClickListeners() {
        subscribeButton?.setOnClickListener {
            handleSubscription()
        }
        
        restoreButton?.setOnClickListener {
            restorePurchases()
        }
    }

    private fun updateFragmentPrices() {
        // Use postDelayed to ensure fragments are fully created and attached
        viewPager?.postDelayed({
            // Update both fragments (monthly and annual)
            for (i in 0..1) {
                val fragment = supportFragmentManager.findFragmentByTag("f$i")
                if (fragment is SubscriptionPlansFragment) {
                    android.util.Log.d("SubscriptionInfo", "Updating prices for fragment $i")
                    fragment.updatePrices(productDetailsMap)
                    fragment.setOnPlanSelectedListener(object : SubscriptionPlansFragment.OnPlanSelectedListener {
                        override fun onPlanSelected(productDetails: ProductDetails?, planName: String, planType: String) {
                            this@SubscriptionInfoActivity.onPlanSelected(productDetails, planName, planType)
                        }
                    })
                } else {
                    android.util.Log.w("SubscriptionInfo", "Fragment $i not found or not SubscriptionPlansFragment")
                }
            }
        }, 300) // 300ms delay to ensure fragments are ready
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
        // Update prices in both fragments
        // Use post to ensure fragments are created
        viewPager?.post {
            updateFragmentPrices()
        }
    }

    private fun handleSubscription() {
        // For free plan, just show a message
        if (selectedPlanName == "free") {
            Toast.makeText(this, "Free plan is already active", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedProductDetails == null) {
            Toast.makeText(this, "Please select a plan", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading()
        billingManager?.launchPurchaseFlow(this, selectedProductDetails!!)
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

                                // Get product name from cached ProductDetails
                                val productDetails = productDetailsMap[productId]
                                val productName = productDetails?.name ?: productDetails?.title ?: productId

                                Log.d("SubscriptionInfo", "Restoring purchase for $productId with name: $productName")

                                val result = subscriptionManager?.restorePurchase(
                                    purchase.purchaseToken,
                                    productId,
                                    purchase.orderId ?: "",
                                    productName
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
