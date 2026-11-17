package com.f7developer.slimeverse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.f7developer.slimeverse.ui.AppRoot
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val app get() = application as SlimeverseApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppRoot(app.repo) }

        app.consent.gatherConsent(this) {
            if (app.consent.canRequestAds) app.ads.init()
        }
        app.billing.start()
        lifecycleScope.launch {
            app.billing.removeAds.collectLatest { owned ->
                if (owned != app.repo.state.removeAds) {
                    app.repo.act { app.repo.state.removeAds = owned }
                    app.repo.save()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        app.repo.save()
    }
}