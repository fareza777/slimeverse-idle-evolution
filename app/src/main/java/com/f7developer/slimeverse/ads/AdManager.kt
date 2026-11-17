package com.f7developer.slimeverse.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class AdManager(private val context: Context) {
    private var interstitial: InterstitialAd? = null
    private var rewarded: RewardedAd? = null
    private var initialized = false

    fun init() {
        if (initialized) return
        initialized = true
        MobileAds.initialize(context) {}
        loadInterstitial()
        loadRewarded()
    }

    fun loadInterstitial() {
        InterstitialAd.load(context, AdConfig.interstitialId, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad }
                override fun onAdFailedToLoad(error: LoadAdError) { interstitial = null }
            })
    }

    fun loadRewarded() {
        RewardedAd.load(context, AdConfig.rewardedId, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { rewarded = ad }
                override fun onAdFailedToLoad(error: LoadAdError) { rewarded = null }
            })
    }

    /** Show at natural breaks only; caller enforces frequency + removeAds. */
    fun showInterstitial(activity: Activity, onDone: () -> Unit) {
        val ad = interstitial
        if (ad == null) { onDone(); loadInterstitial(); return }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { interstitial = null; loadInterstitial(); onDone() }
            override fun onAdFailedToShowFullScreenContent(error: AdError) { interstitial = null; loadInterstitial(); onDone() }
        }
        ad.show(activity)
    }

    val hasRewarded: Boolean get() = rewarded != null

    fun showRewarded(activity: Activity, onReward: () -> Unit, onDone: () -> Unit) {
        val ad = rewarded
        if (ad == null) { onDone(); loadRewarded(); return }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { rewarded = null; loadRewarded(); onDone() }
            override fun onAdFailedToShowFullScreenContent(error: AdError) { rewarded = null; loadRewarded(); onDone() }
        }
        ad.show(activity) { onReward() }
    }
}