package com.parishod.watomatic.billing

import android.app.Activity
import android.content.Context

class BillingManagerImpl(private val context: Context) : BillingManager {
    override fun startConnection() {
        // No-op
    }

    override fun purchase(activity: Activity, skuId: String) {
        // No-op
    }

    override fun queryPurchases() {
        // No-op
    }

    override fun destroy() {
        // No-op
    }
}
