package com.parishod.watomatic.model.data;

public class ContactHolder {
    private final String contactName;
    private final String phoneNumber;
    private boolean isChecked;
    private boolean isCustom;

    public ContactHolder(String contactName, String phoneNumber, boolean isChecked) {
        this.contactName = contactName;
        this.phoneNumber = phoneNumber;
        this.isChecked = isChecked;
        this.isCustom = false;
    }

    public ContactHolder(String contactName, boolean isChecked, boolean isCustom) {
        this.contactName = contactName;
        this.isChecked = isChecked;
        this.isCustom = isCustom;
        this.phoneNumber = null;
    }

    public String getContactName() {
        return contactName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public boolean isCustom() {
        return isCustom;
    }

    public void setCustom(boolean custom) {
        isCustom = custom;
    }
}
