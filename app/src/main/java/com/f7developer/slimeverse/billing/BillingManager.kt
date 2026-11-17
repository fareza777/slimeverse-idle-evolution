package com.f7developer.slimeverse.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.f7developer.slimeverse.ads.AdConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Play Billing for the non-consumable "Remove Ads" upgrade, with restore. */
class BillingManager(private val context: Context) {
    private val _removeAds = MutableStateFlow(false)
    val removeAds: StateFlow<Boolean> = _removeAds.asStateFlow()

    private val _price = MutableStateFlow("$4.99")
    val price: StateFlow<String> = _price.asStateFlow()

    private val _available = MutableStateFlow(false)
    val available: StateFlow<Boolean> = _available.asStateFlow()

    private var client: BillingClient? = null
    private var productDetails: ProductDetails? = null

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (p in purchases) handlePurchase(p)
        }
    }

    fun start(onReady: () -> Unit = {}) {
        val c = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
        client = c
        c.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProduct()
                    restorePurchases()
                    onReady()
                }
            }
            override fun onBillingServiceDisconnected() {}
        })
    }

    private fun queryProduct() {
        val c = client ?: return
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(AdConfig.REMOVE_ADS_PRODUCT)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            ).build()
        c.queryProductDetailsAsync(params) { result, response ->
            val details = response.productDetailsList
            if (result.responseCode == BillingClient.BillingResponseCode.OK && details.isNotEmpty()) {
                productDetails = details[0]
                _price.value = details[0].oneTimePurchaseOfferDetails?.formattedPrice ?: "$4.99"
                _available.value = true
            }
        }
    }

    fun restorePurchases() {
        val c = client ?: return
        c.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                var owned = false
                for (p in purchases) {
                    if (p.products.contains(AdConfig.REMOVE_ADS_PRODUCT)) {
                        owned = true
                        if (p.purchaseState == Purchase.PurchaseState.PURCHASED && !p.isAcknowledged) {
                            acknowledge(p)
                        }
                    }
                }
                _removeAds.value = owned
            }
        }
    }

    fun launchPurchase(activity: Activity) {
        val c = client ?: return
        val details = productDetails ?: run { queryProduct(); return }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                )
            ).build()
        c.launchBillingFlow(activity, params)
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && purchase.products.contains(AdConfig.REMOVE_ADS_PRODUCT)) {
            _removeAds.value = true
            if (!purchase.isAcknowledged) acknowledge(purchase)
        }
    }

    private fun acknowledge(purchase: Purchase) {
        val c = client ?: return
        c.acknowledgePurchase(
            AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        ) {}
    }
}