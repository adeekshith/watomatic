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
    private var btnAtomaticAiEdit: MaterialButton? = null
    private var otherAiProviderCard: MaterialCardView? = null
    private var btnOtherAiEdit: MaterialButton? = null

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
                updateAIState()
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

        // Manual Replies Card - Toggle to disable AI
        manualRepliesCard?.setOnClickListener {
            preferencesManager?.setEnableOpenAIReplies(false)
            updateAIState()
        }

        // Manual Settings Icon - Open edit dialog
        manualSettingsIcon?.setOnClickListener {
            showEditAutoReplyDialog()
        }

        // Initial UI state
        updateAIState()

        // Set up click listener for Automatic AI Provider card
        automaticAiProviderCard?.setOnClickListener {
            handleAutomaticAiProviderClick()
        }

        btnAtomaticAiEdit?.setOnClickListener {
            handleAutomaticAiProviderClick()
        }

        otherAiProviderCard?.setOnClickListener {
            handleOtherAiProviderClick()
        }

        btnOtherAiEdit?.setOnClickListener {
            handleOtherAiProviderClick()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.custom_reply_editor_scroll_view)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()

        updateAIState()
    }

    private fun updateAIState() {
        val isAIEnabled = preferencesManager?.isOpenAIRepliesEnabled ?: false

        // Handle Cards Visual State
        val selectedStrokeWidth = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            2f,
            resources.displayMetrics
        ).toInt()

        val unselectedStrokeWidth = 0
        val selectedStrokeColor = getThemeColor(androidx.appcompat.R.attr.colorPrimary)

        // Manual Replies Card - Selected when AI is disabled
        manualRepliesCard?.strokeWidth = if (!isAIEnabled) selectedStrokeWidth else unselectedStrokeWidth
        manualRepliesCard?.strokeColor = selectedStrokeColor

        // Show/hide Active badge on manual card
        val manualActiveBadge = findViewById<TextView>(R.id.manual_active_badge)
        manualActiveBadge?.visibility = if (!isAIEnabled) android.view.View.VISIBLE else android.view.View.GONE

        // All cards remain enabled and fully interactive
        // No alpha or disabled states applied

        // Handle AI provider selection state (only when AI is enabled)
        if (isAIEnabled) {
            val hasApiKey = !preferencesManager?.openAIApiKey.isNullOrEmpty()
            val isOtherSelected = hasApiKey
            val isAutomaticSelected = !hasApiKey

            automaticAiProviderCard?.strokeWidth = if (isAutomaticSelected) selectedStrokeWidth else unselectedStrokeWidth
            automaticAiProviderCard?.strokeColor = selectedStrokeColor

            otherAiProviderCard?.strokeWidth = if (isOtherSelected) selectedStrokeWidth else unselectedStrokeWidth
            otherAiProviderCard?.strokeColor = selectedStrokeColor
        } else {
            // No selection when AI is disabled
            automaticAiProviderCard?.strokeWidth = unselectedStrokeWidth
            otherAiProviderCard?.strokeWidth = unselectedStrokeWidth
        }
    }

    private fun getThemeColor(attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    private fun handleAutomaticAiProviderClick() {
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

    private fun handleOtherAiProviderClick() {
        // Enable AI mode and select "Other AI" provider
        preferencesManager?.setEnableOpenAIReplies(true)

        // Open Other AI configuration screen
        val intent = Intent(this, OtherAiConfigurationActivity::class.java)
        otherAiConfigLauncher.launch(intent)

        // Update UI to reflect AI mode enabled
        updateAIState()
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
