package com.fittrack.app.billing

import android.app.Activity
import android.app.Application
import android.content.SharedPreferences
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.fittrack.app.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Pro entitlement + Google Play Billing.
 *
 * Holds [Application] (never an Activity) so the Billing client cannot leak
 * window context. Entitlement is the OR of an active subscription
 * (monthly / yearly) and a one-time lifetime purchase, cached in
 * SharedPreferences for an offline-tolerant cold start.
 */
class ProManager(application: Application) : PurchasesUpdatedListener {

    private val appContext = application.applicationContext

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)

    private val _isPro = MutableStateFlow(prefs.getBoolean(KEY_IS_PRO, false))
    val isPro: StateFlow<Boolean> = _isPro

    /**
     * Whether the launch-window founder lifetime price is currently being shown.
     * Auto-derived from [FOUNDER_LIFETIME_END_MILLIS] — flips off automatically
     * the first time anyone opens the upgrade screen after the cutoff.
     */
    val founderLifetimePriceActive: Boolean
        get() = System.currentTimeMillis() < FOUNDER_LIFETIME_END_MILLIS

    /** Epoch millis at which the founder window ends, or null if it has already passed. */
    val founderLifetimeEndsAtMillis: Long?
        get() = FOUNDER_LIFETIME_END_MILLIS.takeIf { founderLifetimePriceActive }

    private var billingClient: BillingClient? = null
    private var subscriptionDetails: List<ProductDetails> = emptyList()
    private var lifetimeDetails: ProductDetails? = null

    fun initialize() {
        if (billingClient != null) return
        billingClient = BillingClient.newBuilder(appContext)
            .setListener(this)
            .enablePendingPurchases()
            .build()
            .also { client ->
                client.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            querySubscriptionProducts()
                            queryLifetimeProduct()
                            queryExistingPurchases()
                        }
                    }

                    override fun onBillingServiceDisconnected() {
                        // Connection retried lazily on next public call.
                    }
                })
            }
    }

    // region Public entitlement API

    /** True when the given capability is unlocked for the current user. */
    fun isUnlocked(gate: FeatureGate): Boolean {
        // For now, every gate maps to "any Pro entitlement". When tier
        // differentiation lands (e.g. Watch only on annual+), branch here.
        @Suppress("UNUSED_PARAMETER") val ignored = gate
        return _isPro.value
    }

    fun purchaseMonthly(activity: Activity): Boolean =
        launchSubscriptionPurchase(activity, PRODUCT_ID_MONTHLY)

    fun purchaseYearly(activity: Activity): Boolean =
        launchSubscriptionPurchase(activity, PRODUCT_ID_YEARLY)

    fun purchaseLifetime(activity: Activity): Boolean {
        val product = lifetimeDetails ?: return false
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(product)
                        .build()
                )
            )
            .build()
        val result = billingClient?.launchBillingFlow(activity, params)
        return result?.responseCode == BillingClient.BillingResponseCode.OK
    }

    /** Re-query Play for any existing entitlements; call from "Restore Purchases". */
    fun restorePurchases() {
        if (billingClient?.isReady == true) {
            queryExistingPurchases()
        } else {
            initialize()
        }
    }

    fun getMonthlyPrice(): String =
        priceFor(PRODUCT_ID_MONTHLY) ?: DEFAULT_MONTHLY_PRICE

    fun getYearlyPrice(): String =
        priceFor(PRODUCT_ID_YEARLY) ?: DEFAULT_YEARLY_PRICE

    fun getLifetimePrice(): String =
        lifetimeDetails?.oneTimePurchaseOfferDetails?.formattedPrice
            ?: if (founderLifetimePriceActive) DEFAULT_LIFETIME_FOUNDER_PRICE
            else DEFAULT_LIFETIME_PRICE

    /**
     * Toggle Pro for local testing. No-op in release builds — release callers
     * still compile but have no way to flip entitlement without a real purchase.
     */
    fun debugTogglePro() {
        if (!BuildConfig.DEBUG) return
        updateProStatus(!_isPro.value)
    }

    fun destroy() {
        billingClient?.endConnection()
        billingClient = null
    }

    // endregion

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK || purchases == null) return
        for (purchase in purchases) {
            handlePurchase(purchase)
        }
    }

    // region Private

    private fun querySubscriptionProducts() {
        val productList = listOf(PRODUCT_ID_MONTHLY, PRODUCT_ID_YEARLY).map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        billingClient?.queryProductDetailsAsync(params) { result, details ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                subscriptionDetails = details
            }
        }
    }

    private fun queryLifetimeProduct() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID_LIFETIME)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()
        billingClient?.queryProductDetailsAsync(params) { result, details ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                lifetimeDetails = details.firstOrNull { it.productId == PRODUCT_ID_LIFETIME }
            }
        }
    }

    private fun queryExistingPurchases() {
        val client = billingClient ?: return
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { result, purchases ->
            val hasActiveSub = result.responseCode == BillingClient.BillingResponseCode.OK &&
                purchases.any(::isActiveSubscription)
            // Don't downgrade entitlement based on subs alone — lifetime
            // could still be entitling the user. Combine after the INAPP query.
            client.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            ) { lifetimeResult, lifetimePurchases ->
                val hasLifetime = lifetimeResult.responseCode == BillingClient.BillingResponseCode.OK &&
                    lifetimePurchases.any(::isLifetimePurchase)
                updateProStatus(hasActiveSub || hasLifetime)
                purchases.filter { !it.isAcknowledged }.forEach(::acknowledge)
                lifetimePurchases.filter { !it.isAcknowledged }.forEach(::acknowledge)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        val grantsPro = purchase.products.any { it in PRO_PRODUCT_IDS }
        if (grantsPro) {
            updateProStatus(true)
            if (!purchase.isAcknowledged) acknowledge(purchase)
        }
    }

    private fun acknowledge(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient?.acknowledgePurchase(params) { /* fire-and-forget */ }
    }

    private fun launchSubscriptionPurchase(activity: Activity, productId: String): Boolean {
        val product = subscriptionDetails.find { it.productId == productId } ?: return false
        // Prefer an offer with a free trial phase if one is available; fall back
        // to the first listed offer (typically the base plan with no intro).
        val offer = product.subscriptionOfferDetails?.firstOrNull(::hasFreePhase)
            ?: product.subscriptionOfferDetails?.firstOrNull()
            ?: return false
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(product)
                        .setOfferToken(offer.offerToken)
                        .build()
                )
            )
            .build()
        val result = billingClient?.launchBillingFlow(activity, params)
        return result?.responseCode == BillingClient.BillingResponseCode.OK
    }

    /**
     * Trial duration string (e.g. "7 days") for the yearly plan, or null when
     * no free-trial offer is configured / available to this user. Driven by
     * the Play Console offer setup — Play only returns offers the user is
     * eligible for, so this also returns null for users who already used
     * their trial.
     */
    fun getYearlyTrialDuration(): String? {
        val product = subscriptionDetails.find { it.productId == PRODUCT_ID_YEARLY } ?: return null
        val trialOffer = product.subscriptionOfferDetails?.firstOrNull(::hasFreePhase) ?: return null
        val freePhase = trialOffer.pricingPhases.pricingPhaseList
            .firstOrNull { it.priceAmountMicros == 0L } ?: return null
        return formatBillingPeriod(freePhase.billingPeriod)
    }

    private fun priceFor(productId: String): String? =
        subscriptionDetails.find { it.productId == productId }
            ?.subscriptionOfferDetails
            // Use the LAST pricing phase for the headline price — for an offer
            // with a free trial, the first phase is free and the second is the
            // ongoing rate, which is what we actually want to display.
            ?.firstOrNull()
            ?.pricingPhases?.pricingPhaseList?.lastOrNull()
            ?.formattedPrice

    private fun updateProStatus(isPro: Boolean) {
        _isPro.value = isPro
        prefs.edit().putBoolean(KEY_IS_PRO, isPro).apply()
    }

    // endregion

    companion object {
        private const val PREFS_NAME = "fittrack_pro"
        private const val KEY_IS_PRO = "is_pro"

        const val PRODUCT_ID_MONTHLY = "fittrack_pro_monthly"
        const val PRODUCT_ID_YEARLY = "fittrack_pro_yearly"
        const val PRODUCT_ID_LIFETIME = "fittrack_pro_lifetime"

        private val PRO_PRODUCT_IDS = setOf(
            PRODUCT_ID_MONTHLY,
            PRODUCT_ID_YEARLY,
            PRODUCT_ID_LIFETIME,
        )

        private const val DEFAULT_MONTHLY_PRICE = "$3.99/mo"
        private const val DEFAULT_YEARLY_PRICE = "$29.99/yr"
        private const val DEFAULT_LIFETIME_PRICE = "$79.99"
        private const val DEFAULT_LIFETIME_FOUNDER_PRICE = "$59.99"

        /**
         * Cutoff for founder lifetime pricing (epoch millis, UTC).
         * Currently set to 2026-06-30T23:59:59Z. Update before each launch / extension.
         * After this instant, the lifetime card automatically falls back to the
         * standard price and copy.
         */
        const val FOUNDER_LIFETIME_END_MILLIS: Long = 1782518399000L

        /** Free-tier limits applied by the UI layer. */
        const val FREE_ACTIVE_PROGRAM_SLOTS = 1
        const val FREE_CUSTOM_ROUTINE_LIMIT = 3
        const val FREE_HISTORY_DAYS = 30
        const val FREE_INSIGHT_PREVIEW_COUNT = 1

        private fun isActiveSubscription(purchase: Purchase): Boolean =
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                purchase.products.any { it == PRODUCT_ID_MONTHLY || it == PRODUCT_ID_YEARLY }

        private fun isLifetimePurchase(purchase: Purchase): Boolean =
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                purchase.products.contains(PRODUCT_ID_LIFETIME)

        private fun hasFreePhase(offer: ProductDetails.SubscriptionOfferDetails): Boolean =
            offer.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L }

        /**
         * Render a Play Billing ISO 8601 period string ("P7D", "P2W", …) as
         * something a paywall can display. Only the periods Play actually
         * uses for trial offers are special-cased; anything else falls
         * through to the raw token.
         */
        private fun formatBillingPeriod(iso: String): String = when (iso) {
            "P3D" -> "3 days"
            "P7D", "P1W" -> "7 days"
            "P14D", "P2W" -> "14 days"
            "P1M" -> "1 month"
            else -> iso
        }
    }
}
