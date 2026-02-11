package com.parishod.watomatic.activity.subscription

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.android.billingclient.api.ProductDetails
import com.google.android.material.card.MaterialCardView
import com.parishod.watomatic.R
import com.parishod.watomatic.billing.BillingManager

class SubscriptionPlansFragment : Fragment() {

    private var planType: String = PLAN_TYPE_MONTHLY
    private var productDetailsMap: Map<String, ProductDetails>? = null
    private var onPlanSelectedListener: OnPlanSelectedListener? = null

    // Plan cards
    private var freePlanCard: MaterialCardView? = null
    private var miniPlanCard: MaterialCardView? = null
    private var standardPlanCard: MaterialCardView? = null
    private var proPlanCard: MaterialCardView? = null

    // Price text views
    private var miniPriceText: TextView? = null
    private var standardPriceText: TextView? = null
    private var proPriceText: TextView? = null

    // "Current Plan" badge on free plan
    private var freePlanPriceText: TextView? = null

    private var selectedCard: MaterialCardView? = null

    // Whether the FREE plan should be marked as current and disabled
    private var isFreePlanCurrent: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_subscription_plans, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get plan type from arguments
        planType = arguments?.getString(ARG_PLAN_TYPE) ?: PLAN_TYPE_MONTHLY

        // Initialize views
        freePlanCard = view.findViewById(R.id.free_plan_card)
        miniPlanCard = view.findViewById(R.id.mini_plan_card)
        standardPlanCard = view.findViewById(R.id.standard_plan_card)
        proPlanCard = view.findViewById(R.id.pro_plan_card)

        miniPriceText = view.findViewById(R.id.mini_price)
        standardPriceText = view.findViewById(R.id.standard_price)
        proPriceText = view.findViewById(R.id.pro_price)
        freePlanPriceText = view.findViewById(R.id.free_price)

        // Setup click listeners
        setupClickListeners()

        // Update prices if available
        productDetailsMap?.let { updatePrices(it) }

