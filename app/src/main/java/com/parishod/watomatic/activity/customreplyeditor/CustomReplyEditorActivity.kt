package com.parishod.watomatic.activity.customreplyeditor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.appbar.MaterialToolbar
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
    private var watoMessageLinkBtn: TextView? = null
    private var enableAIRepliesCheckbox: CheckBox? = null
    private var automaticAiProviderCard: MaterialCardView? = null
    private var otherAiProviderCard: MaterialCardView? = null

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

        autoReplyText = findViewById(R.id.autoReplyTextInputEditText)
        saveAutoReplyTextBtn = findViewById(R.id.saveCustomReplyBtn)
        watoMessageLinkBtn = findViewById(R.id.tip_wato_message)
        enableAIRepliesCheckbox = findViewById(R.id.enable_ai_replies_checkbox)
        automaticAiProviderCard = findViewById(R.id.automatic_ai_provider_card)
        otherAiProviderCard = findViewById(R.id.other_ai_provider_card)

        val intent = intent
        val data = intent.data

        autoReplyText?.setText(
            if ((data != null))
                data.getQueryParameter("message")
            else
                customRepliesData?.get()
        )

        autoReplyText?.requestFocus()
        autoReplyText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(editable: Editable) {
                // Disable save button if text does not satisfy requirements
                saveAutoReplyTextBtn?.setEnabled(CustomRepliesData.isValidCustomReply(editable))
            }
        })

        saveAutoReplyTextBtn?.setOnClickListener {
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

        // Toggle AI enable/disable
        enableAIRepliesCheckbox?.setOnCheckedChangeListener { _, ischecked ->
            preferencesManager?.setEnableOpenAIReplies(ischecked)
            updateAIState()
        }

        // Initial UI state
        updateAIState()

        // Set up click listener for Automatic AI Provider card
        automaticAiProviderCard?.setOnClickListener {
            handleAutomaticAiProviderClick()
        }

        otherAiProviderCard?.setOnClickListener {
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
        updateAIState()
    }

    private fun updateAIState() {
        val isAIEnabled = preferencesManager?.isOpenAIRepliesEnabled ?: false
        
        // Update Checkbox state if needed (e.g. onResume)
        if (enableAIRepliesCheckbox?.isChecked != isAIEnabled) {
            enableAIRepliesCheckbox?.isChecked = isAIEnabled
        }
        
        autoReplyText?.isEnabled = !isAIEnabled

        // Handle Cards Visual State
        val alpha = if (isAIEnabled) 1.0f else 0.5f
        automaticAiProviderCard?.alpha = alpha
        otherAiProviderCard?.alpha = alpha
        
        automaticAiProviderCard?.isEnabled = isAIEnabled
        otherAiProviderCard?.isEnabled = isAIEnabled

        // Handle Selection State
        // Heuristic: If API Key is present, Other AI is selected. Otherwise Automatic AI.
        val hasApiKey = !preferencesManager?.openAIApiKey.isNullOrEmpty()
        val isOtherSelected = hasApiKey
        val isAutomaticSelected = !hasApiKey

        val selectedStrokeWidth = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 
            2f, 
            resources.displayMetrics
        ).toInt()
        
        val unselectedStrokeWidth = 0
        val selectedStrokeColor = getThemeColor(androidx.appcompat.R.attr.colorPrimary)

        automaticAiProviderCard?.strokeWidth = if (isAutomaticSelected && isAIEnabled) selectedStrokeWidth else unselectedStrokeWidth
        automaticAiProviderCard?.strokeColor = selectedStrokeColor

        otherAiProviderCard?.strokeWidth = if (isOtherSelected && isAIEnabled) selectedStrokeWidth else unselectedStrokeWidth
        otherAiProviderCard?.strokeColor = selectedStrokeColor
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

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
