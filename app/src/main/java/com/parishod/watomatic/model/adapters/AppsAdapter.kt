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
import com.parishod.watomatic.model.preferences.PreferencesManager

class AppsAdapter(
    private val items: List<AppItem>,
    private val onToggle: (Int, Boolean) -> Unit
) : RecyclerView.Adapter<AppsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.item_icon)
        val name: TextView = view.findViewById(R.id.item_name)
        val status: TextView = view.findViewById(R.id.item_status)
        val toggle: MaterialSwitch = view.findViewById(R.id.item_toggle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_toggle, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        var icon: Drawable? = null
        try {
            icon = holder.icon.context.packageManager.getApplicationIcon(item.packageName)
        } catch (ignore: PackageManager.NameNotFoundException) {
        }
        if(icon != null){
            holder.icon.setImageDrawable(icon)
        }else {
            holder.icon.setImageResource(item.iconRes)
        }
        holder.name.text = item.name
        holder.status.text = item.status
        holder.toggle.isChecked = PreferencesManager.getPreferencesInstance(holder.icon.context).isAppEnabled(item.packageName)

        holder.toggle.setOnCheckedChangeListener { _, isChecked ->
            PreferencesManager.getPreferencesInstance(holder.icon.context).saveEnabledApps(item.packageName, isChecked)
            onToggle(position, isChecked)
        }
    }

    override fun getItemCount(): Int = items.size
}