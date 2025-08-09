package com.parishod.watomatic.model.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.radiobutton.MaterialRadioButton
import com.parishod.watomatic.R
import com.parishod.watomatic.model.data.MessageTypeItem

class MessageTypeAdapter(
    private val items: List<MessageTypeItem>,
    private val onSelect: (Int, Boolean) -> Unit
) : RecyclerView.Adapter<MessageTypeAdapter.ViewHolder>() {

    private var selectedPosition = items.indexOfFirst { it.isSelected }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.item_title)
        val radioButton: MaterialRadioButton = view.findViewById(R.id.item_radio)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_radio_option, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.title.text = item.title
        holder.radioButton.isChecked = position == selectedPosition

        holder.itemView.setOnClickListener {
            val previousSelected = selectedPosition
            selectedPosition = position
            notifyItemChanged(previousSelected)
            notifyItemChanged(selectedPosition)
            onSelect(position, true)
        }

        holder.radioButton.setOnClickListener {
            holder.itemView.performClick()
        }
    }

    override fun getItemCount(): Int = items.size
}