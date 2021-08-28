package com.parishod.watomatic.fragment

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.*
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.parishod.watomatic.R
import com.parishod.watomatic.databinding.FragmentContactSelectorBinding
import com.parishod.watomatic.model.adapters.ContactListAdapter
import com.parishod.watomatic.model.data.ContactHolder
import com.parishod.watomatic.model.preferences.PreferencesManager
import com.parishod.watomatic.model.utils.ContactsHelper
import java.util.*

class ContactSelectorFragment : Fragment() {

    private var _binding: FragmentContactSelectorBinding? = null
    private val binding get() = _binding!!

    private lateinit var contactsHelper: ContactsHelper
    private lateinit var prefs: PreferencesManager

    private lateinit var contactList: ArrayList<ContactHolder>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContactSelectorBinding.inflate(inflater, container, false)

        contactsHelper = ContactsHelper.getInstance(requireContext()).also {
            if (!it.hasContactPermission()) {
                setHasOptionsMenu(true)
            }
        }
        prefs = PreferencesManager.getPreferencesInstance(requireContext())

        loadContactList()

        return binding.root
    }

    fun loadContactList() {
        binding.dialogButtons.visibility = if (contactsHelper.hasContactPermission()) View.VISIBLE else View.GONE

        contactList = contactsHelper.contactList

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
                adapter.notifyItemChanged(position)
            }
        }
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
                contactsHelper.requestContactPermission(activity)
            }
        }
        return super.onOptionsItemSelected(item)
    }
}