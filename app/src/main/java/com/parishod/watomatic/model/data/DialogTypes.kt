package com.parishod.watomatic.model.data

import com.parishod.watomatic.model.interfaces.DialogItem
import kotlinx.parcelize.Parcelize

@Parcelize
data class AppItem(
    val iconRes: Int,
    val name: String,
    val packageName: String,
    val isEnabled: Boolean = false
) : DialogItem

@Parcelize
data class MessageTypeItem(
    val title: String,
    val isSelected: Boolean = false
) : DialogItem

@Parcelize
data class CooldownItem(
    val cooldownInMinutes: Int
) : DialogItem