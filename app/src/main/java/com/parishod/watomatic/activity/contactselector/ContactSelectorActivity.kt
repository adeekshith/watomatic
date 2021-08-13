package com.parishod.watomatic.activity.contactselector

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.parishod.watomatic.R
import com.parishod.watomatic.activity.BaseActivity
import com.parishod.watomatic.databinding.ActivityContactSelectorBinding
import com.parishod.watomatic.fragment.ContactSelectorFragment
import com.parishod.watomatic.model.utils.ContactsHelper
import com.parishod.watomatic.viewmodel.SwipeToKillAppDetectViewModel

class ContactSelectorActivity: BaseActivity() {
    private lateinit var contactSelectorFragment: ContactSelectorFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityContactSelectorBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }

        contactSelectorFragment = supportFragmentManager.findFragmentById(R.id.contact_selector_layout)
                as ContactSelectorFragment

        title = getString(R.string.contact_selector)

        ViewModelProvider(this).get(SwipeToKillAppDetectViewModel::class.java)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == ContactsHelper.CONTACT_PERMISSION_REQUEST_CODE && this::contactSelectorFragment.isInitialized) {
            contactSelectorFragment.loadContactList()
        }
    }
}