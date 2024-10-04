package com.parishod.watomatic.adapter

import android.content.pm.PackageManager
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.parishod.watomatic.R
import com.parishod.watomatic.databinding.EnabledAppsGridItemBinding
import com.parishod.watomatic.databinding.SupportedAppsListBinding
import com.parishod.watomatic.model.App
import com.parishod.watomatic.model.preferences.PreferencesManager
import com.parishod.watomatic.model.utils.Constants


class SupportedAppsAdapter(private val listType: Constants.EnabledAppsDisplayType, private var supportedAppsList: List<App>, private var onClickListener: View.OnClickListener?) : RecyclerView.Adapter<SupportedAppsAdapter.AppsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppsViewHolder {
        val binding = if (listType == Constants.EnabledAppsDisplayType.VERTICAL) {
            SupportedAppsListBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)
        } else {
            EnabledAppsGridItemBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)
        }
        return AppsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppsViewHolder, position: Int) {
        holder.setData(supportedAppsList[position], onClickListener)
    }

    override fun getItemCount(): Int {
        return supportedAppsList.size
    }

    fun updateList(supportedAppsList: List<App>) {
        this.supportedAppsList = supportedAppsList
        notifyDataSetChanged()
    }

    class AppsViewHolder(private val viewBinding: ViewBinding) : RecyclerView.ViewHolder(viewBinding.root) {

        fun setData(app: App, onClickListener: View.OnClickListener?) {
            try {
                val icon: Drawable = itemView.context.packageManager.getApplicationIcon(app.packageName)
                when (viewBinding) {
                    is EnabledAppsGridItemBinding -> {
                        viewBinding.appIcon.setImageDrawable(icon)
                    }
                    is SupportedAppsListBinding -> {
                        viewBinding.appIcon.setImageDrawable(icon)
                    }
                }
                onClickListener?.let {
                    itemView.setOnClickListener {
                        onClickListener.onClick(it)
                    }
                }
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
                if (viewBinding is SupportedAppsListBinding) {
                    val matrix = ColorMatrix()
                    matrix.setSaturation(0f) //0 means grayscale
                    val cf = ColorMatrixColorFilter(matrix)
                    viewBinding.appIcon.colorFilter = cf
                    viewBinding.appEnableSwitch.setOnClickListener {
                        Toast.makeText(itemView.context, itemView.context.resources.getString(R.string.app_not_installed_text), Toast.LENGTH_SHORT).show()
                        viewBinding.appEnableSwitch.isChecked = false
                    }
                    viewBinding.appIcon.setOnClickListener {
                        Toast.makeText(itemView.context, itemView.context.resources.getString(R.string.app_not_installed_text), Toast.LENGTH_SHORT).show()
                    }
                }
            }

            if (viewBinding is SupportedAppsListBinding) {
                viewBinding.appEnableSwitch.text = app.name
                viewBinding.appEnableSwitch.tag = app
                viewBinding.appEnableSwitch.isChecked = PreferencesManager.getPreferencesInstance(itemView.context).isAppEnabled(app)
                viewBinding.appEnableSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                    val preferencesManager = PreferencesManager.getPreferencesInstance(itemView.context)
                    if (!isChecked && preferencesManager.enabledApps.size <= 1) { // Keep at-least one app selected
                        // Keep at-least one app selected
                        Toast.makeText(
                            itemView.context,
                            itemView.context.resources.getString(R.string.error_atleast_single_app_must_be_selected),
                            Toast.LENGTH_SHORT
                        ).show()
                        buttonView.isChecked = true
                    } else {
                        preferencesManager.saveEnabledApps(buttonView.tag as App, isChecked)
                    }
                }
            }
        }
    }
}