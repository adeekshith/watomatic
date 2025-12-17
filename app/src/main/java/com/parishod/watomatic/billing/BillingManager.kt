package com.parishod.watomatic.billing

import android.app.Activity

interface BillingManager {
    fun startConnection()
    fun purchase(activity: Activity, skuId: String)
    fun queryPurchases()
    fun destroy()
}
