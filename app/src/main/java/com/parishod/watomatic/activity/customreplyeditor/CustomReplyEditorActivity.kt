package com.parishod.watomatic.activity.customreplyeditor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.parishod.watomatic.R
import com.parishod.watomatic.activity.BaseActivity
import com.parishod.watomatic.activity.subscription.SubscriptionInfoActivity
import com.parishod.watomatic.flavor.FlavorNavigator
import com.parishod.watomatic.model.CustomRepliesData
import com.parishod.watomatic.model.preferences.PreferencesManager
import com.parishod.watomatic.viewmodel.SwipeToKillAppDetectViewModel

class CustomReplyEditorActivity : BaseActivity() {
    private var autoReplyText: TextInputEditText? = null
    private var saveAutoReplyTextBtn: Button? = null
    private var customRepliesData: CustomRepliesData? = null
    private var preferencesManager: PreferencesManager? = null
    private var subscriptionManager: com.parishod.watomatic.model.subscription.SubscriptionManager? = null
    private var watoMessageLinkBtn: TextView? = null
    private var manualRepliesCard: MaterialCardView? = null
    private var manualSettingsIcon: android.widget.ImageView? = null
    private var automaticAiProviderCard: MaterialCardView? = null
    private var btnAtomaticAiEdit: TextView? = null
    private var otherAiProviderCard: MaterialCardView? = null
    private var btnOtherAiEdit: android.widget.ImageView? = null

    // New UI elements for expanded Automatic AI card
    private var automaticAiExpandedContent: android.view.View? = null
    private var automaticAiCheckIcon: android.widget.ImageView? = null
    private var automaticAiSettingsIcon: android.widget.ImageView? = null
    private var automaticAiStatusIcon: android.widget.ImageView? = null
    private var automaticAiStatusText: TextView? = null
    private var automaticAiNotSubscribedSection: android.view.View? = null
    private var automaticAiSubscribedSection: android.view.View? = null
    private var btnUnlockSubscription: MaterialButton? = null
    private var subscriptionRenewalDate: TextView? = null
    private var automaticAiTag: TextView? = null

    // BYOK card expanded content
    private var otherAiExpandedContent: android.view.View? = null

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
                updateCardExpansionState()
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

        ViewModelProvider(this)[SwipeToKillAppDetectViewModel::class.java]

        customRepliesData = CustomRepliesData.getInstance(this)
        preferencesManager = PreferencesManager.getPreferencesInstance(this)
        
        preferencesManager?.let {
            subscriptionManager = com.parishod.watomatic.model.subscription.SubscriptionManagerImpl(this, it)
        }

        autoReplyText = findViewById(R.id.autoReplyTextInputEditText)
        saveAutoReplyTextBtn = findViewById(R.id.saveCustomReplyBtn)
        watoMessageLinkBtn = findViewById(R.id.tip_wato_message)
        manualRepliesCard = findViewById(R.id.manual_replies_card)
        manualSettingsIcon = findViewById(R.id.manual_settings_icon)
        automaticAiProviderCard = findViewById(R.id.automatic_ai_provider_card)
        btnAtomaticAiEdit = findViewById(R.id.btn_automatic_ai_edit)
        otherAiProviderCard = findViewById(R.id.other_ai_provider_card)
        btnOtherAiEdit = findViewById(R.id.btn_other_ai_edit)

        // New UI elements
        automaticAiExpandedContent = findViewById(R.id.automatic_ai_expanded_content)
        automaticAiCheckIcon = findViewById(R.id.automatic_ai_check_icon)
        automaticAiSettingsIcon = findViewById(R.id.automatic_ai_settings_icon)
        automaticAiStatusIcon = findViewById(R.id.automatic_ai_status_icon)
        automaticAiStatusText = findViewById(R.id.automatic_ai_status_text)
        automaticAiNotSubscribedSection = findViewById(R.id.automatic_ai_not_subscribed_section)
        automaticAiSubscribedSection = findViewById(R.id.automatic_ai_subscribed_section)
        btnUnlockSubscription = findViewById(R.id.btn_unlock_subscription)
        subscriptionRenewalDate = findViewById(R.id.subscription_renewal_date)
        automaticAiTag = findViewById(R.id.automatic_ai_tag)
        otherAiExpandedContent = findViewById(R.id.other_ai_expanded_content)

