package com.f7developer.slimeverse

import android.app.Application
import com.f7developer.slimeverse.ads.AdManager
import com.f7developer.slimeverse.ads.ConsentManager
import com.f7developer.slimeverse.billing.BillingManager
import com.f7developer.slimeverse.data.GameRepository

class SlimeverseApp : Application() {
    lateinit var repo: GameRepository
        private set
    lateinit var ads: AdManager
        private set
    lateinit var consent: ConsentManager
        private set
    lateinit var billing: BillingManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        repo = GameRepository(this)
        repo.init()
        ads = AdManager(this)
        consent = ConsentManager(this)
        billing = BillingManager(this)
    }

    companion object {
        lateinit var instance: SlimeverseApp
            private set
    }
}