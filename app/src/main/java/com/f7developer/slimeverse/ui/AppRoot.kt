package com.f7developer.slimeverse.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.f7developer.slimeverse.data.GameRepository
import com.f7developer.slimeverse.ui.screens.*
import com.f7developer.slimeverse.ui.theme.SlimeverseTheme
import com.f7developer.slimeverse.ui.theme.ThemeMode

@Composable
fun AppRoot(repo: GameRepository) {
    val rev by repo.rev.collectAsState()
    val theme = when (repo.state.settings.theme) {
        "DARK" -> ThemeMode.DARK
        "SYSTEM" -> ThemeMode.SYSTEM
        else -> ThemeMode.LIGHT
    }
    CompositionLocalProvider(LocalRepo provides repo) {
        SlimeverseTheme(theme) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                val nav = rememberNavController()
                NavHost(
                    navController = nav, startDestination = "splash",
                    enterTransition = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)) + androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.tween(300)) { it / 7 } },
                    exitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(220)) },
                    popEnterTransition = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)) },
                    popExitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(220)) + androidx.compose.animation.slideOutHorizontally(androidx.compose.animation.core.tween(220)) { it / 7 } },
                ) {
                    composable("splash") {
                        SplashScreen(onDone = {
                            val dest = if (repo.state.onboardingDone) "root" else "onboarding"
                            nav.navigate(dest) { popUpTo("splash") { inclusive = true } }
                        })
                    }
                    composable("onboarding") {
                        OnboardingScreen(onDone = {
                            repo.act { repo.state.onboardingDone = true; repo.state.onboardingStep = 99 }
                            repo.save()
                            nav.navigate("root") { popUpTo("onboarding") { inclusive = true } }
                        })
                    }
                    composable("root") { MainScaffold(nav) }
                    composable("battle") { BattleHostScreen(nav) }
                    composable(
                        "detail/{uid}",
                        arguments = listOf(navArgument("uid") { type = NavType.LongType }),
                    ) { entry ->
                        SlimeDetailScreen(uid = entry.arguments?.getLong("uid") ?: 0L, nav = nav)
                    }
                    composable("research") { ResearchScreen(nav) }
                    composable("talents") { TalentsScreen(nav) }
                    composable("codex") { CodexScreen(nav) }
                    composable("ascension") { AscensionScreen(nav) }
                    composable("achievements") { AchievementsScreen(nav) }
                    composable("settings") { SettingsScreen(nav) }
                    composable("about") { AboutScreen(nav) }
                }
            }
        }
    }
}