        val intent = intent
        val data = intent.data

        autoReplyText?.setText(
            if ((data != null))
                data.getQueryParameter("message")
            else
                customRepliesData?.get()
        )

        saveAutoReplyTextBtn?.setOnClickListener {
            val isAIEnabled = preferencesManager?.isOpenAIRepliesEnabled ?: false

            if (isAIEnabled && (subscriptionManager?.isProUser() == true || !preferencesManager?.openAIApiKey.isNullOrEmpty())) {
                preferencesManager?.setEnableOpenAIReplies(true)
            } else if (isAIEnabled) {
                Toast.makeText(
                    this,
                    getString(R.string.configure_ai_llm_s_info),
                    Toast.LENGTH_LONG
                ).show()
                preferencesManager?.setEnableOpenAIReplies(false)
                return@setOnClickListener
            } else {
                preferencesManager?.setEnableOpenAIReplies(false)
            }
            val setString = customRepliesData?.set(autoReplyText?.getText())
            if (setString != null) {
                this.onNavigateUp()
            }
        }

        watoMessageLinkBtn?.setOnClickListener {
            val url = getString(R.string.watomatic_wato_message_url)
            startActivity(
                Intent(Intent.ACTION_VIEW).setData(Uri.parse(url))
            )
        }

        // Manual Replies Card - Toggle to disable AI (expand/collapse only)
        manualRepliesCard?.setOnClickListener {
            preferencesManager?.setEnableOpenAIReplies(false)
            updateCardExpansionState()
        }

        // Manual Settings Icon - Navigate to edit dialog
        manualSettingsIcon?.setOnClickListener {
            showEditAutoReplyDialog()
        }

        // Initial UI state - expand the active card on launch based on saved preferences
        updateCardExpansionState()

        // Automatic AI Card - Toggle expansion and enable AI mode
        automaticAiProviderCard?.setOnClickListener {
            // Enable AI mode for Automatic AI (server-based)
            preferencesManager?.setEnableOpenAIReplies(true)
            // Clear API key to switch to Automatic AI (not BYOK)
            if (!preferencesManager?.openAIApiKey.isNullOrEmpty()) {
                preferencesManager?.saveOpenAIApiKey("")
            }
            updateCardExpansionState()
        }

        // Automatic AI Settings Icon - Navigate to subscription/login (always visible)
        automaticAiSettingsIcon?.setOnClickListener {
            handleAutomaticAiManageClick()
        }

        // Automatic AI Manage button - Navigate to subscription/login
        btnAtomaticAiEdit?.setOnClickListener {
            handleAutomaticAiManageClick()
        }

        // Unlock Subscription button - Navigate to subscription/login
        btnUnlockSubscription?.setOnClickListener {
            handleAutomaticAiManageClick()
        }

        // Other AI Card - Toggle to enable AI with BYOK mode
        otherAiProviderCard?.setOnClickListener {
            // Enable AI mode and mark as using BYOK
            preferencesManager?.setEnableOpenAIReplies(true)

            // If no API key is configured, set a placeholder to ensure BYOK card is selected
            // The user will configure the actual API key via settings icon
            if (preferencesManager?.openAIApiKey.isNullOrEmpty()) {
                preferencesManager?.saveOpenAIApiKey("PENDING_CONFIGURATION")
            }

            updateCardExpansionState()
        }

        // Other AI Settings Icon - Navigate to configuration
        btnOtherAiEdit?.setOnClickListener {
            // First enable AI mode
            preferencesManager?.setEnableOpenAIReplies(true)

            // Open configuration
            val intent = Intent(this, OtherAiConfigurationActivity::class.java)
            otherAiConfigLauncher.launch(intent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.custom_reply_editor_scroll_view)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        updateCardExpansionState()
    }

