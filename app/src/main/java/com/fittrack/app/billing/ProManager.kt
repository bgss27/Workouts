package com.fittrack.app.billing

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages Pro subscription state using Google Play Billing.
 *
 * Falls back to SharedPreferences for local state caching.
 * In debug builds, Pro can be toggled manually for testing.
 */
class ProManager(private val context: Context) : PurchasesUpdatedListener {

    companion object {
        private const val PREFS_NAME = "fittrack_pro"
        private const val KEY_IS_PRO = "is_pro"
        private const val PRODUCT_ID_MONTHLY = "fittrack_pro_monthly"
        private const val PRODUCT_ID_YEARLY = "fittrack_pro_yearly"

        // Free tier limits
        const val FREE_PROGRAM_LIMIT = 1 // Only 1 program (3-day only)
        const val FREE_ROUTINE_LIMIT = 4 // Standalone routines available
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isPro = MutableStateFlow(prefs.getBoolean(KEY_IS_PRO, false))
    val isPro: StateFlow<Boolean> = _isPro

    private var billingClient: BillingClient? = null
    private var productDetails: List<ProductDetails> = emptyList()

    /**
     * Initialize the billing client and check existing purchases.
     */
    fun initialize() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProducts()
                    queryExistingPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Retry connection on next launch
            }
        })
    }

    private fun queryProducts() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_YEARLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { billingResult, details ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetails = details
            }
        }
    }

    private fun queryExistingPurchases() {
        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasActiveSub = purchases.any { purchase ->
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        (purchase.products.contains(PRODUCT_ID_MONTHLY) ||
                            purchase.products.contains(PRODUCT_ID_YEARLY))
                }
                updateProStatus(hasActiveSub)

                // Acknowledge any unacknowledged purchases
                purchases.filter { !it.isAcknowledged }.forEach { purchase ->
                    val ackParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient?.acknowledgePurchase(ackParams) { }
                }
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    updateProStatus(true)
                    // Acknowledge the purchase
                    if (!purchase.isAcknowledged) {
                        val ackParams = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                        billingClient?.acknowledgePurchase(ackParams) { }
                    }
                }
            }
        }
    }

    /**
     * Launch the purchase flow for monthly subscription.
     */
    fun purchaseMonthly(activity: Activity): Boolean {
        return launchPurchase(activity, PRODUCT_ID_MONTHLY)
    }

    /**
     * Launch the purchase flow for yearly subscription.
     */
    fun purchaseYearly(activity: Activity): Boolean {
        return launchPurchase(activity, PRODUCT_ID_YEARLY)
    }

    private fun launchPurchase(activity: Activity, productId: String): Boolean {
        val product = productDetails.find { it.productId == productId } ?: return false
        val offerToken = product.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return false

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(product)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val result = billingClient?.launchBillingFlow(activity, billingFlowParams)
        return result?.responseCode == BillingClient.BillingResponseCode.OK
    }

    /**
     * Get formatted price strings for display.
     */
    fun getMonthlyPrice(): String {
        return productDetails.find { it.productId == PRODUCT_ID_MONTHLY }
            ?.subscriptionOfferDetails?.firstOrNull()
            ?.pricingPhases?.pricingPhaseList?.firstOrNull()
            ?.formattedPrice ?: "$4.99/month"
    }

    fun getYearlyPrice(): String {
        return productDetails.find { it.productId == PRODUCT_ID_YEARLY }
            ?.subscriptionOfferDetails?.firstOrNull()
            ?.pricingPhases?.pricingPhaseList?.firstOrNull()
            ?.formattedPrice ?: "$29.99/year"
    }

    /**
     * Check if a specific feature is available.
     */
    fun hasFeature(feature: ProFeature): Boolean = _isPro.value

    /**
     * Check if user can access a specific program by days-per-week.
     */
    fun canAccessProgram(daysPerWeek: Int): Boolean {
        if (_isPro.value) return true
        // Free users get 3-day programs only
        return daysPerWeek <= 3
    }

    /**
     * For debug/testing: toggle Pro status manually.
     */
    fun debugTogglePro() {
        updateProStatus(!_isPro.value)
    }

    private fun updateProStatus(isPro: Boolean) {
        _isPro.value = isPro
        prefs.edit().putBoolean(KEY_IS_PRO, isPro).apply()
    }

    fun destroy() {
        billingClient?.endConnection()
    }
}
