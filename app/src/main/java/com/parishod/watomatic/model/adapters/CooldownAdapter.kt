package com.parishod.watomatic.model.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.parishod.watomatic.R
import com.parishod.watomatic.model.data.CooldownItem

class CooldownAdapter(
    private var initialTimeInMillis: Long,
    private val items: List<CooldownItem>,
    private val onCooldownTimeChanged: (Int) -> Unit
) : RecyclerView.Adapter<CooldownAdapter.ViewHolder>() {

    private var selectedHour: Int = 0
    private var selectedMinute: Int = 1
    private var isHoursSelected: Boolean = false

    // Track selection separately but only one is active at a time
    private var lastSelectedHourPos: Int = -1
    private var lastSelectedMinutePos: Int = -1

    inner class TimeAdapter(
        private val timeValues: List<Int>,
        private val onTimeSelected: (Int) -> Unit
    ) : RecyclerView.Adapter<TimeAdapter.TimeViewHolder>() {

        private var selectedPosition = -1

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
                holder.itemView.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.primary)
                )
                holder.timeValue.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.text_primary)
                )
            } else {
                holder.itemView.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.unselected_item_text)
                )
                holder.timeValue.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.text_primary)
                )
            }

            holder.itemView.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = holder.adapterPosition
                if (previousPosition != selectedPosition) {
                    if (previousPosition != -1) notifyItemChanged(previousPosition)
                    notifyItemChanged(selectedPosition)
                }
                onTimeSelected(time)
            }
        }

        override fun getItemCount(): Int = timeValues.size

        fun setSelectedPosition(position: Int) {
            val prev = selectedPosition
            selectedPosition = position
            if (prev != -1) notifyItemChanged(prev)
            if (selectedPosition != -1) notifyItemChanged(selectedPosition)
        }

        fun clearSelection() {
            val prev = selectedPosition
            selectedPosition = -1
            if (prev != -1) notifyItemChanged(prev)
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val toggleButtonGroup: MaterialButtonToggleGroup =
            view.findViewById(R.id.toggle_button_group)
        val timeRecyclerView: RecyclerView = view.findViewById(R.id.recycler_view_time)
        val buttonResetTimer: MaterialButton = view.findViewById(R.id.button_reset_timer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cooldown_option, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        items.getOrNull(position)?.let {
            val totalMinutes = it.cooldownInMinutes
            if (totalMinutes >= 60 && totalMinutes % 60 == 0) {
                isHoursSelected = true
                selectedHour = totalMinutes / 60
                lastSelectedHourPos = selectedHour - 1
                lastSelectedMinutePos = -1
            } else {
                isHoursSelected = false
                selectedMinute = totalMinutes
                lastSelectedMinutePos = selectedMinute - 1
                lastSelectedHourPos = -1
            }
        }

        holder.toggleButtonGroup.check(if (isHoursSelected) R.id.button_hours else R.id.button_minutes)
        setupTimeRecyclerView(holder)

        holder.toggleButtonGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                isHoursSelected = checkedId == R.id.button_hours
                val totalMinutes = if (isHoursSelected) selectedHour * 60 else selectedMinute
                initialTimeInMillis = totalMinutes * 60 * 1000L
                setupTimeRecyclerView(holder)
            }
        }
        holder.buttonResetTimer.setOnClickListener {
            selectedHour = 0
            lastSelectedHourPos = -1

            selectedMinute = 0
            lastSelectedMinutePos = -1

            setupTimeRecyclerView(holder)
            notifyTimeChange()
        }
        notifyTimeChange()
    }

    private fun setupTimeRecyclerView(holder: ViewHolder) {
        val timeValues = if (isHoursSelected) (1..24).toList() else (1..59).toList()
        val timeAdapter = TimeAdapter(timeValues) { time ->
            if (isHoursSelected) {
                selectedHour = time
                lastSelectedHourPos = timeValues.indexOf(time)
                lastSelectedMinutePos = -1 // clear minutes selection
            } else {
                selectedMinute = time
                lastSelectedMinutePos = timeValues.indexOf(time)
                lastSelectedHourPos = -1 // clear hours selection
            }
            notifyTimeChange()
        }

        // Apply only the active selection
        val selectedPos = if (isHoursSelected) lastSelectedHourPos else lastSelectedMinutePos
        if (selectedPos != -1) {
            timeAdapter.setSelectedPosition(selectedPos)
        } else {
            timeAdapter.clearSelection()
        }

        holder.timeRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = timeAdapter
        }
    }

    private fun notifyTimeChange() {
        val totalMinutes = if (isHoursSelected && lastSelectedHourPos != -1) {
            selectedHour * 60
        } else if (!isHoursSelected && lastSelectedMinutePos != -1) {
            selectedMinute
        } else {
            0
        }
        onCooldownTimeChanged(totalMinutes)
    }

    override fun getItemCount(): Int = if (items.isEmpty()) 1 else items.size
}