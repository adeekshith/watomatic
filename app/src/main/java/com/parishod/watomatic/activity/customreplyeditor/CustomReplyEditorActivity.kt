package com.parishod.watomatic.activity.customreplyeditor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText
import com.parishod.watomatic.R
import com.parishod.watomatic.activity.BaseActivity
import com.parishod.watomatic.model.CustomRepliesData
import com.parishod.watomatic.model.preferences.PreferencesManager
import com.parishod.watomatic.viewmodel.SwipeToKillAppDetectViewModel
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.view.View
import android.widget.TextView
import com.google.android.material.appbar.MaterialToolbar


import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.parishod.watomatic.model.utils.OpenAIHelper
import com.parishod.watomatic.network.model.openai.ModelData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.app.AlertDialog
import android.os.Handler
import android.os.Looper
import com.parishod.watomatic.activity.subscription.SubscriptionInfoActivity
import com.parishod.watomatic.flavor.FlavorNavigator
// Ensure TextInputLayout is imported if not using fully qualified name,
// or use fully qualified name as in the original snippet for layout.
// For consistency with original dialog code, will use fully qualified name for TextInputLayout constructor.

class CustomReplyEditorActivity : BaseActivity() {
    private var autoReplyText: TextInputEditText? = null
    private var saveAutoReplyTextBtn: Button? = null
    private var customRepliesData: CustomRepliesData? = null
    private var preferencesManager: PreferencesManager? = null
    private var watoMessageLinkBtn: Button? = null
    private var enableAIRepliesCheckbox: CheckBox? = null
    private var automaticAiProviderCard: View? = null
    private var otherAiProviderCard: View? = null

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

        // Load initial state
        val isAIEnabled = preferencesManager?.isOpenAIRepliesEnabled ?: false
        autoReplyText?.isEnabled = !isAIEnabled
        enableAIRepliesCheckbox?.isChecked = isAIEnabled
        automaticAiProviderCard?.visibility = if (isAIEnabled) View.VISIBLE else View.GONE
        otherAiProviderCard?.visibility = if (isAIEnabled) View.VISIBLE else View.GONE

        fun updateAICardsVisibility() {
            val isAIEnabled = enableAIRepliesCheckbox?.isChecked ?: false
            automaticAiProviderCard?.visibility = if (isAIEnabled) View.VISIBLE else View.GONE
            otherAiProviderCard?.visibility = if (isAIEnabled) View.VISIBLE else View.GONE
        }

        // Toggle AI enable/disable
        enableAIRepliesCheckbox?.setOnCheckedChangeListener { _, ischecked ->
            preferencesManager?.setEnableOpenAIReplies(ischecked)
            autoReplyText?.isEnabled = !ischecked
            updateAICardsVisibility()
        }

        // Initial UI state
        updateAICardsVisibility()

        // Set up click listener for Automatic AI Provider card
        automaticAiProviderCard?.setOnClickListener {
            handleAutomaticAiProviderClick()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.custom_reply_editor_scroll_view)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
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
