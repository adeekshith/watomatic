package com.parishod.watomatic.activity.subscription

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class SubscriptionPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> SubscriptionPlansFragment.newInstance(SubscriptionPlansFragment.PLAN_TYPE_MONTHLY)
            1 -> SubscriptionPlansFragment.newInstance(SubscriptionPlansFragment.PLAN_TYPE_ANNUAL)
            else -> throw IllegalStateException("Invalid position: $position")
        }
    }
}

