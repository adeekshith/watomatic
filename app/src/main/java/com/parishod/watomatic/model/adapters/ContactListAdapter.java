package com.parishod.watomatic.model.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.parishod.watomatic.databinding.ContactListRowBinding;
import com.parishod.watomatic.model.data.ContactHolder;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class ContactListAdapter extends RecyclerView.Adapter<ContactListAdapter.ViewHolder> {
    private final ArrayList<ContactHolder> contactHolderArrayList;

    public ContactListAdapter(ArrayList<ContactHolder> contactHolderArrayList) {
        this.contactHolderArrayList = contactHolderArrayList;
    }

    @NonNull
    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        ContactListRowBinding binding = ContactListRowBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull ViewHolder holder, int position) {
        ContactListRowBinding binding = holder.getBinding();
        binding.contactCheckbox.setChecked(contactHolderArrayList.get(position).isChecked());
        binding.contactCheckbox.setText(contactHolderArrayList.get(position).getContactName());
        binding.contactCheckbox.setOnCheckedChangeListener((buttonView, isChecked) ->
                contactHolderArrayList.get(position).setChecked(isChecked));
    }

    @Override
    public void onViewRecycled(@NonNull @NotNull ViewHolder holder) {
        ContactListRowBinding binding = holder.getBinding();
        binding.contactCheckbox.setOnCheckedChangeListener(null);
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return contactHolderArrayList.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ContactListRowBinding binding;
        public ViewHolder(@NonNull @NotNull ContactListRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public ContactListRowBinding getBinding() {
            return binding;
        }
    }

}
