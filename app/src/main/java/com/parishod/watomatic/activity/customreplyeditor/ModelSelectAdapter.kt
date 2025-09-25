package com.parishod.watomatic.activity.customreplyeditor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.radiobutton.MaterialRadioButton
import com.parishod.watomatic.R
import com.parishod.watomatic.network.model.openai.ModelData

class ModelSelectAdapter(
    private var models: List<ModelData>,
    private var selectedModelId: String?,
    private val onModelSelected: (ModelData) -> Unit
) : RecyclerView.Adapter<ModelSelectAdapter.ModelViewHolder>() {

    private var selectedPos = models.indexOfFirst { it.id == selectedModelId }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_radio_option, parent, false)
        return ModelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ModelViewHolder, position: Int) {
        val model = models[position]
        holder.title.text = model.id
        holder.radio.isChecked = position == selectedPos
        holder.itemView.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
            val prevSelected = selectedPos
            selectedPos = pos
            if (prevSelected >= 0) notifyItemChanged(prevSelected)
            notifyItemChanged(selectedPos)
            onModelSelected.invoke(models[selectedPos])
        }
    }

    override fun getItemCount(): Int = models.size

    fun setModels(newModels: List<ModelData>, selectedModelId: String?) {
        models = newModels
        selectedPos = models.indexOfFirst { it.id == selectedModelId }
        notifyDataSetChanged()
    }

    class ModelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.item_title)
        val radio: MaterialRadioButton = itemView.findViewById(R.id.item_radio)
    }
}
