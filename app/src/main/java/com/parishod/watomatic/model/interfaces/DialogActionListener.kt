package com.parishod.watomatic.model.interfaces

import com.parishod.watomatic.model.enums.DialogType

interface DialogActionListener {
    fun onSaveClicked(dialogType: DialogType)
    fun onItemToggled(position: Int, isChecked: Boolean) {}
    fun onItemSelected(position: Int, isSelected: Boolean) {}
    fun onSearchQuery(query: String) {}
    fun onCooldownChanged(totalMinutes: Int) {}
}