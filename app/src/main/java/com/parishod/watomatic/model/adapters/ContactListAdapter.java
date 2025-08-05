package com.parishod.watomatic.model.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.parishod.watomatic.databinding.ContactListRowBinding;
import com.parishod.watomatic.databinding.CustomContactListRowBinding;
import com.parishod.watomatic.model.data.ContactHolder;
import com.parishod.watomatic.model.preferences.PreferencesManager;
import com.parishod.watomatic.model.utils.ContactsHelper;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ContactListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Filterable {
    private final ArrayList<ContactHolder> contactHolderArrayList;
    private ArrayList<ContactHolder> contactHolderArrayListFiltered;
    private Set<String> contactArrayCheckpoint = new HashSet<>();
    private final Context mContext;

    public static final int ITEM_TYPE_CONTACT = 0;
    public static final int ITEM_TYPE_CUSTOM = 1;

    public ContactListAdapter(Context context, ArrayList<ContactHolder> contactHolderArrayList) {
        this.contactHolderArrayList = contactHolderArrayList;
        this.contactHolderArrayListFiltered = new ArrayList<>(contactHolderArrayList);
        mContext = context;
    }

    @NonNull
    @NotNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        if (viewType == ITEM_TYPE_CONTACT) {
            ContactListRowBinding binding = ContactListRowBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(binding);
        } else {
            CustomContactListRowBinding binding = CustomContactListRowBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new CustomHolder(binding);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return contactHolderArrayListFiltered.get(position).isCustom() ? ITEM_TYPE_CUSTOM : ITEM_TYPE_CONTACT;
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull RecyclerView.ViewHolder viewHolder, int position) {
        if (getItemViewType(position) == ITEM_TYPE_CONTACT) {
            ViewHolder holder = (ViewHolder) viewHolder;

            ContactListRowBinding binding = holder.getBinding();
            binding.contactCheckbox.setChecked(contactHolderArrayListFiltered.get(position).isChecked());
            binding.contactCheckbox.setText(contactHolderArrayListFiltered.get(position).getContactName());
            binding.contactCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                contactHolderArrayListFiltered.get(position).setChecked(isChecked);
                saveSelectedContactList();
            });
        } else {
            CustomHolder holder = (CustomHolder) viewHolder;

            CustomContactListRowBinding binding = holder.getBinding();
            binding.contactName.setText(contactHolderArrayListFiltered.get(position).getContactName());
            binding.deleteButton.setOnClickListener((v -> {
                ContactHolder contactHolder = contactHolderArrayListFiltered.get(position);
                contactHolderArrayList.remove(contactHolder);
                contactHolderArrayListFiltered.remove(contactHolder);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, contactHolderArrayListFiltered.size());
                saveSelectedContactList();
            }));
        }
    }

    @Override
    public void onViewRecycled(@NonNull @NotNull RecyclerView.ViewHolder viewHolder) {
        if (viewHolder.getClass() == ViewHolder.class) {
            ViewHolder holder = (ViewHolder) viewHolder;
            ContactListRowBinding binding = holder.getBinding();
            binding.contactCheckbox.setOnCheckedChangeListener(null);
            super.onViewRecycled(holder);
        }
    }

    public void saveSelectedContactList() {
        Set<String> selectedContacts = new HashSet<>();
        Set<String> customContacts = new HashSet<>();
        for (ContactHolder contact : contactHolderArrayList) {
            if (contact.isCustom())
                customContacts.add(contact.getContactName());
            else if (contact.isChecked())
                selectedContacts.add(contact.getContactName());
        }
        PreferencesManager prefs = PreferencesManager.getPreferencesInstance(mContext);
        if (ContactsHelper.getInstance(mContext).hasContactPermission())
            prefs.setReplyToNames(selectedContacts);
        prefs.setCustomReplyNames(customContacts);
    }

    public void createCheckpoint() {
        contactArrayCheckpoint = new HashSet<>();
        for (ContactHolder contact : contactHolderArrayList) {
            if (contact.isChecked()) contactArrayCheckpoint.add(contact.getContactName());
        }
    }

    public void restoreCheckpoint() {
        for (int position = 0; position < contactHolderArrayList.size(); position++) {
            ContactHolder contact = contactHolderArrayList.get(position);
            boolean checked = contactArrayCheckpoint.contains(contact.getContactName());
            if (contact.isChecked() != checked) {
                contact.setChecked(checked);
            }
        }
        notifyDataSetChanged();
    }

    public void addCustomName(String name) {
        ContactHolder contactHolder = new ContactHolder(name, true, true);
        contactHolderArrayList.add(0, contactHolder);
        contactHolderArrayListFiltered.add(0, contactHolder);
        notifyItemInserted(0);
        notifyItemRangeChanged(1, contactHolderArrayListFiltered.size());
        saveSelectedContactList();
    }

    @Override
    public int getItemCount() {
        return contactHolderArrayListFiltered.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String charString = constraint.toString();
                if (charString.isEmpty()) {
                    contactHolderArrayListFiltered = new ArrayList<>(contactHolderArrayList);
                } else {
                    ArrayList<ContactHolder> filteredList = new ArrayList<>();
                    for (ContactHolder row : contactHolderArrayList) {
                        if (row.getContactName().toLowerCase().contains(charString.toLowerCase()) ||
                                (row.getPhoneNumber() != null && row.getPhoneNumber().replaceAll("\\s", "").contains(charString))) {
                            filteredList.add(row);
                        }
                    }
                    contactHolderArrayListFiltered = filteredList;
                }
                FilterResults filterResults = new FilterResults();
                filterResults.values = contactHolderArrayListFiltered;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                contactHolderArrayListFiltered = (ArrayList<ContactHolder>) results.values;
                notifyDataSetChanged();
            }
        };
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ContactListRowBinding binding;

        public ViewHolder(@NonNull @NotNull ContactListRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public ContactListRowBinding getBinding() {
            return binding;
        }
    }

    static class CustomHolder extends RecyclerView.ViewHolder {
        private final CustomContactListRowBinding binding;

        public CustomHolder(@NonNull @NotNull CustomContactListRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public CustomContactListRowBinding getBinding() {
            return binding;
        }
    }

}
