package com.f7developer.slimeverse.ads

import com.f7developer.slimeverse.BuildConfig

/**
 * AdMob configuration.
 *
 * DEBUG builds always use Google's official test ad units (safe, no revenue,
 * no policy risk). For RELEASE, replace the RELEASE_* values below with the
 * real IDs from your AdMob console (https://apps.admob.com):
 *   1. Create an app -> copy the App ID into AndroidManifest.xml
 *      (meta-data com.google.android.gms.ads.APPLICATION_ID).
 *   2. Create ad units: Banner, Interstitial, Rewarded -> paste below.
 * Until real IDs are supplied, release builds also fall back to test units so
 * the game remains fully playable and reviewable.
 */
object AdConfig {
    const val TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"

    // TODO(release): set real AdMob ad unit IDs before publishing.
    const val RELEASE_BANNER = ""
    const val RELEASE_INTERSTITIAL = ""
    const val RELEASE_REWARDED = ""

    private fun pick(release: String, test: String): String =
        if (!BuildConfig.DEBUG && release.isNotBlank()) release else test

    val bannerId: String get() = pick(RELEASE_BANNER, TEST_BANNER)
    val interstitialId: String get() = pick(RELEASE_INTERSTITIAL, TEST_INTERSTITIAL)
    val rewardedId: String get() = pick(RELEASE_REWARDED, TEST_REWARDED)

    const val REMOVE_ADS_PRODUCT = "remove_ads_permanent"
    const val INTERSTITIAL_MIN_INTERVAL_MS = 180_000L
}