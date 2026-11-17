package com.f7developer.slimeverse.ui.screens

import android.app.Activity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.f7developer.slimeverse.SlimeverseApp
import com.f7developer.slimeverse.ads.AdConfig
import com.f7developer.slimeverse.data.BattleMode
import com.f7developer.slimeverse.game.Prestige
import com.f7developer.slimeverse.game.Zones
import com.f7developer.slimeverse.ui.LocalRepo
import com.f7developer.slimeverse.ui.render.BattleScene

@Composable
fun BattleHostScreen(nav: NavController) {
    val repo = LocalRepo.current
    val rev by repo.rev.collectAsState()
    val ab by repo.activeBattle.collectAsState()
    val outcome by repo.outcome.collectAsState()
    val context = LocalContext.current
    val app = context.applicationContext as SlimeverseApp

    // No active battle -> render nothing; navigation is handled explicitly below.
    val current = ab ?: return

    fun leaveBattle() {
        repo.clearOutcome()
        nav.popBackStack()
        repo.stopBattle()
    }

    BattleScene(
        ab = current,
        events = repo.battleEvents,
        outcome = outcome,
        speed = repo.state.settings.battleSpeed,
        maxSpeed = Prestige.battleSpeedMax(repo.state),
        onSpeed = { repo.act { repo.state.settings.battleSpeed = it } },
        onExit = { leaveBattle() },
        onContinue = {
            val win = outcome?.win == true
            val mode = current.mode
            val meaningfulBreak = mode == BattleMode.DUNGEON || Zones.isBoss(current.stage)
            // Navigate FIRST so the game never waits on an ad.
            if (win && mode == BattleMode.TOWER) {
                repo.clearOutcome()
                repo.startTowerBattle()
            } else {
                leaveBattle()
            }
            // Interstitial only at meaningful breaks, after navigation, frequency-capped.
            val now = System.currentTimeMillis()
            val activity = context as? Activity
            if (meaningfulBreak && !repo.state.removeAds && activity != null && app.consent.canRequestAds &&
                now - repo.state.lastInterstitial > AdConfig.INTERSTITIAL_MIN_INTERVAL_MS
            ) {
                repo.act { repo.state.lastInterstitial = now }
                app.ads.showInterstitial(activity) {}
            }
        },
        showDamage = repo.state.settings.showDamageNumbers,
    )
}