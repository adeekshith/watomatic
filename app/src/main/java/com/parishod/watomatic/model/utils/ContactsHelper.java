package com.parishod.watomatic.model.utils;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.provider.ContactsContract;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.parishod.watomatic.R;
import com.parishod.watomatic.model.data.ContactHolder;
import com.parishod.watomatic.model.preferences.PreferencesManager;

import java.util.ArrayList;
import java.util.Set;

public class ContactsHelper {

    public static final int CONTACT_PERMISSION_REQUEST_CODE = 1;
    private final Context mContext;
    private final PreferencesManager prefs;

    public ContactsHelper(Context context) {
        mContext = context;
        prefs = PreferencesManager.getPreferencesInstance(context);
    }

    public static ContactsHelper getInstance(Context context) {
        return new ContactsHelper(context);
    }

    public ArrayList<ContactHolder> getContactList() {
        ArrayList<ContactHolder> customContactList = new ArrayList<>();

        Set<String> savedCustomContacts = prefs.getCustomReplyNames();
        for (String name : savedCustomContacts) {
            customContactList.add(new ContactHolder(name, true, true));
        }

        if (hasContactPermission()) {
            ArrayList<ContactHolder> unselectedContactList = new ArrayList<>();
            ArrayList<ContactHolder> selectedContactList = new ArrayList<>();
            Set<String> previousSelectedContacts = prefs.getReplyToNames();

            ContentResolver contentResolver = mContext.getContentResolver();
            String[] queryColumnAttribute = {ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY};
            Cursor cursor = contentResolver.query(ContactsContract.Data.CONTENT_URI, queryColumnAttribute, null, null, ContactsContract.Contacts.SORT_KEY_PRIMARY + " ASC");

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        int columnIndex = cursor.getColumnIndex(ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY);
                        String contactName = cursor.getString(columnIndex);
                        if (contactName != null && !contactName.isEmpty()) {
                            boolean contactChecked = previousSelectedContacts.contains(contactName);
                            if (contactChecked) {
                                selectedContactList.add(new ContactHolder(contactName, true));
                            } else {
                                unselectedContactList.add(new ContactHolder(contactName, false));
                            }
                        }
                    } while (cursor.moveToNext());
                }
            }

            if (cursor != null) {
                cursor.close();
            }

            customContactList.addAll(selectedContactList);
            customContactList.addAll(unselectedContactList);
        }

        return customContactList;
    }

    public boolean hasContactPermission() {
        return (ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void requestContactPermission(Activity mActivity) {
        new MaterialAlertDialogBuilder(mContext)
                .setTitle(R.string.contact_permission_dialog_title)
                .setMessage(R.string.contact_permission_suggestion_dialog_msg)
                .setPositiveButton(R.string.contact_permission_dialog_enable_permission, ((dialog, which) ->
                        mActivity.requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, CONTACT_PERMISSION_REQUEST_CODE)))
                .setNegativeButton(R.string.contact_permission_dialog_not_now, ((dialog, which) -> {
                }))
                .setCancelable(false)
                .show();

    }
}
