package com.parishod.watomatic.fragment

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.parishod.watomatic.R
import com.parishod.watomatic.databinding.FragmentContactSelectorBinding
import com.parishod.watomatic.model.adapters.ContactListAdapter
import com.parishod.watomatic.model.data.ContactHolder
import com.parishod.watomatic.model.preferences.PreferencesManager
import com.parishod.watomatic.model.utils.ContactsHelper
import java.util.*

class ContactSelectorFragment: Fragment() {

    private var _binding: FragmentContactSelectorBinding? = null
    private val binding get() = _binding!!

    private lateinit var contactsHelper: ContactsHelper
    private lateinit var prefs: PreferencesManager

    private lateinit var contactList: ArrayList<ContactHolder>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

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
        binding.contactList.adapter = ContactListAdapter(contactList)

        binding.buttonSelectAll.setOnClickListener {
            for (contact in contactList) {
                contact.isChecked = true
            }
            binding.contactList.adapter!!.notifyDataSetChanged()
        }

        binding.buttonSelectNone.setOnClickListener {
            for (contact in contactList) {
                contact.isChecked = false
            }
            binding.contactList.adapter!!.notifyDataSetChanged()
        }
    }

    private fun saveAndExit() {
        val selectedContacts = HashSet<String>()
        for (contact in contactList) {
            if (contact.isChecked) selectedContacts.add(contact.contactName)
        }
        prefs.replyToNames = selectedContacts
        activity?.finish()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.save_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.ic_save -> saveAndExit()
        }
        return super.onOptionsItemSelected(item)
    }
}