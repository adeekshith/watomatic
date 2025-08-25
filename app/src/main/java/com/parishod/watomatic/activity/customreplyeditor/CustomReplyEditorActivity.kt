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

class CustomReplyEditorActivity : BaseActivity() {
    private var autoReplyText: TextInputEditText? = null
    private var saveAutoReplyTextBtn: Button? = null
    private var customRepliesData: CustomRepliesData? = null
    private var preferencesManager: PreferencesManager? = null
    private var watoMessageLinkBtn: Button? = null
    private var enableAIRepliesCheckbox: CheckBox? = null
    private var aiProviderCard: View? = null
    private var aiProviderValue: TextView? = null
    private var providerOptions: Array<String>? = null
    private var aiApiKeyCard: View? = null
    private var aiApiKeyEditText: TextInputEditText? = null
    private var aiModelCard: View? = null
    private var aiModelSelectedValue: TextView? = null
    private var modelList: List<ModelData> = emptyList()
    private var isModelLoading = false
    private var modelLoadError: String? = null
    private var selectedModelId: String? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_custom_reply_editor)

        setTitle(R.string.mainAutoReplyLabel)

        ViewModelProvider(this)[SwipeToKillAppDetectViewModel::class.java]

        customRepliesData = CustomRepliesData.getInstance(this)
        preferencesManager = PreferencesManager.getPreferencesInstance(this)

        autoReplyText = findViewById(R.id.autoReplyTextInputEditText)
        saveAutoReplyTextBtn = findViewById(R.id.saveCustomReplyBtn)
        watoMessageLinkBtn = findViewById(R.id.tip_wato_message)
        enableAIRepliesCheckbox = findViewById(R.id.enable_ai_replies_checkbox)
        aiProviderCard = findViewById(R.id.ai_provider_card)
        aiProviderValue = findViewById(R.id.ai_provider_value)
        aiApiKeyCard = findViewById(R.id.ai_api_key_card)
        aiApiKeyEditText = findViewById(R.id.ai_api_key_edittext)
        aiModelCard = findViewById(R.id.ai_model_card)
        aiModelSelectedValue = findViewById(R.id.ai_model_selected_value)

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

        providerOptions = resources.getStringArray(R.array.openai_api_source_entries)
        val providerValues = resources.getStringArray(R.array.openai_api_source_values)

        // Load initial state
        val isAIEnabled = preferencesManager?.isOpenAIRepliesEnabled() ?: false
        enableAIRepliesCheckbox?.isChecked = isAIEnabled
        aiProviderCard?.visibility = if (isAIEnabled) View.VISIBLE else View.GONE

        // Load provider selection
        var providerPref = preferencesManager?.getOpenApiSource() ?: "openai"
        var providerIndex = providerValues.indexOf(providerPref)
        if (providerIndex < 0) providerIndex = 0
        aiProviderValue?.text = providerOptions?.get(providerIndex) ?: "OpenAI"

        fun updateAICardsVisibility() {
            val isAIEnabled = enableAIRepliesCheckbox?.isChecked ?: false
            val providerIsAI = providerPref == "openai" || providerPref == "custom"
            aiProviderCard?.visibility = if (isAIEnabled) View.VISIBLE else View.GONE
            aiApiKeyCard?.visibility = if (isAIEnabled && providerIsAI) View.VISIBLE else View.GONE
            aiModelCard?.visibility =
                if (isAIEnabled && providerIsAI && !aiApiKeyEditText?.text.isNullOrEmpty()) View.VISIBLE else View.GONE
        }

        fun updateModelCardUi() {
            isModelLoading = false
            modelLoadError?.let {
                aiModelSelectedValue?.text = it
                return
            }
            if (modelList.isEmpty()) {
                aiModelSelectedValue?.text = getString(R.string.pref_openai_model_not_set)
            } else {
                val selectedName =
                    modelList.firstOrNull { it.id == selectedModelId }?.id ?: modelList.first().id
                aiModelSelectedValue?.text = selectedName
            }
        }

        fun fetchModelsIfEligible() {
            val isAIEnabled = enableAIRepliesCheckbox?.isChecked ?: false
            val providerIsAI = providerPref == "openai" || providerPref == "custom"
            val apiKey = aiApiKeyEditText?.text?.toString()?.trim() ?: ""
            if (isAIEnabled && providerIsAI && apiKey.isNotEmpty()) {
                isModelLoading = true
                modelLoadError = null
                aiModelCard?.visibility = View.VISIBLE
                aiModelSelectedValue?.text = getString(R.string.pref_openai_model_loading)
                // Save key
                preferencesManager?.saveOpenAIApiKey(apiKey)
                OpenAIHelper.invalidateCache()
                OpenAIHelper.fetchModels(this, object : OpenAIHelper.FetchModelsCallback {
                    override fun onModelsFetched(models: List<ModelData>) {
                        handler.post {
                            modelList = models
                            modelLoadError = null
                            selectedModelId = preferencesManager?.getSelectedOpenAIModel()
                            updateModelCardUi()
                        }
                    }

                    override fun onError(errorMessage: String?) {
                        handler.post {
                            modelList = emptyList()
                            modelLoadError = errorMessage ?: "Could not load models"
                            updateModelCardUi()
                        }
                    }
                })
            } else {
                aiModelCard?.visibility = View.GONE
            }
        }

        // Provider selector popup
        aiProviderValue?.setOnClickListener {
            val builder = android.app.AlertDialog.Builder(this)
            builder.setTitle("Select AI Provider")
            builder.setSingleChoiceItems(providerOptions, providerIndex) { dialog, which ->
                providerIndex = which
                providerPref = providerValues[providerIndex]
                aiProviderValue?.text = providerOptions?.get(providerIndex)
                preferencesManager?.saveOpenApiSource(providerPref)
                dialog.dismiss()
                updateAICardsVisibility()
                fetchModelsIfEligible()
            }
            builder.show()
        }

        // Toggle AI enable/disable
        enableAIRepliesCheckbox?.setOnCheckedChangeListener { _, _ ->
            updateAICardsVisibility()
            fetchModelsIfEligible()
        }

        aiApiKeyEditText?.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) fetchModelsIfEligible()
        }
        aiApiKeyEditText?.setOnEditorActionListener { _, _, _ ->
            fetchModelsIfEligible(); false
        }
        aiApiKeyEditText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                fetchModelsIfEligible()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        aiModelCard?.setOnClickListener {
            if (modelList.isEmpty() || isModelLoading || modelLoadError != null) return@setOnClickListener
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Select Model")
            val modelNames = modelList.map { it.id }.toTypedArray()
            val selIdx = modelList.indexOfFirst { it.id == selectedModelId }.takeIf { it >= 0 } ?: 0
            builder.setSingleChoiceItems(modelNames, selIdx) { dialog, which ->
                val selModel = modelList[which]
                selectedModelId = selModel.id
                preferencesManager?.saveSelectedOpenAIModel(selModel.id)
                aiModelSelectedValue?.text = selModel.id
                dialog.dismiss()
            }
            builder.show()
        }

        // Set initial state for UI
        val initKey = preferencesManager?.getOpenAIApiKey() ?: ""
        aiApiKeyEditText?.setText(initKey)
        updateAICardsVisibility()
        fetchModelsIfEligible()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.custom_reply_editor_scroll_view)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}