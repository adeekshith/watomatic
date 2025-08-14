package com.parishod.watomatic.model.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButtonToggleGroup
import com.parishod.watomatic.R
import com.parishod.watomatic.model.data.CooldownItem

class CooldownAdapter(
    private val items: List<CooldownItem>,
    private val onTimeChanged: (Int) -> Unit
) : RecyclerView.Adapter<CooldownAdapter.ViewHolder>() {

    private var selectedTimeValue: Int = 10 // Default to 10 minutes
    private var isHoursSelected: Boolean = false

    inner class TimeAdapter(
        private val timeValues: List<Int>,
        private val onTimeSelected: (Int) -> Unit
    ) : RecyclerView.Adapter<TimeAdapter.TimeViewHolder>() {

        private var selectedPosition = timeValues.indexOf(selectedTimeValue)

        inner class TimeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val timeValue: TextView = view.findViewById(R.id.time_value)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_cooldown_time, parent, false)
            return TimeViewHolder(view)
        }

        override fun onBindViewHolder(holder: TimeViewHolder, position: Int) {
            val time = timeValues[position]
            holder.timeValue.text = time.toString()

            if (position == selectedPosition) {
                holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.selected_item_background))
                holder.timeValue.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.text_primary))
            } else {
                holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, android.R.color.transparent))
                holder.timeValue.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.unselected_item_text))
            }

            holder.itemView.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = holder.adapterPosition
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)
                onTimeSelected(time)
            }
        }

        override fun getItemCount(): Int = timeValues.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val toggleButtonGroup: MaterialButtonToggleGroup = view.findViewById(R.id.toggle_button_group)
        val timeRecyclerView: RecyclerView = view.findViewById(R.id.recycler_view_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cooldown_option, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Restore state from CooldownItem if available
        items.getOrNull(position)?.let {
            val totalMinutes = it.cooldownInMinutes
            if (totalMinutes >= 60 && totalMinutes % 60 == 0) {
                isHoursSelected = true
                selectedTimeValue = totalMinutes / 60
            } else {
                isHoursSelected = false
                selectedTimeValue = totalMinutes
            }
        }

        holder.toggleButtonGroup.check(if (isHoursSelected) R.id.button_hours else R.id.button_minutes)
        setupTimeRecyclerView(holder)

        holder.toggleButtonGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                isHoursSelected = checkedId == R.id.button_hours
                setupTimeRecyclerView(holder)
                notifyTimeChange()
            }
        }
        notifyTimeChange()
    }

    private fun setupTimeRecyclerView(holder: ViewHolder) {
        val timeValues = if (isHoursSelected) (1..24).toList() else (1..60).toList()
        if (!timeValues.contains(selectedTimeValue)) {
            selectedTimeValue = timeValues[0]
        }
        val timeAdapter = TimeAdapter(timeValues) { time ->
            selectedTimeValue = time
            notifyTimeChange()
        }
        holder.timeRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = timeAdapter
            // Scroll to selected position
            val selectedIndex = timeValues.indexOf(selectedTimeValue)
            if (selectedIndex != -1) {
                (layoutManager as LinearLayoutManager).scrollToPositionWithOffset(selectedIndex, 0)
            }
        }
    }

    private fun notifyTimeChange() {
        val totalMinutes = if (isHoursSelected) selectedTimeValue * 60 else selectedTimeValue
        onTimeChanged(totalMinutes)
    }

    override fun getItemCount(): Int = if (items.isEmpty()) 1 else items.size
}