    private fun updateCardExpansionState() {
        val isAIEnabled = preferencesManager?.isOpenAIRepliesEnabled ?: false
        val isProUser = subscriptionManager?.isProUser() ?: false
        val hasApiKey = !preferencesManager?.openAIApiKey.isNullOrEmpty() &&
                        preferencesManager?.openAIApiKey != "PENDING_CONFIGURATION"
        val hasManualReply = !customRepliesData?.get().isNullOrEmpty()

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

        // Determine which card should be selected/expanded
        val isManualSelected = !isAIEnabled
        val isAutomaticAiSelected = isAIEnabled && !hasApiKey
        val isByokSelected = isAIEnabled && hasApiKey

        // Update Manual Replies Card
        manualRepliesCard?.strokeWidth = if (isManualSelected) selectedStrokeWidth else unselectedStrokeWidth
        // Manual card can be configured via edit dialog

        // Update Automatic AI Card
        automaticAiProviderCard?.strokeWidth = if (isAutomaticAiSelected) selectedStrokeWidth else unselectedStrokeWidth

        // Settings icon is always visible
        automaticAiSettingsIcon?.visibility = android.view.View.VISIBLE

        if (isAutomaticAiSelected) {
            // Show expanded content for Automatic AI
            automaticAiExpandedContent?.visibility = android.view.View.VISIBLE
            automaticAiCheckIcon?.visibility = android.view.View.VISIBLE

            // Update badge and status based on subscription
            if (isProUser) {
                // Configured state (4.html) - user has subscription
                automaticAiTag?.text = "PRO"
                automaticAiTag?.setBackgroundResource(R.drawable.bg_badge_pro)

                automaticAiStatusIcon?.setImageResource(R.drawable.ic_task_alt)
                automaticAiStatusIcon?.setColorFilter(0xFF34C759.toInt())

                automaticAiStatusText?.text = "Status: Configured"
                automaticAiStatusText?.setTextColor(0xFF34C759.toInt())
                automaticAiStatusText?.visibility = android.view.View.VISIBLE

                automaticAiNotSubscribedSection?.visibility = android.view.View.GONE
                automaticAiSubscribedSection?.visibility = android.view.View.VISIBLE

                subscriptionRenewalDate?.text = "Renews on Oct 12, 2024"
            } else {
                // Not configured state (2.html) - user needs subscription
                automaticAiTag?.text = "FREE"
                automaticAiTag?.setBackgroundResource(R.drawable.bg_badge_gray)

                automaticAiStatusIcon?.setImageResource(R.drawable.ic_error_outline)
                automaticAiStatusIcon?.setColorFilter(0xFFFF453A.toInt())

                automaticAiStatusText?.text = "Status: Not Configured"
                automaticAiStatusText?.setTextColor(0xFFFF453A.toInt()) // Red text for not configured
                automaticAiStatusText?.visibility = android.view.View.VISIBLE

                automaticAiNotSubscribedSection?.visibility = android.view.View.VISIBLE
                automaticAiSubscribedSection?.visibility = android.view.View.GONE
            }
        } else {
            // Collapse Automatic AI card when not selected
            automaticAiExpandedContent?.visibility = android.view.View.GONE
            automaticAiCheckIcon?.visibility = android.view.View.GONE
        }

        // Update Bring Your Own Key Card
        otherAiProviderCard?.strokeWidth = if (isByokSelected) selectedStrokeWidth else unselectedStrokeWidth

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

    private fun handleAutomaticAiManageClick() {
        val isLoggedIn = preferencesManager?.isLoggedIn ?: false

        if (!isLoggedIn) {
            // User is not logged in, navigate to Login screen
            FlavorNavigator.startLogin(this)
        } else {
            // User is logged in, navigate to Subscription Info screen
            val intent = Intent(this, SubscriptionInfoActivity::class.java)
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

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
