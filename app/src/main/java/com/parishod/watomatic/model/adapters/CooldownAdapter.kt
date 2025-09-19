package com.parishod.watomatic.model.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButtonToggleGroup
import com.parishod.watomatic.R
import com.parishod.watomatic.model.data.CooldownItem

class CooldownAdapter(
    private val items: List<CooldownItem>,
    private val onCooldownTimeChanged: (Int) -> Unit
) : RecyclerView.Adapter<CooldownAdapter.ViewHolder>() {

    private var selectedHour: Int = 0
    private var selectedMinute: Int = 1
    private var isHoursSelected: Boolean = false
    private var isReset: Boolean = false

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val toggleButtonGroup: MaterialButtonToggleGroup = view.findViewById(R.id.toggle_button_group)
        val numberPicker: NumberPicker = view.findViewById(R.id.numberPicker)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cooldown_option, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (!isReset) {
            items.getOrNull(position)?.let {
                val totalMinutes = it.cooldownInMinutes
                if (totalMinutes >= 60 && totalMinutes % 60 == 0) {
                    isHoursSelected = true
                    selectedHour = totalMinutes / 60
                } else {
                    isHoursSelected = false
                    selectedMinute = totalMinutes
                }
            }
        }
        isReset = false

        holder.toggleButtonGroup.check(if (isHoursSelected) R.id.button_hours else R.id.button_minutes)
        setupNumberPicker(holder)

        holder.toggleButtonGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                isHoursSelected = checkedId == R.id.button_hours
                setupNumberPicker(holder)
                notifyTimeChange()
            }
        }

        holder.buttonResetTimer.setOnClickListener {
            selectedHour = 0
            selectedMinute = 0
            setupNumberPicker(holder)
            notifyTimeChange()
        }
        notifyTimeChange()
    }

    private fun setupNumberPicker(holder: ViewHolder) {
        holder.numberPicker.apply {
            minValue = 0
            maxValue = if (isHoursSelected) 24 else 59
            value = if (isHoursSelected) selectedHour else selectedMinute
            wrapSelectorWheel = true
            setOnValueChangedListener { _, _, newVal ->
                if (isHoursSelected) {
                    selectedHour = newVal
                } else {
                    selectedMinute = newVal
                }
                notifyTimeChange()
            }
        }
    }

    private fun notifyTimeChange() {
        val totalMinutes = if (isHoursSelected) selectedHour * 60 else selectedMinute
        onCooldownTimeChanged(totalMinutes)
    }

    override fun getItemCount(): Int = items.size

    fun reset() {
        isReset = true
        selectedHour = 0
        selectedMinute = 0
        notifyDataSetChanged()
    }
}