package com.parishod.watomatic.activity.contactselector

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.parishod.watomatic.R
import com.parishod.watomatic.activity.BaseActivity
import com.parishod.watomatic.databinding.ActivityContactSelectorBinding
import com.parishod.watomatic.viewmodel.SwipeToKillAppDetectViewModel

class ContactSelectorActivity: BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityContactSelectorBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }

        title = getString(R.string.contact_selector)

        val viewModel = ViewModelProvider(this).get(SwipeToKillAppDetectViewModel::class.java)
    }
}