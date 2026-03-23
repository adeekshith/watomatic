package com.parishod.watomatic.activity.customreplyeditor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.parishod.watomatic.R
import com.parishod.watomatic.activity.BaseActivity
import com.parishod.watomatic.activity.subscription.SubscriptionInfoActivity
import com.parishod.watomatic.activity.subscription.SubscriptionMode
import com.parishod.watomatic.flavor.FlavorNavigator
import com.parishod.watomatic.model.CustomRepliesData
import com.parishod.watomatic.model.preferences.PreferencesManager
import com.parishod.watomatic.utils.UnsavedChangesDialog
import com.parishod.watomatic.viewmodel.SwipeToKillAppDetectViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.content.SharedPreferences

class CustomReplyEditorActivity : BaseActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        private const val REPLY_METHOD_MANUAL = "manual"
        private const val REPLY_METHOD_AUTOMATIC_AI = "automatic_ai"
        private const val REPLY_METHOD_BYOK = "byok"
        private const val SUBSCRIPTION_STATUS_REFRESH_INTERVAL = 30 * 60 * 1000L // 30 minutes
    }

    /**
     * Data class to hold plan-specific UI configuration
     */
    private data class PlanUIConfig(
        val badgeText: String,
        val badgeBackgroundRes: Int,
        val dateLabel: String // e.g., "Renews on" or "Expires on"
    )

    /**
     * Get UI configuration based on subscription auto-renewal status
     * Single source of truth for plan-based UI logic
     *
     * @param isAutoRenewing Whether the subscription auto-renews
     * @return PlanUIConfig with appropriate badge and date label
     */
    private fun getPlanUIConfig(isAutoRenewing: Boolean): PlanUIConfig {
        return if (isAutoRenewing) {
            // Auto-renewing subscription (paid plans)
            PlanUIConfig(
                badgeText = getString(R.string.badge_pro),
                badgeBackgroundRes = R.drawable.bg_badge_pro,
                dateLabel = getString(R.string.subscription_renews_on).substringBefore(" %s") // "Renews on"
            )
        } else {
            // Non-renewing subscription (FREE plan or cancelled subscription)
            PlanUIConfig(
                badgeText = getString(R.string.badge_free),
                badgeBackgroundRes = R.drawable.bg_badge_pro,
                dateLabel = getString(R.string.subscription_expires_on).substringBefore(" %s") // "Expires on"
            )
        }
    }
    private var autoReplyText: TextInputEditText? = null
    private var saveAutoReplyTextBtn: Button? = null
    private var customRepliesData: CustomRepliesData? = null
    private var preferencesManager: PreferencesManager? = null
    private var subscriptionManager: com.parishod.watomatic.model.subscription.SubscriptionManager? = null
    private var watoMessageLinkBtn: TextView? = null
    private var manualRepliesCard: MaterialCardView? = null
    private var manualSettingsIcon: android.widget.ImageView? = null
    private var manualCheckIcon: android.widget.ImageView? = null
    private var automaticAiProviderCard: MaterialCardView? = null
    private var btnAtomaticAiEdit: TextView? = null
    private var otherAiProviderCard: MaterialCardView? = null
    private var btnOtherAiEdit: android.widget.ImageView? = null
    private var otherAiCheckIcon: android.widget.ImageView? = null

    // New UI elements for expanded Automatic AI card
    private var automaticAiExpandedContent: android.view.View? = null
    private var automaticAiCheckIcon: android.widget.ImageView? = null
    private var automaticAiSettingsIcon: android.widget.ImageView? = null
    private var automaticAiStatusIcon: android.widget.ImageView? = null
    private var automaticAiStatusText: TextView? = null
    private var automaticAiNotSubscribedSection: android.view.View? = null
    private var automaticAiSubscribedSection: android.view.View? = null
    private var btnUnlockSubscription: MaterialButton? = null
    private var btnUpgradePlan: MaterialButton? = null
    private var subscriptionRenewalDate: TextView? = null
    private var subscriptionPlanName: TextView? = null
    private var automaticAiRemainingReplies: TextView? = null
    private var automaticAiTag: TextView? = null

    // BYOK card expanded content
    private var otherAiExpandedContent: android.view.View? = null

    private var selectedReplyMethod = REPLY_METHOD_MANUAL
    private var initialSelectedReplyMethod = REPLY_METHOD_MANUAL

    private val otherAiConfigLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            if (data != null) {
                val provider = data.getStringExtra("provider")
                val apiKey = data.getStringExtra("apiKey")
                val model = data.getStringExtra("model")
                val systemPrompt = data.getStringExtra("systemPrompt")
                val baseUrl = data.getStringExtra("baseUrl")

                if (apiKey != null) preferencesManager?.saveOpenAIApiKey(apiKey)
                if (provider != null) preferencesManager?.saveOpenApiSource(provider)
                if (model != null) preferencesManager?.saveSelectedOpenAIModel(model)
                if (systemPrompt != null) preferencesManager?.saveOpenAICustomPrompt(systemPrompt)
                if (baseUrl != null) preferencesManager?.saveCustomOpenAIApiUrl(baseUrl)

                Toast.makeText(this, "AI Configuration Saved", Toast.LENGTH_SHORT).show()
                updateCardExpansionState(REPLY_METHOD_BYOK)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_custom_reply_editor)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Setup back button handler for unsaved selection changes
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Check if selection has changed
                if (hasSelectionChanged()) {
                    showUnsavedChangesDialog()
                } else {
                    // Default behavior - remove callback and trigger back
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        ViewModelProvider(this)[SwipeToKillAppDetectViewModel::class.java]

        customRepliesData = CustomRepliesData.getInstance(this)
        preferencesManager = PreferencesManager.getPreferencesInstance(this)
        
        preferencesManager?.let {
            subscriptionManager = com.parishod.watomatic.model.subscription.SubscriptionManagerImpl(this, it)
        }

        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        prefs.registerOnSharedPreferenceChangeListener(this)

        initializeViews()
        // Refresh subscription status after views are initialized so callbacks can update UI safely
        refreshSubscriptionStatusIfNeeded()
        setupClickListeners()
        handleDeepLink()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.custom_reply_editor_scroll_view)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        // Restore the saved reply method and update UI
        restoreSelectedReplyMethod()
    }

    override fun onDestroy() {
        super.onDestroy()
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        prefs.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "pref_remaining_atoms") {
            // Update UI dynamically when remaining atoms change
            runOnUiThread {
                updateCardExpansionState(selectedReplyMethod)
            }
        }
    }

    /**
     * Refresh subscription status from backend if the last check was more than 30 minutes ago
     */
    private fun refreshSubscriptionStatusIfNeeded() {
        val lastChecked = preferencesManager?.getSubscriptionStatusLastChecked() ?: 0
        val now = System.currentTimeMillis()

        if (now - lastChecked > SUBSCRIPTION_STATUS_REFRESH_INTERVAL) {
            lifecycleScope.launch {
                try {
                    subscriptionManager?.refreshSubscriptionStatus()
                    preferencesManager?.setSubscriptionStatusLastChecked(now)
                } catch (e: Exception) {
                    // If refresh fails, continue with cached status
                    android.util.Log.e("CustomReplyEditor", "Failed to refresh subscription status", e)
                }
            }
        }
    }

    private fun initializeViews() {
        autoReplyText = findViewById(R.id.autoReplyTextInputEditText)
        saveAutoReplyTextBtn = findViewById(R.id.saveCustomReplyBtn)
        watoMessageLinkBtn = findViewById(R.id.tip_wato_message)
        manualRepliesCard = findViewById(R.id.manual_replies_card)
        manualSettingsIcon = findViewById(R.id.manual_settings_icon)
        manualCheckIcon = findViewById(R.id.manual_check_icon)
        automaticAiProviderCard = findViewById(R.id.automatic_ai_provider_card)
        btnAtomaticAiEdit = findViewById(R.id.btn_automatic_ai_edit)
        otherAiProviderCard = findViewById(R.id.other_ai_provider_card)
        btnOtherAiEdit = findViewById(R.id.btn_other_ai_edit)
        otherAiCheckIcon = findViewById(R.id.other_ai_check_icon)

        // Expanded content views
        automaticAiExpandedContent = findViewById(R.id.automatic_ai_expanded_content)
        automaticAiCheckIcon = findViewById(R.id.automatic_ai_check_icon)
        automaticAiSettingsIcon = findViewById(R.id.automatic_ai_settings_icon)
        automaticAiStatusIcon = findViewById(R.id.automatic_ai_status_icon)
        automaticAiStatusText = findViewById(R.id.automatic_ai_status_text)
        automaticAiNotSubscribedSection = findViewById(R.id.automatic_ai_not_subscribed_section)
        automaticAiSubscribedSection = findViewById(R.id.automatic_ai_subscribed_section)
        btnUnlockSubscription = findViewById(R.id.btn_unlock_subscription)
        btnUpgradePlan = findViewById(R.id.btn_upgrade_plan)
        subscriptionRenewalDate = findViewById(R.id.subscription_renewal_date)
        subscriptionPlanName = findViewById(R.id.subscription_plan_name)
        automaticAiRemainingReplies = findViewById(R.id.automatic_ai_remaining_replies)
        automaticAiTag = findViewById(R.id.automatic_ai_tag)
        otherAiExpandedContent = findViewById(R.id.other_ai_expanded_content)
    }

    private fun handleDeepLink() {
        val intent = intent
        val data = intent.data

        autoReplyText?.setText(
            if ((data != null))
                data.getQueryParameter("message")
            else
                customRepliesData?.get() ?: ""
        )
    }

    private fun setupClickListeners() {
        saveAutoReplyTextBtn?.setOnClickListener {
            handleSaveClick()
        }

        watoMessageLinkBtn?.setOnClickListener {
            val url = getString(R.string.watomatic_wato_message_url)
            startActivity(
                Intent(Intent.ACTION_VIEW).setData(Uri.parse(url))
            )
        }

        // Manual Replies Card - Select this reply method
        manualRepliesCard?.setOnClickListener {
            selectReplyMethod(REPLY_METHOD_MANUAL)
        }

        // Manual Settings Icon - Edit manual reply
        manualSettingsIcon?.setOnClickListener {
            showEditAutoReplyDialog()
        }

        // Automatic AI Card - Select this reply method
        automaticAiProviderCard?.setOnClickListener {
            selectReplyMethod(REPLY_METHOD_AUTOMATIC_AI)
        }

        // Automatic AI Settings Icon - Navigate to subscription/login
        automaticAiSettingsIcon?.setOnClickListener {
            val isProUser = subscriptionManager?.isProUser() ?: false
            handleAutomaticAiManageClick(mode = if (isProUser) SubscriptionMode.MANAGE else null)
        }

        // Automatic AI Manage button - Navigate to subscription in MANAGE mode for subscribed users
        btnAtomaticAiEdit?.setOnClickListener {
            val isProUser = subscriptionManager?.isProUser() ?: false
            handleAutomaticAiManageClick(mode = if (isProUser) SubscriptionMode.MANAGE else null)
        }

        // Unlock Subscription button - Non-subscribed user → show plans UI (no explicit mode)
        btnUnlockSubscription?.setOnClickListener {
            handleAutomaticAiManageClick(mode = null)
        }

        // Upgrade Plan button - FREE plan user → show plans UI in UPGRADE mode
        btnUpgradePlan?.setOnClickListener {
            handleAutomaticAiManageClick(mode = SubscriptionMode.UPGRADE)
        }

        // Other AI Card - Select BYOK method
        otherAiProviderCard?.setOnClickListener {
            selectReplyMethod(REPLY_METHOD_BYOK)
        }

        // Other AI Settings Icon - Navigate to configuration
        btnOtherAiEdit?.setOnClickListener {
            val intent = Intent(this, OtherAiConfigurationActivity::class.java)
            otherAiConfigLauncher.launch(intent)
        }
    }

    /**
     * Centralized method to select a reply method and update UI
     */
    private fun selectReplyMethod(method: String) {
        Log.d("CustomReplyEditor", "selectReplyMethod reply method: $method")
        selectedReplyMethod = method

        // Update UI to reflect the selection
        updateCardExpansionState(method)
    }

    private fun getSelectedReplyMethod(): String{
        if(preferencesManager?.isByokRepliesEnabled == true){
            return REPLY_METHOD_BYOK
        }else if(preferencesManager?.isAutomaticAiRepliesEnabled == true){
            return REPLY_METHOD_AUTOMATIC_AI
        }
        return REPLY_METHOD_MANUAL
    }

    /**
     * Restore the previously selected reply method from preferences
     * For existing users upgrading, infer the method from current settings
     */
    @Suppress("DEPRECATION", "deprecation")
    private fun restoreSelectedReplyMethod() {
        var savedMethod = getSelectedReplyMethod()
        Log.d("CustomReplyEditor", "Restoring selected reply method: $savedMethod")

        // Save initial selection state for unsaved changes detection
        initialSelectedReplyMethod = savedMethod

        // Update UI
        selectReplyMethod(savedMethod)
    }

    /**
     * Validate and save the selected reply method configuration
     */
    private fun handleSaveClick() {
        val selectedMethod = selectedReplyMethod//preferencesManager?.getSelectedReplyMethod() ?: REPLY_METHOD_MANUAL

        // Validate the selected method is properly configured
        when (selectedMethod) {
            REPLY_METHOD_MANUAL -> {
                // Validate manual reply text
                val replyText = autoReplyText?.text
                if (!CustomRepliesData.isValidCustomReply(replyText)) {
                    Toast.makeText(
                        this,
                        "Please enter a valid auto reply message",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
                // Save manual reply
                customRepliesData?.set(replyText)
                preferencesManager?.setEnableAutomaticAiReplies(false)
                preferencesManager?.setEnableByokReplies(false)
            }
            REPLY_METHOD_AUTOMATIC_AI -> {
                // Validate subscription for automatic AI
                val isProUser = subscriptionManager?.isProUser() ?: false
                if (!isProUser) {
                    Toast.makeText(
                        this,
                        "Please subscribe to use Automatic AI replies",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }
                preferencesManager?.setEnableAutomaticAiReplies(true)
                preferencesManager?.setEnableByokReplies(false)
            }
            REPLY_METHOD_BYOK -> {
                // Validate BYOK configuration
                val apiKey = preferencesManager?.openAIApiKey
                if (apiKey.isNullOrEmpty() || apiKey == "PENDING_CONFIGURATION") {
                    Toast.makeText(
                        this,
                        "Please configure your AI provider settings",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }
                preferencesManager?.setEnableAutomaticAiReplies(false)
                preferencesManager?.setEnableByokReplies(true)
            }
        }

        // Save successful, update initial selection to prevent unsaved changes dialog
        initialSelectedReplyMethod = selectedReplyMethod

        // Navigate back
        onNavigateUp()
    }

    private fun updateCardExpansionState(selectedMethod: String) {
        Log.d("CustomReplyEditor", "updateCardExpansionState reply method: $selectedMethod")
//        val selectedMethod = preferencesManager?.getSelectedReplyMethod() ?: REPLY_METHOD_MANUAL
        val isProUser = subscriptionManager?.isProUser() ?: false
        val hasApiKey = !preferencesManager?.openAIApiKey.isNullOrEmpty() &&
                        preferencesManager?.openAIApiKey != "PENDING_CONFIGURATION"

        // Stroke widths for selection state
        val selectedStrokeWidth = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            2f,
            resources.displayMetrics
        ).toInt()
        val unselectedStrokeWidth = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            1f,
            resources.displayMetrics
        ).toInt()

        // Get colors for stroke
        val primaryColor = getThemeColor(androidx.appcompat.R.attr.colorPrimary)
        val defaultStrokeColor = 0xFF38383A.toInt() // Gray color for unselected

        // Determine which card is selected based on saved preference
        val isManualSelected = selectedMethod == REPLY_METHOD_MANUAL
        val isAutomaticAiSelected = selectedMethod == REPLY_METHOD_AUTOMATIC_AI
        val isByokSelected = selectedMethod == REPLY_METHOD_BYOK

        // Update Manual Replies Card
        manualRepliesCard?.apply {
            strokeWidth = if (isManualSelected) selectedStrokeWidth else unselectedStrokeWidth
            strokeColor = if (isManualSelected) primaryColor else defaultStrokeColor
        }
        // Show/hide manual check icon
        manualCheckIcon?.visibility = if (isManualSelected) android.view.View.VISIBLE else android.view.View.GONE

        // Show/hide manual expanded content (text input)
        if (isManualSelected) {
            autoReplyText?.visibility = android.view.View.VISIBLE
            autoReplyText?.setText(customRepliesData?.get())
        } else {
            autoReplyText?.visibility = android.view.View.GONE
        }

        // Update Automatic AI Card
        automaticAiProviderCard?.apply {
            strokeWidth = if (isAutomaticAiSelected) selectedStrokeWidth else unselectedStrokeWidth
            strokeColor = if (isAutomaticAiSelected) primaryColor else defaultStrokeColor
        }

        // Settings icon is always visible
        automaticAiSettingsIcon?.visibility = android.view.View.VISIBLE

        if (isAutomaticAiSelected) {
            // Show expanded content for Automatic AI
            automaticAiExpandedContent?.visibility = android.view.View.VISIBLE
            automaticAiCheckIcon?.visibility = android.view.View.VISIBLE

            // Update badge and status based on subscription
            if (isProUser) {
                // Configured state - user has subscription

                // Get subscription details
                val productName = preferencesManager?.subscriptionProductName
                val planType = preferencesManager?.subscriptionPlanType
                val isAutoRenewing = preferencesManager?.isSubscriptionAutoRenewing ?: false

                // Get plan-specific UI configuration based on auto-renewal status
                val planConfig = getPlanUIConfig(isAutoRenewing)

                // Apply badge configuration
                automaticAiTag?.text = planConfig.badgeText
                automaticAiTag?.setBackgroundResource(planConfig.badgeBackgroundRes)

                automaticAiStatusIcon?.setImageResource(R.drawable.ic_task_alt)
                automaticAiStatusIcon?.setColorFilter(0xFF34C759.toInt())

                automaticAiStatusText?.text = "Status: Configured"
                automaticAiStatusText?.setTextColor(0xFF34C759.toInt())
                automaticAiStatusText?.visibility = android.view.View.VISIBLE

                automaticAiNotSubscribedSection?.visibility = android.view.View.GONE
                automaticAiSubscribedSection?.visibility = android.view.View.VISIBLE

                // Show "Upgrade Plan" button for any plan that is NOT the highest tier (Pro)
                val productId = preferencesManager?.subscriptionProductId ?: ""
                val isProPlan = productId.contains("pro", ignoreCase = true)
                btnUpgradePlan?.visibility = if (!isProPlan) android.view.View.VISIBLE else android.view.View.GONE

                // Debug logging
                android.util.Log.d("CustomReplyEditor", "Product Name: '$productName'")
                android.util.Log.d("CustomReplyEditor", "Plan Type: '$planType'")
                android.util.Log.d("CustomReplyEditor", "Auto-Renewing: $isAutoRenewing")
                android.util.Log.d("CustomReplyEditor", "Badge: '${planConfig.badgeText}', Date Label: '${planConfig.dateLabel}'")

                val displayName = if (!productName.isNullOrEmpty()) {
                    // Use actual product name from Google Play
                    android.util.Log.d("CustomReplyEditor", "Using product name: $productName")
                    "$productName - Active"
                } else {
                    // Fallback: try to use plan type if available
                    val fallbackName = when {
                        !planType.isNullOrEmpty() -> {
                            android.util.Log.d("CustomReplyEditor", "Using plan type fallback: $planType")
                            "$planType - Active"
                        }
                        else -> {
                            android.util.Log.d("CustomReplyEditor", "Using default fallback: Pro Plan")
                            "Pro Plan - Active"
                        }
                    }
                    fallbackName
                }

                android.util.Log.d("CustomReplyEditor", "Setting plan name to: '$displayName'")
                subscriptionPlanName?.text = displayName

                // Format renewal/expiry date with plan-specific label
                val expiryTime = preferencesManager?.subscriptionExpiryTime ?: 0
                if (expiryTime > 0) {
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    val dateStr = dateFormat.format(Date(expiryTime))
                    subscriptionRenewalDate?.text = "${planConfig.dateLabel} $dateStr"
                } else {
                    subscriptionRenewalDate?.text = "Active subscription"
                }

                // Show remaining atoms
                val remainingAtoms = preferencesManager?.remainingAtoms ?: -1
                automaticAiRemainingReplies?.visibility = android.view.View.VISIBLE
                if (remainingAtoms >= 0) {
                    automaticAiRemainingReplies?.text = "Remaining Replies: $remainingAtoms"
                    when {
                        remainingAtoms == 0 -> {
                            automaticAiRemainingReplies?.setTextColor(0xFFFF453A.toInt()) // Red
                        }
                        remainingAtoms < 10 -> {
                            automaticAiRemainingReplies?.setTextColor(0xFFFF9F0A.toInt()) // Orange
                        }
                        else -> {
                            // Default text color
//                            automaticAiRemainingReplies?.setTextColor(getThemeColor(android.R.attr.textColorPrimary))
                        }
                    }
                } else {
                    automaticAiRemainingReplies?.text = "Remaining Atoms: N/A"
//                    automaticAiRemainingReplies?.setTextColor(getThemeColor(android.R.attr.textColorPrimary))
                }
            } else {
                // Not configured state - user needs subscription
                automaticAiTag?.text = getString(R.string.badge_free)
                automaticAiTag?.setBackgroundResource(R.drawable.bg_badge_gray)

                automaticAiStatusIcon?.setImageResource(R.drawable.ic_error_outline)
                automaticAiStatusIcon?.setColorFilter(0xFFFF453A.toInt())

                automaticAiStatusText?.text = "Status: Not Configured"
                automaticAiStatusText?.setTextColor(0xFFFF453A.toInt()) // Red text for not configured
                automaticAiStatusText?.visibility = android.view.View.VISIBLE

                automaticAiNotSubscribedSection?.visibility = android.view.View.VISIBLE
                automaticAiSubscribedSection?.visibility = android.view.View.GONE
                
                // Show remaining atoms even when not subscribed, but it might be --
                val remainingAtoms = preferencesManager?.remainingAtoms ?: -1
                automaticAiRemainingReplies?.visibility = android.view.View.GONE
                /*if (remainingAtoms >= 0) {
                    automaticAiRemainingReplies?.text = "Remaining Replies: $remainingAtoms"
                    when {
                        remainingAtoms == 0 -> {
                            automaticAiRemainingReplies?.setTextColor(0xFFFF453A.toInt()) // Red
                        }
                        remainingAtoms < 10 -> {
                            automaticAiRemainingReplies?.setTextColor(0xFFFF9F0A.toInt()) // Orange
                        }
                        else -> {
                            // Default text color
                            automaticAiRemainingReplies?.setTextColor(getThemeColor(android.R.attr.textColorSecondary))
                        }
                    }
                } else {
                    automaticAiRemainingReplies?.text = "Remaining Atoms: --"
                    automaticAiRemainingReplies?.setTextColor(getThemeColor(android.R.attr.textColorSecondary))
                }*/
            }
        } else {
            // Collapse Automatic AI card when not selected
            automaticAiExpandedContent?.visibility = android.view.View.GONE
            automaticAiCheckIcon?.visibility = android.view.View.GONE
        }

        // Update Bring Your Own Key Card
        otherAiProviderCard?.apply {
            strokeWidth = if (isByokSelected) selectedStrokeWidth else unselectedStrokeWidth
            strokeColor = if (isByokSelected) primaryColor else defaultStrokeColor
        }
        // Show/hide BYOK check icon
        otherAiCheckIcon?.visibility = if (isByokSelected) android.view.View.VISIBLE else android.view.View.GONE

        if (isByokSelected) {
            // Show expanded content for BYOK
            otherAiExpandedContent?.visibility = android.view.View.VISIBLE

            // Update status text based on configuration
            if (hasApiKey) {
                // Configured - API key is set
                findViewById<TextView>(R.id.other_ai_status_text)?.apply {
                    text = "Status: Configured"
                    setTextColor(0xFF34C759.toInt()) // Green for configured
                }
            } else {
                // Not configured - API key not set or pending
                findViewById<TextView>(R.id.other_ai_status_text)?.apply {
                    text = "Status: Not Configured"
                    setTextColor(0xFFFF453A.toInt()) // Red for not configured
                }
            }
        } else {
            // Collapse BYOK card when not selected
            otherAiExpandedContent?.visibility = android.view.View.GONE
        }
    }

    private fun getThemeColor(attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    /**
     * Navigate to SubscriptionInfoActivity with the given mode.
     *
     * @param mode null  → default behaviour (non-subscribed → plans list)
     *             MANAGE → show active subscription details
     *             UPGRADE → show plans list with FREE marked as current
     */
    private fun handleAutomaticAiManageClick(mode: SubscriptionMode? = null) {
        val isLoggedIn = preferencesManager?.isLoggedIn ?: false

        if (!isLoggedIn) {
            // User is not logged in, navigate to Login screen
            FlavorNavigator.startLogin(this)
        } else {
            // User is logged in, navigate to Subscription Info screen with mode
            val intent = Intent(this, SubscriptionInfoActivity::class.java)
            mode?.let {
                intent.putExtra(SubscriptionMode.EXTRA_KEY, it.name)
            }
            startActivity(intent)
        }
    }


    private fun showEditAutoReplyDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_auto_reply, null)
        val editText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.dialog_auto_reply_edit_text)

        // Set current text
        editText?.setText(customRepliesData?.get())

        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.mainAutoReplyLabel))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val newText = editText?.text
                if (CustomRepliesData.isValidCustomReply(newText)) {
                    customRepliesData?.set(newText)
                    autoReplyText?.setText(newText)
                    Toast.makeText(this, "Auto reply updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Invalid auto reply text", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(android.R.string.cancel), null)
            .create()

        dialog.show()

        // Request focus and show keyboard
        editText?.requestFocus()
        editText?.postDelayed({
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(editText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }, 100)
    }

    /**
     * Check if the current selection has changed from the initial selection
     */
    private fun hasSelectionChanged(): Boolean {
        val changed = selectedReplyMethod != initialSelectedReplyMethod
        Log.d("CustomReplyEditor", "hasSelectionChanged: $changed (current: $selectedReplyMethod, initial: $initialSelectedReplyMethod)")
        return changed
    }

    /**
     * Show a dialog to confirm discarding unsaved selection changes
     */
    private fun showUnsavedChangesDialog() {
        UnsavedChangesDialog.show(
            context = this,
            onDiscard = {
                // Reset selection to initial state and navigate back
                finish()
            }
        )
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
