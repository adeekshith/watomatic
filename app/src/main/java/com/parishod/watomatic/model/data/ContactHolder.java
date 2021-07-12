package com.parishod.watomatic.model.data;

public class ContactHolder {
    private final String contactName;
    private boolean isChecked;
    private boolean isCustom;

    public ContactHolder(String contactName, boolean isChecked) {
        this.contactName = contactName;
        this.isChecked = isChecked;
        this.isCustom = false;
    }

    public ContactHolder(String contactName, boolean isChecked, boolean isCustom) {
        this.contactName = contactName;
        this.isChecked = isChecked;
        this.isCustom = isCustom;
    }

    public String getContactName() {
        return contactName;
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
