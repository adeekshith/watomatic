package com.parishod.watomatic.model.adapters

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch
import com.parishod.watomatic.R
import com.parishod.watomatic.model.data.AppItem
import com.parishod.watomatic.model.data.DialogListItem
import com.parishod.watomatic.model.preferences.PreferencesManager

class AppsAdapter(
    private val items: List<DialogListItem>,
    private val onToggle: (Int, Boolean) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SECTION_HEADER = 0
        private const val VIEW_TYPE_APP = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is DialogListItem.SectionHeader -> VIEW_TYPE_SECTION_HEADER
            is DialogListItem.AppItemWrapper -> VIEW_TYPE_APP
        }
    }

    class SectionHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.sectionTitle)
    }

    class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.item_icon)
        val name: TextView = view.findViewById(R.id.item_name)
        val status: TextView = view.findViewById(R.id.item_status)
        val toggle: MaterialSwitch = view.findViewById(R.id.item_toggle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SECTION_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.section_header_item, parent, false)
                SectionHeaderViewHolder(view)
            }
            VIEW_TYPE_APP -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_app_toggle, parent, false)
                AppViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is DialogListItem.SectionHeader -> {
                (holder as SectionHeaderViewHolder).title.text = item.title
            }
            is DialogListItem.AppItemWrapper -> {
                val appHolder = holder as AppViewHolder
                val appItem = item.appItem

                var icon: Drawable? = null
                try {
                    icon = appHolder.icon.context.packageManager.getApplicationIcon(appItem.packageName)
                } catch (ignore: PackageManager.NameNotFoundException) {
                }
                if(icon != null){
                    appHolder.icon.setImageDrawable(icon)
                }else {
                    appHolder.icon.setImageResource(appItem.iconRes)
                }
                appHolder.name.text = appItem.name
                appHolder.toggle.isChecked = PreferencesManager.getPreferencesInstance(appHolder.icon.context).isAppEnabled(appItem.packageName)

                appHolder.toggle.setOnCheckedChangeListener { _, isChecked ->
                    PreferencesManager.getPreferencesInstance(appHolder.icon.context).saveEnabledApps(appItem.packageName, isChecked)
                    onToggle(position, isChecked)
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size
}