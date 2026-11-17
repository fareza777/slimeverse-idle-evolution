package com.f7developer.slimeverse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.f7developer.slimeverse.SlimeverseApp
import com.f7developer.slimeverse.ads.BannerAd
import com.f7developer.slimeverse.game.HabitatKind
import com.f7developer.slimeverse.ui.LocalRepo
import com.f7developer.slimeverse.ui.components.*
import com.f7developer.slimeverse.ui.theme.LocalExtraColors
import kotlinx.coroutines.flow.collectLatest

private data class Tab(val label: String, val icon: ImageVector, val active: ImageVector)

private val tabs = listOf(
    Tab("Nest", Icons.Outlined.Home, Icons.Filled.Home),
    Tab("Slimes", Icons.Outlined.Pets, Icons.Filled.Pets),
    Tab("Fuse", Icons.Outlined.Science, Icons.Filled.Science),
    Tab("Battle", Icons.Outlined.Bolt, Icons.Filled.Bolt),
    Tab("More", Icons.Outlined.Apps, Icons.Filled.Apps),
)

@Composable
fun MainScaffold(nav: NavController) {
    val repo = LocalRepo.current
    val rev by repo.rev.collectAsState()
    val offline by repo.offline.collectAsState()
    var tab by rememberSaveable { mutableStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val app = context.applicationContext as SlimeverseApp
    val removeAds by app.billing.removeAds.collectAsState()
    val widthDp = LocalConfiguration.current.screenWidthDp

    LaunchedEffect(Unit) {
        repo.toast.collectLatest { snackbar.showSnackbar(it) }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ResourceBar(nav)
            Box(Modifier.weight(1f)) {
                when (tab) {
                    0 -> HomeScreen(nav)
                    1 -> CollectionScreen(nav)
                    2 -> FuseScreen(nav)
                    3 -> BattleScreen(nav)
                    else -> MoreScreen(nav)
                }
            }
            if (!removeAds && app.consent.canRequestAds) {
                BannerAd(Modifier.fillMaxWidth(), widthDp)
            }
            BottomNav(tab) { tab = it }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp))
        if (offline != null) {
            OfflineModal(offline!!)
        }
    }
}

@Composable
private fun ResourceBar(nav: NavController) {
    val repo = LocalRepo.current
    val rev by repo.rev.collectAsState()
    val r = repo.state.resources
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ResourcePill(Icons.Filled.Paid, formatNum(r.gold), Color(0xFFFFB300), "Gold")
        ResourcePill(Icons.Filled.AutoAwesome, formatNum(r.essence), Color(0xFF26C6DA), "Essence")
        ResourcePill(Icons.Filled.Science, formatNum(r.mutagen), Color(0xFFAB47BC), "Mutagen")
        ResourcePill(Icons.Filled.School, formatNum(r.research), Color(0xFF42A5F5), "Research")
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text("Lv ${repo.state.breederLevel}", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun BottomNav(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        tabs.forEachIndexed { i, t ->
            val sel = i == selected
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelect(i) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.clip(RoundedCornerShape(12.dp)).background(if (sel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent).padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Icon(if (sel) t.active else t.icon, t.label, tint = if (sel) MaterialTheme.colorScheme.primary else LocalExtraColors.current.inkFaint, modifier = Modifier.size(24.dp))
                }
                Text(t.label, fontSize = 11.sp, fontWeight = if (sel) FontWeight.ExtraBold else FontWeight.SemiBold, color = if (sel) MaterialTheme.colorScheme.primary else LocalExtraColors.current.inkFaint)
            }
        }
    }
}

@Composable
private fun OfflineModal(report: com.f7developer.slimeverse.game.OfflineReport) {
    val repo = LocalRepo.current
    val context = LocalContext.current
    val app = context.applicationContext as SlimeverseApp
    val activity = context as? android.app.Activity
    var doubled by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surface).padding(24.dp).widthIn(max = 340.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Welcome back!", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            val hrs = report.elapsedMs / 3_600_000.0
            Text("Your slimes kept working for ${String.format("%.1f", hrs)}h (cap ${report.cappedHours}h).", style = MaterialTheme.typography.bodyMedium, color = LocalExtraColors.current.inkSoft, textAlign = TextAlign.Center)
            GameCard(color = MaterialTheme.colorScheme.surfaceVariant, corner = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (report.clears > 0) RewardRow("Idle battles", "${report.clears} clears")
                    if (report.gold + report.habitatGold > 0) RewardRow("Gold", "+${formatNum(report.gold + report.habitatGold)}")
                    if (report.essence + report.habitatEssence > 0) RewardRow("Essence", "+${formatNum(report.essence + report.habitatEssence)}")
                    if (report.habitatMutagen > 0) RewardRow("Mutagen", "+${report.habitatMutagen}")
                    if (report.habitatResearch > 0) RewardRow("Research", "+${report.habitatResearch}")
                    if (report.xp > 0) RewardRow("Squad XP", "+${formatNum(report.xp)}")
                    if (report.eggsHatched > 0) RewardRow("Eggs hatched", "${report.eggsHatched}")
                }
            }
            if (app.ads.hasRewarded && !doubled && activity != null) {
                SoftButton("Watch ad · double rewards", {
                    app.ads.showRewarded(activity, onReward = {
                        repo.act {
                            repo.state.resources.gold += report.gold + report.habitatGold
                            repo.state.resources.essence += report.essence + report.habitatEssence
                            repo.state.resources.mutagen += report.habitatMutagen
                            repo.state.resources.research += report.habitatResearch
                        }
                        doubled = true
                    }, onDone = {})
                }, icon = Icons.Filled.PlayCircle, tint = Color(0xFF43A047), modifier = Modifier.fillMaxWidth())
            }
            PrimaryButton("Collect", { repo.dismissOffline() }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun RewardRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.5.sp, color = LocalExtraColors.current.inkSoft, fontWeight = FontWeight.SemiBold)
        Text(value, fontSize = 13.5.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold)
    }
}