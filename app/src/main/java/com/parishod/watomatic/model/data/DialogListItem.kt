package com.parishod.watomatic.model.data

import com.parishod.watomatic.model.interfaces.DialogItem
import kotlinx.parcelize.Parcelize

sealed class DialogListItem : DialogItem {
    @Parcelize
    data class SectionHeader(val title: String) : DialogListItem()

    @Parcelize
    data class AppItemWrapper(val appItem: AppItem) : DialogListItem()
}