        // Apply FREE plan current state if set before view was created
        if (isFreePlanCurrent) {
            applyFreePlanCurrentState()
        }
    }

    private fun setupClickListeners() {
        freePlanCard?.setOnClickListener {
            // If FREE plan is marked as current (UPGRADE mode), ignore clicks
            if (isFreePlanCurrent) return@setOnClickListener
            selectPlan(it as MaterialCardView, null, "free")
        }

        miniPlanCard?.setOnClickListener {
            val sku = if (planType == PLAN_TYPE_MONTHLY) {
                BillingManager.SKU_MONTHLY_MINI
            } else {
                BillingManager.SKU_ANNUAL_MINI
            }
            selectPlan(it as MaterialCardView, sku, "mini")
        }

        standardPlanCard?.setOnClickListener {
            val sku = if (planType == PLAN_TYPE_MONTHLY) {
                BillingManager.SKU_MONTHLY_STANDARD
            } else {
                BillingManager.SKU_ANNUAL_STANDARD
            }
            selectPlan(it as MaterialCardView, sku, "standard")
        }

        proPlanCard?.setOnClickListener {
            val sku = if (planType == PLAN_TYPE_MONTHLY) {
                BillingManager.SKU_MONTHLY_PRO
            } else {
                BillingManager.SKU_ANNUAL_PRO
            }
            selectPlan(it as MaterialCardView, sku, "pro")
        }
    }

    private fun selectPlan(card: MaterialCardView, productId: String?, planName: String) {
        // Deselect previous
        selectedCard?.apply {
            strokeWidth = resources.getDimensionPixelSize(R.dimen.card_stroke_width_normal)
            strokeColor = context?.getColor(R.color.card_stroke_default) ?: 0
            cardElevation = 0f
        }

        // Select new
        selectedCard = card
        card.strokeWidth = resources.getDimensionPixelSize(R.dimen.card_stroke_width_selected)
        card.strokeColor = context?.getColor(R.color.primary) ?: 0

        // Notify listener
        val productDetails = productId?.let { productDetailsMap?.get(it) }
        onPlanSelectedListener?.onPlanSelected(productDetails, planName, planType)
    }

    fun updatePrices(productDetails: Map<String, ProductDetails>) {
        this.productDetailsMap = productDetails

        android.util.Log.d("SubscriptionPlans", "updatePrices called with ${productDetails.size} products for $planType")

        if (view == null) {
            android.util.Log.w("SubscriptionPlans", "View is null, cannot update prices yet")
            return
        }

        // Update Mini plan price
        val miniSku = if (planType == PLAN_TYPE_MONTHLY) {
            BillingManager.SKU_MONTHLY_MINI
        } else {
            BillingManager.SKU_ANNUAL_MINI
        }
        productDetails[miniSku]?.let { details ->
            val price = details.subscriptionOfferDetails?.firstOrNull()
                ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
            android.util.Log.d("SubscriptionPlans", "Mini price for $miniSku: $price")
            miniPriceText?.text = price ?: ""
        } ?: android.util.Log.w("SubscriptionPlans", "No product details for $miniSku")

        // Update Standard plan price
        val standardSku = if (planType == PLAN_TYPE_MONTHLY) {
            BillingManager.SKU_MONTHLY_STANDARD
        } else {
            BillingManager.SKU_ANNUAL_STANDARD
        }
        productDetails[standardSku]?.let { details ->
            val price = details.subscriptionOfferDetails?.firstOrNull()
                ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
            android.util.Log.d("SubscriptionPlans", "Standard price for $standardSku: $price")
            standardPriceText?.text = price ?: ""
        } ?: android.util.Log.w("SubscriptionPlans", "No product details for $standardSku")

        // Update Pro plan price
        val proSku = if (planType == PLAN_TYPE_MONTHLY) {
            BillingManager.SKU_MONTHLY_PRO
        } else {
            BillingManager.SKU_ANNUAL_PRO
        }
        productDetails[proSku]?.let { details ->
            val price = details.subscriptionOfferDetails?.firstOrNull()
                ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
            android.util.Log.d("SubscriptionPlans", "Pro price for $proSku: $price")
            proPriceText?.text = price ?: ""
        } ?: android.util.Log.w("SubscriptionPlans", "No product details for $proSku")
    }

    fun setOnPlanSelectedListener(listener: OnPlanSelectedListener) {
        this.onPlanSelectedListener = listener
    }

    /**
     * Mark the FREE plan as "Current Plan" and disable its selection.
     * Called by SubscriptionInfoActivity when in UPGRADE mode.
     */
    fun setFreePlanAsCurrent(isCurrent: Boolean) {
        isFreePlanCurrent = isCurrent
        // Only apply if view is already created
        if (view != null) {
            applyFreePlanCurrentState()
        }
    }

    /**
     * Apply visual state: grey out the FREE plan card, show "Current Plan" label,
     * and disable click interaction.
     */
    private fun applyFreePlanCurrentState() {
        if (isFreePlanCurrent) {
            freePlanCard?.apply {
                // Grey out: reduce alpha to indicate disabled state
                alpha = 0.5f
                isClickable = false
                isFocusable = false
            }
            // Replace price text with "Current Plan" label
            freePlanPriceText?.text = getString(R.string.plan_current)
        } else {
            freePlanCard?.apply {
                alpha = 1.0f
                isClickable = true
                isFocusable = true
            }
            freePlanPriceText?.text = getString(R.string.plan_free_price)
        }
    }

    interface OnPlanSelectedListener {
        fun onPlanSelected(productDetails: ProductDetails?, planName: String, planType: String)
    }

    companion object {
        private const val ARG_PLAN_TYPE = "plan_type"
        const val PLAN_TYPE_MONTHLY = "monthly"
        const val PLAN_TYPE_ANNUAL = "annual"

        fun newInstance(planType: String): SubscriptionPlansFragment {
            return SubscriptionPlansFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PLAN_TYPE, planType)
                }
            }
        }
    }
}
