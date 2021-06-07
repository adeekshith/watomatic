package com.parishod.watomatic.model.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;

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
            cursor.moveToFirst();
            do {
                int columnIndex = cursor.getColumnIndex(ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY);
                String contactName = cursor.getString(columnIndex);
                boolean contactChecked = previousSelectedContacts.contains(contactName);
                contactList.add(new ContactHolder(contactName, contactChecked));
            } while (cursor.moveToNext());
        }

        if (cursor != null) {
            cursor.close();
        }

        return contactList;
    }
}
