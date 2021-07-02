package com.parishod.watomatic.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
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

        contactsHelper = ContactsHelper.getInstance(requireContext())
        prefs = PreferencesManager.getPreferencesInstance(requireContext())

        loadContactList()

        return binding.root
    }

    private fun loadContactList() {
        contactList = contactsHelper.contactList

        binding.contactList.layoutManager = LinearLayoutManager(requireContext())
        binding.contactList.adapter = ContactListAdapter(activity, contactList)

        binding.buttonSelectAll.setOnClickListener {
            toggleSelection(true)
        }

        binding.buttonSelectNone.setOnClickListener {
            toggleSelection(false)
        }
    }

    private fun toggleSelection(checked: Boolean) {
        val adapter = (binding.contactList.adapter!! as ContactListAdapter)
        adapter.createCheckpoint()
        for (contact in contactList) {
            contact.isChecked = checked
        }
        adapter.saveSelectedContactList()
        adapter.notifyDataSetChanged()

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
}