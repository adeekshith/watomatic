package com.parishod.watomatic.model.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.ContactsContract
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.parishod.watomatic.R
import com.parishod.watomatic.model.data.ContactHolder
import com.parishod.watomatic.model.preferences.PreferencesManager

class ContactsHelper(private val mContext: Context) {
    private val prefs: PreferencesManager = PreferencesManager.getPreferencesInstance(mContext)

    fun getContactList(): ArrayList<ContactHolder> {
        val customContactList = ArrayList<ContactHolder>()
        val savedCustomContacts = prefs.customReplyNames
        for (name in savedCustomContacts) {
            customContactList.add(ContactHolder(name, true, true))
        }
        if (hasContactPermission()) {
            val unselectedContactList = ArrayList<ContactHolder>()
            val selectedContactList = ArrayList<ContactHolder>()
            val previousSelectedContacts = prefs.replyToNames
            val contentResolver = mContext.contentResolver
            val queryColumnAttribute = arrayOf(
                ContactsContract.Data.DISPLAY_NAME_PRIMARY,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            val selection = ContactsContract.Data.MIMETYPE + " = ?"
            val selectionArgs = arrayOf(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
            val cursor = contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                queryColumnAttribute,
                selection,
                selectionArgs,
                ContactsContract.Data.SORT_KEY_PRIMARY + " ASC"
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    do {
                        val nameColumnIndex = it.getColumnIndex(ContactsContract.Data.DISPLAY_NAME_PRIMARY)
                        val numberColumnIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        val contactName = it.getString(nameColumnIndex)
                        val phoneNumber = it.getString(numberColumnIndex)
                        if (contactName != null && contactName.isNotEmpty()) {
                            val contactChecked = previousSelectedContacts.contains(contactName)
                            val contactHolder = ContactHolder(contactName, phoneNumber, contactChecked)
                            if (contactChecked) {
                                selectedContactList.add(contactHolder)
                            } else {
                                unselectedContactList.add(contactHolder)
                            }
                        }
                    } while (it.moveToNext())
                }
            }
            customContactList.addAll(selectedContactList)
            customContactList.addAll(unselectedContactList)
        }
        return customContactList
    }

    fun hasContactPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            mContext,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    fun requestContactPermission(mActivity: Activity) {
        MaterialAlertDialogBuilder(mContext)
            .setTitle(R.string.contact_permission_dialog_title)
            .setMessage(R.string.contact_permission_suggestion_dialog_msg)
            .setPositiveButton(
                R.string.contact_permission_dialog_enable_permission
            ) { _, _ ->
                mActivity.requestPermissions(
                    arrayOf(Manifest.permission.READ_CONTACTS),
                    CONTACT_PERMISSION_REQUEST_CODE
                )
            }
            .setNegativeButton(
                R.string.contact_permission_dialog_not_now
            ) { _, _ -> }
            .setCancelable(false)
            .show()
    }

    companion object {
        const val CONTACT_PERMISSION_REQUEST_CODE = 1
        fun getInstance(context: Context): ContactsHelper {
            return ContactsHelper(context)
        }
    }
}