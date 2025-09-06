package com.parishod.watomatic.fragment

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.parishod.watomatic.utils.ThemeUtils
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.parishod.watomatic.R
import com.parishod.watomatic.model.adapters.AppsAdapter
import com.parishod.watomatic.model.adapters.CooldownAdapter
import com.parishod.watomatic.model.adapters.MessageTypeAdapter
import com.parishod.watomatic.model.data.AppItem
import com.parishod.watomatic.model.data.CooldownItem
import com.parishod.watomatic.model.data.DialogConfig
import com.parishod.watomatic.model.data.MessageTypeItem
import com.parishod.watomatic.model.enums.DialogType
import com.parishod.watomatic.model.interfaces.DialogActionListener

class UniversalDialogFragment(val mContext: Context) : DialogFragment() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var searchLayout: TextInputLayout
    private lateinit var searchEditText: TextInputEditText
    private lateinit var descriptionText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var saveButton: MaterialButton
    private lateinit var resetButton: MaterialButton

    private var dialogConfig: DialogConfig? = null
    private var actionListener: DialogActionListener? = null
    companion object {
        fun newInstance(context: Context, config: DialogConfig): UniversalDialogFragment {
            val dialog = UniversalDialogFragment(context)
            val args = Bundle().apply {
                putParcelable("config", config)
            }
            dialog.arguments = args
            return dialog
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialogTheme)

        arguments?.let {
            dialogConfig = it.getParcelable("config")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.let { window ->
            // Remove FLAG_FULLSCREEN to avoid overlap
            window.statusBarColor = ThemeUtils.getThemeColor(requireContext(), com.google.android.material.R.attr.colorPrimaryVariant)
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_universal, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Apply status bar insets to avoid overlap (API 21+, uses root LinearLayout)
        view.setOnApplyWindowInsetsListener { v, insets ->
            val statusBar = insets.systemWindowInsetTop
            v.setPadding(0, statusBar, 0, 0)
            insets
        }
        view.requestApplyInsets()

        setupViews(view)
        configureDialog()
    }

    private fun setupViews(view: View) {
        toolbar = view.findViewById(R.id.toolbar)
        searchLayout = view.findViewById(R.id.search_layout)
        searchEditText = view.findViewById(R.id.search_edit_text)
        descriptionText = view.findViewById(R.id.description_text)
        recyclerView = view.findViewById(R.id.recycler_view)
        saveButton = view.findViewById(R.id.save_button)
        resetButton = view.findViewById(R.id.reset_button)
    }

    private fun configureDialog() {
        dialogConfig?.let { config ->
            // Setup toolbar
            toolbar.title = config.title
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
            toolbar.setNavigationOnClickListener { dismiss() }

            // Setup search bar
            searchLayout.visibility = if (config.showSearch) View.VISIBLE else View.GONE
            if (config.showSearch) {
                searchLayout.setStartIconDrawable(R.drawable.ic_search)
                searchEditText.hint = config.searchHint
                setupSearchListener()
            }

            // Setup description
            if (config.description.isNotEmpty()) {
                descriptionText.visibility = View.VISIBLE
                descriptionText.text = config.description
            } else {
                descriptionText.visibility = View.GONE
            }

            // Setup RecyclerView
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            recyclerView.adapter = createAdapter(config)

            // Setup save button
            if(!TextUtils.isEmpty(config.saveButtonText)) {
                saveButton.text = config.saveButtonText
                saveButton.setOnClickListener {
                    actionListener?.onSaveClicked(config.dialogType)
                    dismiss()
                }
            }else{
                saveButton.visibility = View.GONE
            }

            // Setup reset button
            if (config.dialogType == DialogType.COOLDOWN) {
                resetButton.visibility = View.VISIBLE
                resetButton.setOnClickListener {
                    (recyclerView.adapter as? CooldownAdapter)?.reset()
                }
            }
        }
    }

    private fun createAdapter(config: DialogConfig): RecyclerView.Adapter<*> {
        return when (config.dialogType) {
            DialogType.APPS -> AppsAdapter(config.items as List<AppItem>) { position, isChecked ->
                actionListener?.onItemToggled(position, isChecked)
            }
            DialogType.MESSAGE_TYPE -> MessageTypeAdapter(config.items as List<MessageTypeItem>) { position, isSelected ->
                actionListener?.onItemSelected(position, isSelected)
            }
            DialogType.COOLDOWN -> CooldownAdapter(config.items as List<CooldownItem>) { totalMinutes ->
                actionListener?.onCooldownChanged(totalMinutes)
            }
        }
    }

    private fun setupSearchListener() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                actionListener?.onSearchQuery(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    fun setActionListener(listener: DialogActionListener) {
        this.actionListener = listener
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            dialog?.setCanceledOnTouchOutside(false)
        }
    }
}