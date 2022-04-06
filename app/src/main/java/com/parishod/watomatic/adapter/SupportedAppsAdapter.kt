package com.parishod.watomatic.adapter

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.pm.PackageManager
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.parishod.watomatic.R
import com.parishod.watomatic.model.logs.App
import com.parishod.watomatic.model.preferences.PreferencesManager
import com.parishod.watomatic.model.utils.Constants
import kotlinx.android.synthetic.main.supported_apps_list.view.*


class SupportedAppsAdapter(private val listType: Constants.EnabledAppsDisplayType, private var supportedAppsList: List<App>, private var onClickListener: View.OnClickListener?) : RecyclerView.Adapter<SupportedAppsAdapter.AppsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppsViewHolder {
        val itemView = if (listType == Constants.EnabledAppsDisplayType.VERTICAL) {
            LayoutInflater.from(parent.context)
                    .inflate(R.layout.supported_apps_list, parent, false)
        } else {
            LayoutInflater.from(parent.context)
                    .inflate(R.layout.enabled_apps_grid_item, parent, false)
        }
        return AppsViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AppsViewHolder, position: Int) {
        holder.setData(supportedAppsList[position], listType, onClickListener)
    }

    override fun getItemCount(): Int {
        return supportedAppsList.size
    }

    fun updateList(supportedAppsList: List<App>) {
        this.supportedAppsList = supportedAppsList
        notifyDataSetChanged()
    }

    class AppsViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        fun setData(app: App, listType: Constants.EnabledAppsDisplayType, onClickListener: View.OnClickListener?) {
            var isAppInstalled = true;
            try {
                val icon: Drawable = itemView.context.packageManager.getApplicationIcon(app.packageName)
                itemView.appIcon.setImageDrawable(icon)
                onClickListener?.let {
                    itemView.setOnClickListener {
                        onClickListener.onClick(it)
                    }
                }
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
                isAppInstalled = false
                if (listType == Constants.EnabledAppsDisplayType.VERTICAL) {
                    val matrix = ColorMatrix()
                    matrix.setSaturation(0f) //0 means grayscale
                    val cf = ColorMatrixColorFilter(matrix)
                    itemView.appIcon.colorFilter = cf

                    (itemView.appEnableSwitch as SwitchMaterial).setOnClickListener {
                        showSnackBar(itemView, app.packageName, itemView.context.resources.getString(R.string.app_not_installed_text))
                        itemView.appEnableSwitch.isChecked = false
                        PreferencesManager.getPreferencesInstance(itemView.context).saveEnabledApps(itemView.appEnableSwitch.tag as App, itemView.appEnableSwitch.isChecked)
                    }
                    itemView.appIcon.setOnClickListener {
                        showSnackBar(itemView, app.packageName, itemView.context.resources.getString(R.string.app_not_installed_text))
                        itemView.appEnableSwitch.isChecked = false
                        PreferencesManager.getPreferencesInstance(itemView.context).saveEnabledApps(itemView.appEnableSwitch.tag as App, itemView.appEnableSwitch.isChecked)
                    }
                }
            }

            if (listType == Constants.EnabledAppsDisplayType.VERTICAL) {
                itemView.appEnableSwitch.text = app.name
                itemView.appEnableSwitch.tag = app
                itemView.appEnableSwitch.isChecked = PreferencesManager.getPreferencesInstance(itemView.context).isAppEnabled(app)
                if(isAppInstalled) {
                    (itemView.appEnableSwitch as SwitchMaterial).setOnClickListener {
                        val preferencesManager =
                            PreferencesManager.getPreferencesInstance(itemView.context)
                        if (!itemView.appEnableSwitch.isChecked && preferencesManager.enabledApps.size <= 1) { // Keep at-least one app selected
                            // Keep at-least one app selected
                            Toast.makeText(
                                itemView.context,
                                itemView.context.resources.getString(R.string.error_atleast_single_app_must_be_selected),
                                Toast.LENGTH_SHORT
                            ).show()
                            itemView.appEnableSwitch.isChecked = true
                        }else {
                            preferencesManager.saveEnabledApps(
                                itemView.appEnableSwitch.tag as App,
                                itemView.appEnableSwitch.isChecked
                            )
                        }
                    }
                }
            }
        }

        private fun showSnackBar(itemView: View, packageName: String, msg: String) {
            val snackBar = Snackbar.make(itemView.rootView.findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG)
            snackBar.setAction(itemView.context.resources.getString(R.string.install)) {
                itemView.context.startActivity(
                    Intent(
                        ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                    )
                )
            }
            snackBar.show()
        }
    }
}