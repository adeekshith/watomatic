package com.parishod.watomatic.activity.contactselector

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.parishod.watomatic.R
import com.parishod.watomatic.activity.BaseActivity
import com.parishod.watomatic.databinding.ActivityContactSelectorBinding
import com.parishod.watomatic.fragment.ContactSelectorFragment
import com.parishod.watomatic.model.utils.ContactsHelper
import com.parishod.watomatic.viewmodel.SwipeToKillAppDetectViewModel

import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class ContactSelectorActivity : BaseActivity() {
    private lateinit var contactSelectorFragment: ContactSelectorFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val binding = ActivityContactSelectorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        contactSelectorFragment = supportFragmentManager.findFragmentById(R.id.contact_selector_layout)
                as ContactSelectorFragment

        title = getString(R.string.contact_selector)

        ViewModelProvider(this).get(SwipeToKillAppDetectViewModel::class.java)

        ViewCompat.setOnApplyWindowInsetsListener(binding.contactSelectorRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
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