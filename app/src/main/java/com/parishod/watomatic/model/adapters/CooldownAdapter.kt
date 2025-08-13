package com.parishod.watomatic.model.adapters

import com.parishod.watomatic.model.data.CooldownItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.NumberPicker
import com.parishod.watomatic.R

class CooldownAdapter(
    private val items: List<CooldownItem>,
    private val onTimeChanged: (Int) -> Unit
) : RecyclerView.Adapter<CooldownAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.item_title)
        val container: LinearLayout = view.findViewById(R.id.item_container)
        val valuePicker: NumberPicker = view.findViewById(R.id.picker_value)
        val unitPicker: NumberPicker = view.findViewById(R.id.picker_unit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cooldown_option, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.title.text = items.getOrNull(position)?.title
            ?: holder.itemView.context.getString(R.string.reply_cooldown)

        // Value picker: 1..120
        holder.valuePicker.minValue = 1
        holder.valuePicker.maxValue = 120
        holder.valuePicker.wrapSelectorWheel = true

        // Unit picker: mins/hrs
        val unitValues = arrayOf(
            holder.itemView.context.getString(R.string.minutes),
            holder.itemView.context.getString(R.string.hours)
        )
        holder.unitPicker.minValue = 0
        holder.unitPicker.maxValue = unitValues.size - 1
        holder.unitPicker.displayedValues = unitValues
        holder.unitPicker.wrapSelectorWheel = true

        // Defaults: 10 minutes
        if (holder.valuePicker.value == 0) holder.valuePicker.value = 10
        if (holder.unitPicker.value !in 0..1) holder.unitPicker.value = 0

        val notifyChange = {
            val value = holder.valuePicker.value
            val isHours = holder.unitPicker.value == 1
            val totalMinutes = if (isHours) value * 60 else value
            onTimeChanged(totalMinutes)
        }

        holder.valuePicker.setOnValueChangedListener { _, _, _ -> notifyChange() }
        holder.unitPicker.setOnValueChangedListener { _, _, _ -> notifyChange() }

        // Trigger initial callback
        notifyChange()
    }

    override fun getItemCount(): Int = if (items.isEmpty()) 1 else items.size
}