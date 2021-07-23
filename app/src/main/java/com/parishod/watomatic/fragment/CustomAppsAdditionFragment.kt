package com.parishod.watomatic.fragment

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.UserManager
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.parishod.watomatic.R
import com.parishod.watomatic.adapter.InstalledAppsAdapter
import com.parishod.watomatic.model.logs.App
import com.parishod.watomatic.model.utils.DbUtils
import kotlinx.android.synthetic.main.fragment_custom_apps.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList

class CustomAppsAdditionFragment: Fragment(), InstalledAppsAdapter.ItemClickListener  {
    lateinit var fragmentView: View
    lateinit var installedApps: List<App>
    lateinit var installedAppsAdapter: InstalledAppsAdapter
    lateinit var saveMenu: MenuItem
    var newlyAddedApps: MutableList<App> = ArrayList()
    val dbUtils: DbUtils by lazy {
        DbUtils(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentView = inflater.inflate(R.layout.fragment_custom_apps, container, false)
        fragmentView.shimmerFrameLayout.startShimmerAnimation()

        CoroutineScope(Dispatchers.Main).launch {
            installedApps = getInstalledApps(context)
            showInstalledApps()
        }

        return fragmentView
    }

    private fun showInstalledApps() {
        fragmentView.shimmerFrameLayout.stopShimmerAnimation()
        fragmentView.shimmerFrameLayout.visibility = View.GONE

        val layoutManager = LinearLayoutManager(context)
        installedAppsAdapter = InstalledAppsAdapter(this@CustomAppsAdditionFragment as InstalledAppsAdapter.ItemClickListener, installedApps)
        fragmentView.installedAppsList.layoutManager = layoutManager
        fragmentView.installedAppsList.adapter = installedAppsAdapter
    }

    private suspend fun getInstalledApps(context: Context?): List<App> =
        withContext(Dispatchers.Default) {
            val apps: MutableList<App> = ArrayList<App>()
            context?.let {
                val pm: PackageManager = context.packageManager
                val manager = context.getSystemService(Context.USER_SERVICE) as UserManager?
                val launcher = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps?

                // Handle multi-profile support introduced in Android 5
                manager?.let {
                    launcher?.let {
                        for (profile in manager.userProfiles) {
                            for (activityInfo in launcher.getActivityList(null, profile)) {
                                val i = Intent(Intent.ACTION_MAIN)
                                i.component = activityInfo.componentName
                                i.addCategory(Intent.CATEGORY_LAUNCHER)
                                val resolveInfo = pm.resolveActivity(i, 0)
                                resolveInfo?.let {
                                    if((resolveInfo.activityInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 1) {
                                        val app = App(resolveInfo.loadLabel(pm) as String, resolveInfo.activityInfo.packageName)//AppInfo(resolveInfo.loadLabel(pm) as String, resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.loadIcon(pm))
                                        if(!apps.contains(app)) {
                                            apps.add(app)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return@withContext apps.sortedBy { it.name }
        }

    override fun itemClick(newlyAddedApps: List<App>) {
        this.newlyAddedApps.clear()
        this.newlyAddedApps.addAll(newlyAddedApps)
    }
}