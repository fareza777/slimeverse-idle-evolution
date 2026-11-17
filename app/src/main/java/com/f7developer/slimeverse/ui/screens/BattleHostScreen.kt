package com.f7developer.slimeverse.ui.screens

import android.app.Activity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.f7developer.slimeverse.SlimeverseApp
import com.f7developer.slimeverse.ads.AdConfig
import com.f7developer.slimeverse.data.BattleMode
import com.f7developer.slimeverse.game.Prestige
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

    if (ab == null) {
        LaunchedEffect(Unit) { nav.popBackStack() }
        return
    }
    val current = ab!!

    fun afterContinue() {
        val win = outcome?.win == true
        val mode = current.mode
        repo.clearOutcome()
        if (win && mode == BattleMode.TOWER) {
            repo.startTowerBattle()
        } else {
            repo.stopBattle()
            nav.popBackStack()
        }
    }

    BattleScene(
        ab = current,
        events = repo.battleEvents,
        outcome = outcome,
        speed = repo.state.settings.battleSpeed,
        maxSpeed = Prestige.battleSpeedMax(repo.state),
        onSpeed = { repo.act { repo.state.settings.battleSpeed = it } },
        onExit = { repo.stopBattle(); repo.clearOutcome(); nav.popBackStack() },
        onContinue = {
            val now = System.currentTimeMillis()
            val activity = context as? Activity
            if (!repo.state.removeAds && activity != null && app.consent.canRequestAds &&
                now - repo.state.lastInterstitial > AdConfig.INTERSTITIAL_MIN_INTERVAL_MS
            ) {
                repo.act { repo.state.lastInterstitial = now }
                app.ads.showInterstitial(activity) { afterContinue() }
            } else {
                afterContinue()
            }
        },
        showDamage = repo.state.settings.showDamageNumbers,
    )
}