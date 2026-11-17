package com.f7developer.slimeverse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.f7developer.slimeverse.game.*
import com.f7developer.slimeverse.ui.LocalRepo
import com.f7developer.slimeverse.ui.components.*
import com.f7developer.slimeverse.ui.render.SlimeView
import com.f7developer.slimeverse.ui.theme.LocalExtraColors

@Composable
fun CodexScreen(nav: NavController) {
    val repo = LocalRepo.current
    val rev by repo.rev.collectAsState()
    val state = repo.state
    var tab by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(nav, "Evolution Codex")
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Species", "Genes", "Fusions").forEachIndexed { i, l -> FilterChip(tab == i, { tab = i }, { Text(l, fontSize = 13.sp) }) }
        }
        when (tab) {
            0 -> {
                val baseSpecies = Species.all.filter { it.tier == 0 }
                Text("${state.codexSpecies.count { !it.contains(".t") }} / ${baseSpecies.size} species · ${state.codexSpecies.size} / ${Species.totalForms} forms", Modifier.padding(16.dp), fontSize = 13.sp, color = LocalExtraColors.current.inkSoft, fontWeight = FontWeight.Bold)
                LazyVerticalGrid(GridCells.Fixed(4), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                    items(baseSpecies) { sp ->
                        val found = sp.id in state.codexSpecies
                        Column(Modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp)).background(if (found) Color(sp.element.color).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                if (found) {
                                    val v = UnitVisual(sp.family, sp.element, null, 0, Rarity.of(sp.rarityFloor), "standard", "calm", "smooth", "none", "round", "none", "plain", "soft", false, sp.id.hashCode())
                                    SlimeView(v, Modifier.fillMaxSize(), radiusFactor = 0.3f, animate = false)
                                } else {
                                    Text("?", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = LocalExtraColors.current.inkFaint)
                                }
                            }
                            Text(if (found) sp.name else "???", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center, color = if (found) MaterialTheme.colorScheme.onSurface else LocalExtraColors.current.inkFaint)
                        }
                    }
                }
            }
            1 -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Locus.entries.forEach { locus ->
                    GameCard {
                        Text(locus.displayName, style = MaterialTheme.typography.titleMedium)
                        Text(locus.desc, fontSize = 13.5.sp, color = LocalExtraColors.current.inkSoft)
                        Spacer(Modifier.height(6.dp))
                        Genes.pool(locus).forEach { a ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                Text(a.displayName + (if (a.rare) " ★" else ""), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(110.dp), color = if (a.rare) Color(0xFFE040FB) else MaterialTheme.colorScheme.onSurface)
                                Text("dom ${a.dominance}" + (a.trait?.let { " · ${it.displayName}" } ?: ""), fontSize = 13.sp, color = LocalExtraColors.current.inkSoft)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            else -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GameCard {
                    Text("Fusion discoveries · ${state.codexFusions.size} / ${Families.fusionPairs.size}", style = MaterialTheme.typography.titleMedium)
                    Text("Hybrid families: Seraph, Chimera, Titan, Phantom.", fontSize = 13.5.sp, color = LocalExtraColors.current.inkSoft)
                }
                Families.fusionPairs.forEach { (a, b) ->
                    val key = repo.engine.fusionKey(a, b)
                    val found = key in state.codexFusions
                    GameCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${a.displayName} + ${b.displayName}", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, modifier = Modifier.weight(1f))
                            Text(if (found) Families.fusionResult(a, b, kotlin.random.Random(0)).displayName else "Undiscovered", fontSize = 12.5.sp, color = if (found) Color(0xFF43A047) else LocalExtraColors.current.inkFaint, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun AscensionScreen(nav: NavController) {
    val repo = LocalRepo.current
    val rev by repo.rev.collectAsState()
    val state = repo.state
    var confirmRebirth by remember { mutableStateOf(false) }
    var confirmAscend by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(nav, "Rebirth & Ascension")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GameCard(color = MaterialTheme.colorScheme.surface) {
                Text("Rebirth", style = MaterialTheme.typography.headlineSmall, color = Color(0xFFFFB300))
                Text("Reset zones and slime levels. Keep your collection, codex, research and talents. Gain Stardust for permanent multipliers.", style = MaterialTheme.typography.bodySmall, color = LocalExtraColors.current.inkSoft)
                Spacer(Modifier.height(8.dp))
                Text("Requires: clear Zone 6 · Gain: ${formatNum(repo.engine.rebirthGain())} Stardust", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Current bonus: +${fmt0((Prestige.stardustMult(state) - 1) * 100)}% gold & essence", fontSize = 13.sp, color = Color(0xFFFFB300), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                PrimaryButton("Rebirth", { confirmRebirth = true }, enabled = repo.engine.canRebirth(), color = Color(0xFFFFB300), modifier = Modifier.fillMaxWidth())
            }
            RebirthUpgrades.all.forEach { u ->
                val level = RebirthUpgrades.level(state, u.id)
                GameCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${u.name} · Lv $level/${u.maxLevel}", style = MaterialTheme.typography.titleSmall)
                            Text(u.desc, fontSize = 13.5.sp, color = LocalExtraColors.current.inkSoft)
                        }
                        SoftButton("${RebirthUpgrades.cost(u.id, level)} ✦", {
                            repo.act { if (!repo.engine.buyRebirthUpgrade(u.id)) repo.toast("Not enough Stardust.") }
                        }, enabled = level < u.maxLevel && state.resources.stardust >= RebirthUpgrades.cost(u.id, level), tint = Color(0xFFFFB300))
                    }
                }
            }
            GameCard {
                Text("Ascension", style = MaterialTheme.typography.headlineSmall, color = Color(0xFFE040FB))
                Text("A deeper reset. Keep only Celestial slimes and your codex/perks. Gain Aether Cores for massive permanent power and the Aether element.", style = MaterialTheme.typography.bodySmall, color = LocalExtraColors.current.inkSoft)
                Spacer(Modifier.height(8.dp))
                Text("Requires: clear Zone 12 + Tower 50 · Gain: ${formatNum(repo.engine.ascendGain())} Aether", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Current bonus: +${fmt0((Prestige.ascensionMult(state) / (if (AscensionPerks.has(state, "ap_stats")) 1.15 else 1.0) - 1) * 100)}% all stats", fontSize = 13.sp, color = Color(0xFFE040FB), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                PrimaryButton("Ascend", { confirmAscend = true }, enabled = repo.engine.canAscend(), color = Color(0xFFE040FB), modifier = Modifier.fillMaxWidth())
            }
            AscensionPerks.all.forEach { p ->
                val owned = p.id in state.ascensionPerks
                GameCard(color = if (owned) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = MaterialTheme.typography.titleSmall)
                            Text(p.desc, fontSize = 13.5.sp, color = LocalExtraColors.current.inkSoft)
                        }
                        if (owned) Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF43A047))
                        else SoftButton("${p.cost} ◈", { repo.act { if (!repo.engine.buyAscensionPerk(p.id)) repo.toast("Not enough Aether.") } }, enabled = state.resources.aether >= p.cost, tint = Color(0xFFE040FB))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
    if (confirmRebirth) ConfirmDialog("Rebirth?", "Zones and levels reset. You gain ${formatNum(repo.engine.rebirthGain())} Stardust. Collection is kept.", "Rebirth", {
        repo.act { val g = repo.engine.rebirth(); repo.toast("Reborn! +$g Stardust") }; confirmRebirth = false
    }, { confirmRebirth = false })
    if (confirmAscend) ConfirmDialog("Ascend?", "Only Celestial slimes survive. You gain ${formatNum(repo.engine.ascendGain())} Aether Cores.", "Ascend", {
        repo.act { val g = repo.engine.ascend(); repo.toast("Ascended! +$g Aether") }; confirmAscend = false
    }, { confirmAscend = false }, danger = true)
}

@Composable
fun AchievementsScreen(nav: NavController) {
    val repo = LocalRepo.current
    val rev by repo.rev.collectAsState()
    val state = repo.state
    val claimable = repo.engine.claimableAchievements()
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(nav, "Achievements")
        if (claimable.isNotEmpty()) {
            PrimaryButton("Claim all (${claimable.size})", { repo.act { repo.engine.claimAllAchievements() } }, Modifier.padding(16.dp).fillMaxWidth(), icon = Icons.Filled.EmojiEvents)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Achievements.all.forEach { a ->
                val claimed = a.id in state.achievementsClaimed
                val value = repo.engine.statValue(a.key)
                val done = value >= a.threshold
                GameCard(color = if (claimed) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(a.name, style = MaterialTheme.typography.titleSmall)
                            Text(a.desc, fontSize = 13.5.sp, color = LocalExtraColors.current.inkSoft)
                            if (!claimed) LinearProgress((value.toFloat() / a.threshold).coerceIn(0f, 1f), Color(0xFFFFB300), Modifier.fillMaxWidth().padding(top = 4.dp), 5.dp)
                        }
                        Spacer(Modifier.width(8.dp))
                        if (claimed) Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF43A047))
                        else if (done) SoftButton("Claim", { repo.act { repo.engine.claimAchievement(a.id) } }, tint = Color(0xFFFFB300))
                        else Text("${formatNum(value)}/${formatNum(a.threshold)}", fontSize = 12.5.sp, color = LocalExtraColors.current.inkFaint, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}