package com.parishod.watomatic.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.parishod.watomatic.R
import com.parishod.watomatic.model.preferences.PreferencesManager

class NotificationSettingsFragment: Fragment() {
    lateinit var dismissNotificationSwitch: SwitchMaterial
    lateinit var dismissNotificationInfoText: TextView
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_notification_settings, container, false)

        preferencesManager = PreferencesManager.getPreferencesInstance(activity)

        dismissNotificationSwitch = view.findViewById(R.id.dismissNotificationSwitch)
        dismissNotificationSwitch.isChecked = preferencesManager.isDismissNotificationEnabled

        dismissNotificationInfoText = view.findViewById(R.id.dismissNotificationInfoText)
        dismissNotificationInfoText.setText(if(preferencesManager.isDismissNotificationEnabled) R.string.dismiss_notification_switch_on_text else R.string.dismiss_notification_switch_off_text)

        dismissNotificationSwitch.setOnCheckedChangeListener {buttonView, isChecked ->
            dismissNotificationInfoText.setText(if(isChecked) R.string.dismiss_notification_switch_on_text else R.string.dismiss_notification_switch_off_text)
            preferencesManager.setDismissNotificationPref(isChecked)
        }
        return view
    }
}