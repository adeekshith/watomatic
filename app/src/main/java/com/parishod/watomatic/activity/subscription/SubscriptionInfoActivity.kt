package com.parishod.watomatic.activity.subscription

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.parishod.watomatic.R
import com.parishod.watomatic.activity.BaseActivity
import com.parishod.watomatic.billing.BillingManager
import com.parishod.watomatic.utils.UnsavedChangesDialog
import com.parishod.watomatic.billing.BillingManagerImpl
import com.parishod.watomatic.billing.PurchaseUpdateListener
import com.parishod.watomatic.model.preferences.PreferencesManager
import com.parishod.watomatic.model.subscription.SubscriptionState
import androidx.lifecycle.lifecycleScope
import com.parishod.watomatic.model.utils.Constants
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SubscriptionInfoActivity : BaseActivity() {
    private var preferencesManager: PreferencesManager? = null
    private var billingManager: BillingManager? = null
    private var subscriptionManager: com.parishod.watomatic.model.subscription.SubscriptionManager? = null

    // Mode passed via intent
    private var mode: SubscriptionMode? = null
    
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

    // Dirty state tracking for Active State
    private var isDirty = false
    private var initialAiPrompt: String = ""
    private var initialFallbackMessage: String = ""
    private var currentUIState: UIState = UIState.LOADING

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

        // Read mode from intent
        mode = SubscriptionMode.fromIntent(intent)
        Log.d("SubscriptionInfo", "Opened with mode: $mode")

        // Setup back button handler for unsaved changes
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Check if we're in ACTIVE state and have unsaved changes
                if (currentUIState == UIState.ACTIVE && isDirty) {
                    showUnsavedChangesDialog()
                } else {
                    // Default behavior - remove callback and trigger back
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // Dismiss quota exhausted notification if opened from it
        if (intent.getBooleanExtra("from_quota_notification", false)) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.cancel(1002) // QUOTA_NOTIFICATION_ID
        }

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

        // Track current state
        currentUIState = state

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

    /**
     * Resolve which UIState to show based on mode + subscription state.
     *
     * Rules:
     * - Non-subscribed user: always show plans list (INACTIVE)
     * - MANAGE mode: show active subscription UI (ACTIVE)
     * - UPGRADE mode: show plans list with FREE marked as current (INACTIVE)
     * - No mode (legacy / fallback): use existing behaviour based on isActive
     */
    private fun resolveUIState(state: SubscriptionState): UIState {
        if (state.isLoading) return UIState.LOADING

        // Non-subscribed user → always show plans list regardless of mode
        if (!state.isActive) return UIState.INACTIVE

        // Subscribed user → mode-driven
        return when (mode) {
            SubscriptionMode.MANAGE -> UIState.ACTIVE
            SubscriptionMode.UPGRADE -> UIState.INACTIVE  // plans list with FREE disabled
            null -> UIState.ACTIVE  // legacy fallback: subscribed → show active
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

            val uiState = resolveUIState(state)

            when (uiState) {
                UIState.LOADING -> {
                    Log.d("SubscriptionInfo", "Still loading subscription status...")
                }
                UIState.ACTIVE -> {
                    Log.d("SubscriptionInfo", "Showing ACTIVE state (mode=$mode)")
                    showUIState(UIState.ACTIVE)
                    updateActiveSubscriptionDetails(state)
                }
                UIState.INACTIVE -> {
                    Log.d("SubscriptionInfo", "Showing INACTIVE state (mode=$mode)")
                    showUIState(UIState.INACTIVE)

                    // If in UPGRADE mode with active subscription, notify fragments
                    // to mark the user's current plan tier as current
                    if (mode == SubscriptionMode.UPGRADE && state.isActive) {
                        markCurrentPlanInFragments()
                    }

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
        val savedAiPrompt = preferencesManager?.getAtomaticAICustomPrompt() ?: Constants.DEFAULT_LLM_PROMPT
        val savedFallbackMessage = preferencesManager?.getFallbackMessage() ?: ""

        aiPromptInput?.setText(savedAiPrompt)
        fallbackMessageInput?.setText(savedFallbackMessage)

        // Store initial values for dirty tracking
        initialAiPrompt = savedAiPrompt
        initialFallbackMessage = savedFallbackMessage
        isDirty = false

        // Add TextWatchers to detect changes
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                checkDirtyState()
            }
        }

        aiPromptInput?.addTextChangedListener(textWatcher)
        fallbackMessageInput?.addTextChangedListener(textWatcher)

        // Save Configuration button (reusing manage_subscription_button ID)
        manageButton?.setOnClickListener {
            // Save AI configuration
            // Get the current value from the edit box (will be default prompt or user-edited)
            val aiPrompt = aiPromptInput?.text?.toString() ?: Constants.DEFAULT_LLM_PROMPT
            val fallbackMessage = fallbackMessageInput?.text?.toString() ?: ""

            if(aiPrompt.isEmpty() && fallbackMessage.isEmpty()) {
                Toast.makeText(this, R.string.ai_config_empty_error, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save the prompt to preferences (will be default or user-edited)
            preferencesManager?.saveAtomaticAICustomPrompt(aiPrompt)
            preferencesManager?.saveFallbackMessage(fallbackMessage)

            // Reset dirty state after successful save
            initialAiPrompt = aiPrompt
            initialFallbackMessage = fallbackMessage
            isDirty = false

            Toast.makeText(this, R.string.ai_config_saved, Toast.LENGTH_SHORT).show()
            onNavigateUp()
        }
        
        // Reset to Defaults link
        helpLink?.setOnClickListener {
            // Reset to default prompt instead of empty string
            aiPromptInput?.setText(Constants.DEFAULT_LLM_PROMPT)
            fallbackMessageInput?.setText("")
            Toast.makeText(this, R.string.ai_config_reset, Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Check if user has made changes to the configuration
     */
    private fun checkDirtyState() {
        val currentAiPrompt = aiPromptInput?.text?.toString() ?: ""
        val currentFallbackMessage = fallbackMessageInput?.text?.toString() ?: ""

        isDirty = currentAiPrompt != initialAiPrompt || currentFallbackMessage != initialFallbackMessage
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

        // If in UPGRADE mode, mark current plan tier after fragments load
        if (mode == SubscriptionMode.UPGRADE) {
            viewPager?.post {
                markCurrentPlanInFragments()
            }
        }
    }

    /**
     * Resolve the user's current plan tier name from the stored product ID.
     * Product IDs follow the pattern: atomatic_ai_{tier}_{period}
     * e.g. "atomatic_ai_mini_monthly" → "mini"
     * For the free plan, planType is stored as "free".
     */
    private fun resolveCurrentPlanTier(): String {
        val productId = preferencesManager?.subscriptionProductId ?: ""
        val planType = preferencesManager?.subscriptionPlanType ?: ""

        return when {
            productId.contains("pro", ignoreCase = true) -> "pro"
            productId.contains("standard", ignoreCase = true) -> "standard"
            productId.contains("mini", ignoreCase = true) -> "mini"
            planType.equals("free", ignoreCase = true) -> "free"
            else -> "free" // safe default
        }
    }

    /**
     * In UPGRADE mode, notify all plan fragments to mark the user's current
     * plan as "Current Plan" and disable selection on it and below.
     */
    private fun markCurrentPlanInFragments() {
        val tier = resolveCurrentPlanTier()
        viewPager?.postDelayed({
            for (i in 0..1) {
                val fragment = supportFragmentManager.findFragmentByTag("f$i")
                if (fragment is SubscriptionPlansFragment) {
                    fragment.setCurrentPlan(tier)
                }
            }
        }, 300)
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
                        // Re-apply UPGRADE mode state on fragment after page change
                        if (mode == SubscriptionMode.UPGRADE) {
                            fragment.setCurrentPlan(resolveCurrentPlanTier())
                        }
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
            "Subscribe to ${planName.replaceFirstChar { it.uppercase() }} Plan"
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

    private var lastUpdateTimestamp: Long = 0

    private fun updateFragmentPrices() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateTimestamp < 500) {
            android.util.Log.d("SubscriptionInfo", "Skipping redundant price update")
            return
        }
        lastUpdateTimestamp = currentTime

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
        // Handle FREE plan separately - no Google Play Billing needed
        if (selectedPlanName?.equals("free", ignoreCase = true) == true) {
            Log.d("SubscriptionInfo", "FREE plan selected - activating without Google Play Billing")
            activateFreePlan()
            return
        }

        // For paid plans, proceed with normal Google Play Billing flow
        if (selectedProductDetails == null) {
            Toast.makeText(this, "Please select a plan", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading()
        billingManager?.launchPurchaseFlow(this, selectedProductDetails!!)
    }

    /**
     * Activate FREE plan without Google Play Billing.
     * Generates a simulated purchase and sends it to backend.
     */
    private fun activateFreePlan() {
        showLoading()

        lifecycleScope.launch {
            try {
                Log.d("SubscriptionInfo", "Activating FREE plan...")

                val success = subscriptionManager?.activateFreePlan() ?: false

                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    hideLoading()

                    if (success) {
                        Toast.makeText(
                            this@SubscriptionInfoActivity,
                            "FREE plan activated successfully!",
                            Toast.LENGTH_SHORT
                        ).show()

                        Log.d("SubscriptionInfo", "FREE plan activated, refreshing UI...")

                        // Refresh subscription status
                        try {
                            subscriptionManager?.refreshSubscriptionStatus()

                            // Check if active and recreate
                            val currentState = subscriptionManager?.subscriptionStatus?.value
                            if (currentState?.isActive == true) {
                                Log.d("SubscriptionInfo", "FREE plan is active, recreating activity")
                                restartInManageMode()
                            }
                        } catch (e: Exception) {
                            Log.e("SubscriptionInfo", "Error refreshing after FREE plan activation", e)
                            restartInManageMode()
                        }
                    } else {
                        Toast.makeText(
                            this@SubscriptionInfoActivity,
                            "Failed to activate FREE plan. Please try again.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("SubscriptionInfo", "Error activating FREE plan", e)
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    hideLoading()
                    Toast.makeText(
                        this@SubscriptionInfoActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
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

                                    // Refresh subscription status
                                    lifecycleScope.launch {
                                        try {
                                            Log.d(
                                                "SubscriptionInfo",
                                                "Starting status refresh (restore)..."
                                            )

                                            // Trigger refresh
                                            subscriptionManager?.refreshSubscriptionStatus()

                                            // Only recreate activity if this is an interactive restore
                                            // For automatic sync (isInteractive=false), let LiveData observer handle UI update
                                            if (isInteractive) {
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
                                                            "Reloading activity (interactive restore)."
                                                        )
                                                        restartInManageMode()
                                                    }
                                                }
                                            } else {
                                                Log.d(
                                                    "SubscriptionInfo",
                                                    "Automatic sync - UI will update via LiveData observer"
                                                )
                                            }
                                        } catch (e: Exception) {
                                            Log.e(
                                                "SubscriptionInfo",
                                                "Error refreshing subscription status",
                                                e
                                            )
                                            // Only recreate on interactive restore, even on error
                                            if (isInteractive) {
                                                withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                    restartInManageMode()
                                                }
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
                                    restartInManageMode()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("SubscriptionInfo", "Error refreshing subscription status", e)
                            // Still try to recreate to show updated UI
                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                restartInManageMode()
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

    /**
     * Re-launch this activity in MANAGE mode after a successful purchase or plan activation.
     * Using recreate() would re-read the original intent (e.g. UPGRADE mode), so instead we
     * start a fresh instance with MANAGE mode and finish the current one.
     */
    private fun restartInManageMode() {
        val intent = android.content.Intent(this, SubscriptionInfoActivity::class.java)
        intent.putExtra(SubscriptionMode.EXTRA_KEY, SubscriptionMode.MANAGE.name)
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        finish()
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

    /**
     * Show a dialog to confirm discarding unsaved changes
     */
    private fun showUnsavedChangesDialog() {
        UnsavedChangesDialog.show(
            context = this,
            onDiscard = {
                isDirty = false  // Clear dirty flag to prevent dialog from showing again
                finish()  // Close the activity
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager?.destroy()
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
