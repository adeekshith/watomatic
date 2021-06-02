package com.parishod.watomatic.adapter

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.parishod.watomatic.R
import com.parishod.watomatic.model.App
import com.parishod.watomatic.model.preferences.PreferencesManager
import com.parishod.watomatic.model.utils.Constants
import kotlinx.android.synthetic.main.supported_apps_list.view.*


class SupportedAppsAdapter(listType:Constants.EnabledAppsDisplayType, supportedAppsList: ArrayList<App>): RecyclerView.Adapter<SupportedAppsAdapter.AppsViewHolder>() {
    private val listType = listType
    private var supportedAppsList: ArrayList<App> = supportedAppsList

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppsViewHolder {
        val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.supported_apps_list, parent, false)
        return AppsViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AppsViewHolder, position: Int) {
        holder.setData(supportedAppsList[position], listType)
    }

    override fun getItemCount(): Int {
        return supportedAppsList.size
    }

    fun updateList(supportedAppsList: ArrayList<App>){
        this.supportedAppsList = supportedAppsList
        notifyDataSetChanged()
    }

    class AppsViewHolder(view: View) : RecyclerView.ViewHolder(view){

        fun setData(app: App, listType: Constants.EnabledAppsDisplayType){
            try {
                val icon: Drawable = itemView.context.packageManager.getApplicationIcon(app.packageName)
                itemView.appIcon.setImageDrawable(icon)
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }

            if(listType == Constants.EnabledAppsDisplayType.VERTICAL){
                itemView.appEnableSwitch.text = app.name
                itemView.appEnableSwitch.tag = app
                itemView.appEnableSwitch.isChecked = PreferencesManager.getPreferencesInstance(itemView.context).isAppEnabled(app)
                itemView.appEnableSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                    PreferencesManager.getPreferencesInstance(itemView.context).saveEnabledApps(buttonView.tag as App, isChecked)
                }
            }else{
                itemView.appEnableSwitch.visibility = View.GONE
                val params = itemView.layoutParams as RecyclerView.LayoutParams
                params.width = ViewGroup.LayoutParams.WRAP_CONTENT
                itemView.layoutParams = params
            }
        }
    }
}