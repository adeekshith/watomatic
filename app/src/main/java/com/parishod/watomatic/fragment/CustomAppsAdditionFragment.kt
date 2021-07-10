package com.parishod.watomatic.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.parishod.watomatic.R
import com.parishod.watomatic.model.logs.App

import com.parishod.watomatic.model.utils.DbUtils
import kotlinx.android.synthetic.main.fragment_custom_apps.view.*

class CustomAppsAdditionFragment: Fragment()  {
    lateinit var fragmentView: View
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentView = inflater.inflate(R.layout.fragment_custom_apps, container, false)

        fragmentView.customAppTextInputEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                fragmentView.customAppTextInputLayout.isErrorEnabled = false
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })
        fragmentView.customPackageTextInputEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                fragmentView.customPackageTextInputLayout.isErrorEnabled = false
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })
        fragmentView.saveCustomPackageBtn.setOnClickListener{
            when {
                fragmentView.customAppTextInputEditText.text.isNullOrEmpty() -> {
                    fragmentView.customAppTextInputLayout.isErrorEnabled = true
                    fragmentView.customAppTextInputLayout.error = "App name cannot be empty"
                }
                fragmentView.customPackageTextInputEditText.text.isNullOrEmpty() -> {
                    fragmentView.customPackageTextInputLayout.isErrorEnabled = true
                    fragmentView.customPackageTextInputLayout.error = "Package name cannot be empty"
                }
                else -> {
                    val dbutils = DbUtils(activity)
                    val app = App(
                        fragmentView.customAppTextInputEditText.text.toString(),
                        fragmentView.customPackageTextInputEditText.text.toString()
                    )
                    dbutils.insertSupportedApp(app)
                    (fragmentView.customAppTextInputEditText.text as Editable).clear()
                    (fragmentView.customPackageTextInputEditText.text as Editable).clear()
                    Toast.makeText(activity, "Custom App Saved", Toast.LENGTH_SHORT).show()
                }
            }
        }
        return fragmentView
    }
}