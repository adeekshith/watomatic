package com.parishod.watomatic.fragment

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.*
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.parishod.watomatic.R
import com.parishod.watomatic.databinding.FragmentContactSelectorBinding
import com.parishod.watomatic.model.adapters.ContactListAdapter
import com.parishod.watomatic.model.data.ContactHolder
import com.parishod.watomatic.model.preferences.PreferencesManager
import com.parishod.watomatic.model.utils.ContactsHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class ContactSelectorFragment : Fragment() {

    private fun initHeaderControls() {
        val enabled = prefs.isContactReplyEnabled
        // Switch state
        binding.switchContactReplies.isChecked = enabled
        // Mode state
        val isBlacklist = prefs.isContactReplyBlacklistMode()
        binding.replyModeGroup.check(if (isBlacklist) R.id.radio_blacklist else R.id.radio_whitelist)

        // Listeners
        binding.switchContactReplies.setOnCheckedChangeListener { _, isChecked ->
            prefs.setContactReplyEnabled(isChecked)
            enableContactUi(isChecked)
        }
        binding.replyModeGroup.setOnCheckedChangeListener { _, checkedId ->
            prefs.setContactReplyBlacklistMode(checkedId == R.id.radio_blacklist)
        }

        enableContactUi(enabled)
    }

    private fun setChildrenEnabled(view: View, enabled: Boolean) {
        view.isEnabled = enabled
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                setChildrenEnabled(view.getChildAt(i), enabled)
            }
        }
    }

    private fun enableContactUi(enabled: Boolean) {
        // Visually dim/enable
        binding.modeContainer.alpha = if (enabled) 1f else 0.5f
        setChildrenEnabled(binding.modeContainer, enabled)

        binding.searchLayout.alpha = if (enabled) 1f else 0.5f
        setChildrenEnabled(binding.searchLayout, enabled)

        binding.dialogButtons.alpha = if (enabled) 1f else 0.5f
        setChildrenEnabled(binding.dialogButtons, enabled)

        binding.contactList.alpha = if (enabled) 1f else 0.5f
        binding.contactList.isEnabled = enabled

        binding.addCustomButton.alpha = if (enabled) 1f else 0.5f
        binding.addCustomButton.isEnabled = enabled
    }

    private var _binding: FragmentContactSelectorBinding? = null
    private val binding get() = _binding!!

    private lateinit var contactsHelper: ContactsHelper
    private lateinit var prefs: PreferencesManager

    private lateinit var contactList: ArrayList<ContactHolder>
    private var searchJob: Job? = null

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContactSelectorBinding.inflate(inflater, container, false)

        contactsHelper = ContactsHelper.getInstance(requireContext()).also {
            if (!it.hasContactPermission()) {
                it.requestContactPermission(requireActivity())
            }
        }
        prefs = PreferencesManager.getPreferencesInstance(requireContext())

        // Initialize header controls (switch and mode)
        initHeaderControls()

        loadContactList()

        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300) // 300ms debounce
                    (binding.contactList.adapter as? ContactListAdapter)?.filter?.filter(s.toString())
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        return binding.root
    }

    fun loadContactList() {
        binding.dialogButtons.visibility = if (contactsHelper.hasContactPermission()) View.VISIBLE else View.GONE

        contactList = contactsHelper.getContactList()

        binding.contactList.layoutManager = LinearLayoutManager(requireContext())
        binding.contactList.adapter = ContactListAdapter(activity, contactList)

        binding.buttonSelectAll.setOnClickListener {
            toggleSelection(true)
        }

        binding.buttonSelectNone.setOnClickListener {
            toggleSelection(false)
        }

        binding.addCustomButton.setOnClickListener { addCustomContactDialog() }
    }

    private fun toggleSelection(checked: Boolean) {
        val adapter = (binding.contactList.adapter!! as ContactListAdapter)
        adapter.createCheckpoint()
        contactList.forEachIndexed { position, contact ->
            if (contact.isChecked != checked) {
                contact.isChecked = checked
            }
        }
        adapter.notifyDataSetChanged()
        adapter.saveSelectedContactList()

        val snackbar = Snackbar.make(
                binding.root,
                if (checked) R.string.all_contacts_selected else R.string.all_contacts_unselected,
                Snackbar.LENGTH_LONG
        )
        snackbar.setAction(R.string.undo) {
            adapter.restoreCheckpoint()
            adapter.saveSelectedContactList()
        }
        snackbar.show()
    }

    private fun addCustomContactDialog() {
        val builder = MaterialAlertDialogBuilder(requireActivity())
                .setTitle(R.string.add_custom_contact)

        val input = EditText(activity).also {
            it.inputType = InputType.TYPE_TEXT_VARIATION_PERSON_NAME
        }

        builder.setPositiveButton(android.R.string.ok, null)

        builder.setNegativeButton(android.R.string.cancel) { dialog, _ ->
            dialog.cancel()
        }

        builder.create().also { dialog ->
            val margin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, activity?.resources?.displayMetrics)
            dialog.setView(input, margin.toInt(), 0, margin.toInt(), 0)

            dialog.setOnShowListener {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val name = input.text.toString()
                    when {
                        name.isBlank() -> {
                            input.error = getString(R.string.error_name_cannot_be_blank)
                            //Make sure error text is shown by focusing input
                            input.requestFocus()
                        }
                        customExists(name) -> {
                            input.error = getString(R.string.error_name_cannot_be_duplicate)
                            input.requestFocus()
                        }
                        else -> {
                            val adapter = binding.contactList.adapter as ContactListAdapter
                            adapter.addCustomName(name)
                            binding.contactList.scrollToPosition(0)
                            dialog.dismiss()
                        }
                    }
                }
            }

            dialog.show()
        }
    }

    private fun customExists(name: String): Boolean {
        contactList.forEach { contact ->
            if (contact.contactName == name) return true
            //Custom contacts are first
            else if (!contact.isCustom) return false
        }
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.enable_contact_permission_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.enable_contact_permission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                contactsHelper.requestContactPermission(requireActivity())
            }
        }
        return super.onOptionsItemSelected(item)
    }
}