package com.parishod.watomatic.activity.customreplyeditor

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.parishod.watomatic.R
import com.parishod.watomatic.model.preferences.PreferencesManager
import com.parishod.watomatic.model.utils.Constants
import com.parishod.watomatic.network.OpenAIService
import com.parishod.watomatic.utils.UnsavedChangesDialog
import com.parishod.watomatic.network.RetrofitInstance
import com.parishod.watomatic.network.model.openai.OpenAIModelsResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OtherAiConfigurationActivity : AppCompatActivity() {

    private lateinit var providerInput: AutoCompleteTextView
    private lateinit var apiKeyInput: TextInputEditText
    private lateinit var baseUrlInput: TextInputEditText
    private lateinit var baseUrlLayout: TextInputLayout
    private lateinit var modelInput: AutoCompleteTextView
    private lateinit var systemPromptInput: TextInputEditText

    private val providers = listOf("OpenAI", "Claude", "Grok", "Gemini", "DeepSeek", "Mistral", "Custom")
    private val providerUrls = Constants.PROVIDER_URLS

    private lateinit var preferencesManager: PreferencesManager

    // Track initial state for unsaved changes detection
    private var initialProvider: String = ""
    private var initialApiKey: String = ""
    private var initialBaseUrl: String = ""
    private var initialModel: String = ""
    private var initialSystemPrompt: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_other_ai_configuration)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Setup back button handler for unsaved changes
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Check if configuration has changed
                if (hasUnsavedChanges()) {
                    showUnsavedChangesDialog()
                } else {
                    // Default behavior - remove callback and trigger back
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        providerInput = findViewById(R.id.llmProviderAutoCompleteTextView)
        apiKeyInput = findViewById(R.id.apiKeyEditText)
        baseUrlInput = findViewById(R.id.baseUrlEditText)
        baseUrlLayout = findViewById(R.id.baseUrlInputLayout)
        modelInput = findViewById(R.id.modelAutoCompleteTextView)
        systemPromptInput = findViewById(R.id.systemPromptEditText)

        preferencesManager = PreferencesManager.getPreferencesInstance(this)

        setupProviderDropdown()
        prefillData()
        setupListeners()

        findViewById<View>(R.id.saveConfigBtn).setOnClickListener {
            saveConfiguration()
        }
    }

    private fun prefillData() {
        val savedProvider = preferencesManager.openApiSource
        if (!savedProvider.isNullOrEmpty()) {
            providerInput.setText(savedProvider, false)
            updateBaseUrlVisibility(savedProvider)
            initialProvider = savedProvider
        } else {
             providerInput.setText(providers[0], false)
             initialProvider = providers[0]
        }

        val savedApiKey = preferencesManager.openAIApiKey
        if (!savedApiKey.isNullOrEmpty()) {
            apiKeyInput.setText(savedApiKey)
            initialApiKey = savedApiKey
        } else {
            initialApiKey = ""
        }

        val savedBaseUrl = preferencesManager.customOpenAIApiUrl
        if (!savedBaseUrl.isNullOrEmpty()) {
            baseUrlInput.setText(savedBaseUrl)
            initialBaseUrl = savedBaseUrl
        } else {
            initialBaseUrl = ""
        }

        val savedModel = preferencesManager.selectedOpenAIModel
        if (!savedModel.isNullOrEmpty()) {
            modelInput.setText(savedModel, false)
            initialModel = savedModel
        } else {
            initialModel = ""
        }

        val savedPrompt = preferencesManager.openAICustomPrompt
        if (!savedPrompt.isNullOrEmpty()) {
            systemPromptInput.setText(savedPrompt)
            initialSystemPrompt = savedPrompt
        } else {
            initialSystemPrompt = ""
        }

        // Capture actual UI state after prefilling (handles XML defaults and preference defaults)
        // This ensures we compare against what's actually displayed
        initialProvider = providerInput.text.toString()
        initialApiKey = apiKeyInput.text.toString()
        initialBaseUrl = baseUrlInput.text.toString()
        initialModel = modelInput.text.toString()
        initialSystemPrompt = systemPromptInput.text.toString()

        Log.d("OtherAiConfig", "Initial state - Provider: '$initialProvider', ApiKey: '$initialApiKey', BaseUrl: '$initialBaseUrl', Model: '$initialModel', Prompt: '$initialSystemPrompt'")
    }

    private fun setupProviderDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, providers)
        providerInput.setAdapter(adapter)
        
        // Default to OpenAI if not set (though XML has default text)
        if (providerInput.text.isEmpty()) {
            providerInput.setText(providers[0], false)
        }
    }

    private fun setupListeners() {
        providerInput.setOnItemClickListener { _, _, position, _ ->
            val selectedProvider = providers[position] // This might be risky if filtered, but inputType=none
            // Better to get text
            val provider = providerInput.text.toString()
            updateBaseUrlVisibility(provider)
            modelInput.text = null // Clear selected model
            modelInput.setAdapter(null) // Clear adapter
            fetchModels()
        }
        
        // Also listen for text changes in case it's set programmatically or otherwise
        providerInput.addTextChangedListener(object : TextWatcher {
             override fun afterTextChanged(s: Editable?) {
                 updateBaseUrlVisibility(s.toString())
                 // Only clear if user changed it? 
                 // But this is called on prefill too. 
                 // We should be careful not to clear prefilled data.
                 // Actually, prefillData sets text, which triggers this.
                 // So we should NOT clear model here unconditionally.
                 // But we should fetch models.
                 fetchModels()
             }
             override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
             override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        apiKeyInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!s.isNullOrEmpty()) {
                    fetchModels()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        baseUrlInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (providerInput.text.toString() == "Custom") {
                    fetchModels()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        modelInput.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                val provider = providerInput.text.toString()
                val apiKey = apiKeyInput.text.toString()

                if (provider.isEmpty()) {
                    Toast.makeText(this, "Please select an LLM provider first.", Toast.LENGTH_SHORT).show()
                    return@setOnTouchListener true
                }
                if (apiKey.isEmpty()) {
                    Toast.makeText(this, "Enter an API key to fetch available models.", Toast.LENGTH_SHORT).show()
                    return@setOnTouchListener true
                }
                // If models are not fetched yet, we might want to trigger fetch or check if adapter is empty
                if (modelInput.adapter == null || modelInput.adapter.count == 0) {
                     // Try fetching if we have credentials
                     fetchModels()
                     // Don't block, but maybe show message if it's taking time?
                     // For now, let it proceed, fetchModels will populate adapter.
                     // If adapter is empty, it will show empty dropdown.
                }
            }
            false
        }
    }

    private fun updateBaseUrlVisibility(provider: String) {
        if (provider == "Custom") {
            baseUrlLayout.visibility = View.VISIBLE
        } else {
            baseUrlLayout.visibility = View.GONE
        }
    }

    private fun fetchModels() {
        val provider = providerInput.text.toString()
        val apiKey = apiKeyInput.text.toString()

        if (apiKey.isEmpty()) return

        val baseUrl = if (provider == "Custom") {
            baseUrlInput.text.toString()
        } else {
            providerUrls[provider]
        }

        if (baseUrl.isNullOrEmpty()) return

        // Create Retrofit instance
        val retrofit = RetrofitInstance.getOpenAIRetrofitInstance(baseUrl)
        val service = retrofit.create(OpenAIService::class.java)

        val headers = HashMap<String, String>()
        if (provider == "Claude") {
            headers["x-api-key"] = apiKey
            headers["anthropic-version"] = "2023-06-01"
        } else {
            headers["Authorization"] = "Bearer $apiKey"
        }

        service.getModels(headers).enqueue(object : Callback<OpenAIModelsResponse> {
            override fun onResponse(call: Call<OpenAIModelsResponse>, response: Response<OpenAIModelsResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val models = response.body()!!.data.map { it.id }
                    updateModelDropdown(models)
                } else {
                    Log.e("OtherAiConfig", "Error fetching models: ${response.code()}")
                    Toast.makeText(this@OtherAiConfigurationActivity, "Error fetching models: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<OpenAIModelsResponse>, t: Throwable) {
                Log.e("OtherAiConfig", "Failed to fetch models", t)
                Toast.makeText(this@OtherAiConfigurationActivity, "Failed to fetch models: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateModelDropdown(models: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, models)
        modelInput.setAdapter(adapter)
        if (models.isNotEmpty()) {
             // If current selection is not in list, maybe clear it?
             // For now, let user select.
        }
    }

    private fun saveConfiguration() {
        val provider = providerInput.text.toString()
        val apiKey = apiKeyInput.text.toString()
        val model = modelInput.text.toString()

        if (apiKey.isEmpty()) {
            Toast.makeText(this, "API key cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        if (model.isEmpty()) {
            Toast.makeText(this, "Select a model", Toast.LENGTH_SHORT).show()
            return
        }

        // Return result
        val resultIntent = Intent()
        resultIntent.putExtra("provider", provider)
        resultIntent.putExtra("apiKey", apiKey)
        resultIntent.putExtra("model", model)
        resultIntent.putExtra("systemPrompt", systemPromptInput.text.toString())
        if (provider == "Custom") {
            resultIntent.putExtra("baseUrl", baseUrlInput.text.toString())
        }

        // Update initial state to prevent unsaved changes dialog
        initialProvider = provider
        initialApiKey = apiKey
        initialModel = model
        initialSystemPrompt = systemPromptInput.text.toString()
        initialBaseUrl = if (provider == "Custom") baseUrlInput.text.toString() else ""

        setResult(RESULT_OK, resultIntent)
        finish()
    }

    /**
     * Check if the current configuration has changed from the initial state
     */
    private fun hasUnsavedChanges(): Boolean {
        val currentProvider = providerInput.text.toString()
        val currentApiKey = apiKeyInput.text.toString()
        val currentBaseUrl = baseUrlInput.text.toString()
        val currentModel = modelInput.text.toString()
        val currentSystemPrompt = systemPromptInput.text.toString()

        val changed = currentProvider != initialProvider ||
                currentApiKey != initialApiKey ||
                currentBaseUrl != initialBaseUrl ||
                currentModel != initialModel ||
                currentSystemPrompt != initialSystemPrompt

        Log.d("OtherAiConfig", "hasUnsavedChanges: $changed")
        if (changed) {
            Log.d("OtherAiConfig", "  Provider: '$currentProvider' vs '$initialProvider' = ${currentProvider != initialProvider}")
            Log.d("OtherAiConfig", "  ApiKey: '$currentApiKey' vs '$initialApiKey' = ${currentApiKey != initialApiKey}")
            Log.d("OtherAiConfig", "  BaseUrl: '$currentBaseUrl' vs '$initialBaseUrl' = ${currentBaseUrl != initialBaseUrl}")
            Log.d("OtherAiConfig", "  Model: '$currentModel' vs '$initialModel' = ${currentModel != initialModel}")
            Log.d("OtherAiConfig", "  Prompt: '$currentSystemPrompt' vs '$initialSystemPrompt' = ${currentSystemPrompt != initialSystemPrompt}")
        }
        return changed
    }

    /**
     * Show a dialog to confirm discarding unsaved configuration changes
     */
    private fun showUnsavedChangesDialog() {
        UnsavedChangesDialog.show(
            context = this,
            onDiscard = {
                // Navigate back without saving
                finish()
            }
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_other_ai_config, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.action_save -> {
                saveConfiguration()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
