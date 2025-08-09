package com.parishod.watomatic.model.adapters

import com.parishod.watomatic.model.data.CooldownItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.parishod.watomatic.R

class CooldownAdapter(
    private val items: List<CooldownItem>,
    private val onSelect: (Int, Boolean) -> Unit
) : RecyclerView.Adapter<CooldownAdapter.ViewHolder>() {

    private var selectedPosition = items.indexOfFirst { it.isSelected }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.item_title)
        val container: LinearLayout = view.findViewById(R.id.item_container)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cooldown_option, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.title.text = item.title

        // Update background based on selection
        if (position == selectedPosition) {
            holder.container.setBackgroundResource(R.drawable.selected_option_background)
        } else {
            holder.container.setBackgroundResource(R.drawable.unselected_option_background)
        }

        holder.itemView.setOnClickListener {
            val previousSelected = selectedPosition
            selectedPosition = position
            notifyItemChanged(previousSelected)
            notifyItemChanged(selectedPosition)
            onSelect(position, true)
        }
    }

    override fun getItemCount(): Int = items.size
}