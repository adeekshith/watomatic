package com.parishod.watomatic.activity.subscription

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.parishod.watomatic.R
import com.parishod.watomatic.activity.BaseActivity
import com.parishod.watomatic.model.preferences.PreferencesManager

class SubscriptionInfoActivity : BaseActivity() {
    private var preferencesManager: PreferencesManager? = null
    private var monthlyPlanCard: MaterialCardView? = null
    private var annualPlanCard: MaterialCardView? = null
    private var subscribeButton: Button? = null
    private var statusTextView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_subscription_info)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.subscription_info_title)

        preferencesManager = PreferencesManager.getPreferencesInstance(this)

        initializeViews()
        loadSubscriptionStatus()
        setupClickListeners()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.subscription_scroll_view)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initializeViews() {
        monthlyPlanCard = findViewById(R.id.monthly_plan_card)
        annualPlanCard = findViewById(R.id.annual_plan_card)
        subscribeButton = findViewById(R.id.subscribe_button)
        statusTextView = findViewById(R.id.subscription_status)
    }

    private fun loadSubscriptionStatus() {
        // TODO: Implement actual subscription status check
        // For now, show a placeholder status
        val userEmail = preferencesManager?.userEmail ?: ""

        if (userEmail.isNotEmpty()) {
            statusTextView?.text = getString(R.string.subscription_status_logged_in, userEmail)
            statusTextView?.visibility = View.VISIBLE
        } else {
            statusTextView?.text = getString(R.string.subscription_status_no_subscription)
            statusTextView?.visibility = View.VISIBLE
        }
    }

    private fun setupClickListeners() {
        monthlyPlanCard?.setOnClickListener {
            // TODO: Handle monthly plan selection
            selectPlan("monthly")
        }

        annualPlanCard?.setOnClickListener {
            // TODO: Handle annual plan selection
            selectPlan("annual")
        }

        subscribeButton?.setOnClickListener {
            // TODO: Implement subscription flow
            handleSubscription()
        }
    }

    private fun selectPlan(planType: String) {
        // Reset both cards
        monthlyPlanCard?.strokeWidth = 0
        annualPlanCard?.strokeWidth = 0

        // Highlight selected plan
        val selectedCard = if (planType == "monthly") monthlyPlanCard else annualPlanCard
        selectedCard?.strokeWidth = 4
        selectedCard?.strokeColor = getColor(R.color.primary)
    }

    private fun handleSubscription() {
        // TODO: Implement actual subscription flow with payment
        // This would typically integrate with Google Play Billing or similar
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}

