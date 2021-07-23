package com.parishod.watomatic.adapter

import android.content.pm.PackageManager
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.parishod.watomatic.R
import com.parishod.watomatic.model.logs.App
import com.parishod.watomatic.model.utils.DbUtils
import kotlinx.android.synthetic.main.installed_apps_list.view.*
import kotlinx.android.synthetic.main.supported_apps_list.view.appIcon


class InstalledAppsAdapter(private var itemClickListener: ItemClickListener, private var installedAppsList: List<App>): RecyclerView.Adapter<InstalledAppsAdapter.AppsViewHolder>() {
    lateinit var dbUtils: DbUtils
    var newlyAddedApps: MutableList<App> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppsViewHolder {
        val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.installed_apps_list, parent, false)

        dbUtils = DbUtils(parent.context)
        newlyAddedApps = ArrayList<App>(dbUtils.supportedApps)

        return AppsViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AppsViewHolder, position: Int) {
        holder.setData(installedAppsList[position])
    }

    override fun getItemCount(): Int {
        return installedAppsList.size
    }

    fun updateList(installedAppsList: List<App>){
        this.installedAppsList = installedAppsList
        notifyDataSetChanged()
    }

    inner class AppsViewHolder(view: View) : RecyclerView.ViewHolder(view){

        fun setData(app: App){
            try {
                val icon: Drawable = itemView.context.packageManager.getApplicationIcon(app.packageName)
                itemView.appIcon.setImageDrawable(icon)
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
                val matrix = ColorMatrix()
                matrix.setSaturation(0f) //0 means grayscale
                val cf = ColorMatrixColorFilter(matrix)
                itemView.appIcon.colorFilter = cf
            }
            itemView.appName.text = app.name
            itemView.parentLayout.tag = app
            itemView.parentLayout.setOnClickListener {
                if(itemView.appCheckBox.visibility == View.VISIBLE){
                    itemView.appCheckBox.visibility = View.GONE
                    itemView.parentLayout.setBackgroundColor(itemView.context.resources.getColor(R.color.white))
                    newlyAddedApps.remove(app)
                }else{
                    itemView.appCheckBox.visibility = View.VISIBLE
                    itemView.parentLayout.setBackgroundColor(itemView.context.resources.getColor(R.color.platinum))
                    newlyAddedApps.add(app)
                }
                itemClickListener.itemClick(newlyAddedApps)
            }
            if(newlyAddedApps.contains(app)){
                itemView.appCheckBox.visibility = View.VISIBLE
                itemView.parentLayout.setBackgroundColor(itemView.context.resources.getColor(R.color.platinum))
            }else{
                itemView.appCheckBox.visibility = View.GONE
                itemView.parentLayout.setBackgroundColor(itemView.context.resources.getColor(R.color.white))
            }
        }
    }

    interface ItemClickListener{
        fun itemClick(newlyAddedApps: List<App>)
    }
}