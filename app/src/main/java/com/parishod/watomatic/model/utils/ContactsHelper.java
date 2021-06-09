package com.parishod.watomatic.model.utils;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.parishod.watomatic.R;
import com.parishod.watomatic.model.adapters.ContactListAdapter;
import com.parishod.watomatic.model.data.ContactHolder;
import com.parishod.watomatic.model.preferences.PreferencesManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ContactsHelper {

    public static final int CONTACT_PERMISSION_REQUEST_CODE = 1;
    private Context mContext;
    private PreferencesManager prefs;

    public ContactsHelper(Context context) {
        mContext = context;
        prefs = PreferencesManager.getPreferencesInstance(context);
    }

    public static ContactsHelper getInstance(Context context) {
        return new ContactsHelper(context);
    }

    public void showContactPicker() {
        ArrayList<ContactHolder> contactList = getContactList();

        View customLayout = View.inflate(mContext, R.layout.dialog_contact_picker, null);

        RecyclerView contactRecyclerView = customLayout.findViewById(R.id.contact_list);
        contactRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        contactRecyclerView.setAdapter(new ContactListAdapter(contactList));

        Button selectAllButton = customLayout.findViewById(R.id.button_select_all);
        selectAllButton.setOnClickListener((v -> {
            for (ContactHolder contact : contactList) {
                contact.setChecked(true);
            }
            contactRecyclerView.getAdapter().notifyDataSetChanged();
        }));

        Button selectNoneButton = customLayout.findViewById(R.id.button_select_none);
        selectNoneButton.setOnClickListener((v -> {
            for (ContactHolder contact : contactList) {
                contact.setChecked(false);
            }
            contactRecyclerView.getAdapter().notifyDataSetChanged();
        }));

        MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(mContext)
                .setTitle(R.string.contact_selector)
                .setView(customLayout)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    Set<String> selectedContacts = new HashSet<>();
                    for (ContactHolder contact : contactList) {
                        if (contact.isChecked())
                            selectedContacts.add(contact.getContactName());
                    }
                    prefs.setReplyToNames(selectedContacts);
                })
                .setNegativeButton(android.R.string.cancel, ((dialog, which) -> {}))
                .setCancelable(false);
        materialAlertDialogBuilder.show();
    }

    private ArrayList<ContactHolder> getContactList() {
        ArrayList<ContactHolder> contactList = new ArrayList<>();
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
                        contactList.add(new ContactHolder(contactName, contactChecked));
                    }
                } while (cursor.moveToNext());
            }
        }

        if (cursor != null) {
            cursor.close();
        }

        return contactList;
    }

    public boolean hasContactPermission() {
        return (ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void requestContactPermission(Activity mActivity) {
        new MaterialAlertDialogBuilder(mContext)
                .setTitle(R.string.permission_dialog_title)
                .setMessage(R.string.contact_permission_dialog_msg)
                .setPositiveButton(R.string.contact_permission_dialog_proceed, ((dialog, which) ->
                        mActivity.requestPermissions(new String[]{ Manifest.permission.READ_CONTACTS }, CONTACT_PERMISSION_REQUEST_CODE)))
                .setNegativeButton(R.string.contact_permission_dialog_cancel, ((dialog, which) -> {}))
                .setCancelable(false)
                .show();

    }
}
