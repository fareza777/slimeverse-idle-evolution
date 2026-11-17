package com.f7developer.slimeverse.ads

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/** Google UMP consent flow (GDPR / US states). Gates ad requests. */
class ConsentManager(private val context: Context) {
    private val consentInfo: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)

    val canRequestAds: Boolean get() = consentInfo.canRequestAds()

    fun gatherConsent(activity: Activity, onDone: () -> Unit) {
        val params = ConsentRequestParameters.Builder().build()
        consentInfo.requestConsentInfoUpdate(
            activity, params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    onDone()
                }
            },
            { onDone() },
        )
    }

    fun reset() { consentInfo.reset() }
}