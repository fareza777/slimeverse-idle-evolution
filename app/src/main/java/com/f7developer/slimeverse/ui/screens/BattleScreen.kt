package com.f7developer.slimeverse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.f7developer.slimeverse.game.*
import com.f7developer.slimeverse.ui.LocalRepo
import com.f7developer.slimeverse.ui.components.*
import com.f7developer.slimeverse.ui.render.SlimeView
import com.f7developer.slimeverse.ui.theme.LocalExtraColors

@Composable
fun BattleScreen(nav: NavController) {
    val repo = LocalRepo.current
    val rev by repo.rev.collectAsState()
    var tab by remember { mutableStateOf(0) }
    var pickSlot by remember { mutableStateOf(-1) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeader("Battle", "Squad power ${formatNum(repo.engine.squadPower())}")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Campaign", "Squad", "Tower", "Dungeons", "Daily").forEachIndexed { i, label ->
                FilterChip(tab == i, { tab = i }, { Text(label, fontSize = 12.sp) })
            }
        }
        when (tab) {
            0 -> CampaignTab(nav)
            1 -> SquadTab { pickSlot = it }
            2 -> TowerTab(nav)
            3 -> DungeonsTab(nav)
            else -> DailyTab()
        }
        Spacer(Modifier.height(16.dp))
    }
    if (pickSlot >= 0) {
        SlimePickerDialog(
            title = "Choose slime for slot ${pickSlot + 1}",
            filter = { true },
            onPick = { uid -> repo.act { repo.engine.setSquadSlot(pickSlot, uid) }; pickSlot = -1 },
            onDismiss = { pickSlot = -1 },
        )
    }
}

@Composable
private fun CampaignTab(nav: NavController) {
    val repo = LocalRepo.current
    val rev by repo.rev.collectAsState()
    val state = repo.state
    val highest = state.highestZoneCleared()
    var zone by remember { mutableStateOf(state.farmZone) }
    val z = Zones.get(zone)
    val unlocked = zone <= highest + 1

    GameCard {
        Text("Zones", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Zones.all) { zd ->
                val open = zd.index <= highest + 1
                val sel = zd.index == zone
                Column(
                    Modifier.clip(RoundedCornerShape(14.dp)).background(if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant).clickable(enabled = open) { zone = zd.index }.padding(10.dp).width(110.dp),
                ) {
                    Box(Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(8.dp)).background(Color(zd.bgTop)))
                    Spacer(Modifier.height(6.dp))
                    Text(zd.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, color = if (open) MaterialTheme.colorScheme.onSurface else LocalExtraColors.current.inkFaint)
                    Text(if (open) "Best ${state.maxStage(zd.index)}/10" else "Locked", fontSize = 10.5.sp, color = LocalExtraColors.current.inkSoft)
                }
            }
        }
    }
    GameCard {
        Text(z.name, style = MaterialTheme.typography.titleLarge)
        Text(z.subtitle, style = MaterialTheme.typography.bodySmall, color = LocalExtraColors.current.inkSoft)
        Spacer(Modifier.height(10.dp))
        if (!unlocked) {
            Text("Clear the previous zone's boss to unlock.", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        } else {
            (1..Zones.STAGES).forEach { stage ->
                val cleared = state.maxStage(zone) >= stage
                val boss = Zones.isBoss(stage)
                val isFarm = state.farmZone == zone && state.farmStage == stage
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (isFarm) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant).clickable {
                        repo.act { state.farmZone = zone; state.farmStage = stage }
                        repo.startStageBattle(zone, stage); nav.navigate("battle")
                    }.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(if (boss) Icons.Filled.Whatshot else if (cleared) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null, tint = if (boss) Color(0xFFFF7043) else if (cleared) Color(0xFF43A047) else LocalExtraColors.current.inkFaint, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(if (boss) "Boss Stage" else "Stage $stage", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    if (isFarm) Text("FARMING", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.tertiary)
                    Icon(Icons.Filled.PlayArrow, null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(6.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Auto-progress", fontSize = 13.sp, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Switch(state.settings.autoProgress, { repo.act { state.settings.autoProgress = it } }, enabled = "a2" in state.research || Prestige.automationUnlocked(state))
            }
        }
    }
}

@Composable
private fun SquadTab(onPick: (Int) -> Unit) {
    val repo = LocalRepo.current
    val rev by repo.rev.collectAsState()
    val state = repo.state
    var presetName by remember { mutableStateOf("") }
    GameCard {
        Text("Formation", style = MaterialTheme.typography.titleMedium)
        Text("Front row (slots 1–2) is targeted first. Back row (3–5) is safer.", style = MaterialTheme.typography.bodySmall, color = LocalExtraColors.current.inkSoft)
        Spacer(Modifier.height(10.dp))
        Text("FRONT", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = LocalExtraColors.current.inkFaint)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (0..1).forEach { slot -> SquadSlot(slot, Modifier.weight(1f), onPick) }
        }
        Spacer(Modifier.height(8.dp))
        Text("BACK", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = LocalExtraColors.current.inkFaint)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (2..4).forEach { slot -> SquadSlot(slot, Modifier.weight(1f), onPick) }
        }
        Spacer(Modifier.height(12.dp))
        val synergies = repo.engine.squadSynergy()
        if (synergies.isNotEmpty()) {
            Text("Synergies", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            synergies.forEach { Text("• $it", fontSize = 12.sp, color = Color(0xFF43A047), fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(8.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SoftButton("Smart Squad", { repo.act { repo.engine.smartSquad() } }, icon = Icons.Filled.AutoFixHigh, modifier = Modifier.weight(1f), enabled = "a6" in state.research || Prestige.automationUnlocked(state))
            SoftButton("Clear", { repo.act { for (i in 0..4) state.squad[i] = null } }, icon = Icons.Filled.Clear, modifier = Modifier.weight(1f))
        }
    }
    GameCard {
        Text("Loadout Presets", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        state.presets.forEach { p ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(p.name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, modifier = Modifier.weight(1f))
                SoftButton("Load", { repo.act { repo.engine.loadPreset(p.name) } })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(presetName, { presetName = it }, placeholder = { Text("Preset name", fontSize = 13.sp) }, singleLine = true, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), textStyle = MaterialTheme.typography.bodyMedium)
            SoftButton("Save", { if (presetName.isNotBlank()) { repo.act { repo.engine.savePreset(presetName) }; presetName = "" } })
        }
    }
}

@Composable
private fun SquadSlot(slot: Int, modifier: Modifier = Modifier, onPick: (Int) -> Unit) {
    val repo = LocalRepo.current
    val s = repo.state.slime(repo.state.squad[slot])
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable { onPick(slot) }.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (s != null) {
            SlimeView(s.visual(), Modifier.size(56.dp), radiusFactor = 0.32f, animate = false)
            Text(s.displayName, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        } else {
            Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Add, null, tint = LocalExtraColors.current.inkFaint) }
            Text("Empty", fontSize = 10.5.sp, color = LocalExtraColors.current.inkFaint)
        }
    }
}

@Composable
private fun TowerTab(nav: NavController) {
    val repo = LocalRepo.current
    val rev by repo.rev.collectAsState()
    GameCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Apartment, null, tint = Color(0xFF7C4DFF), modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Endless Tower", style = MaterialTheme.typography.titleLarge)
                Text("Floor ${repo.state.towerFloor} · Best ${repo.state.towerBest}", fontSize = 13.sp, color = LocalExtraColors.current.inkSoft, fontWeight = FontWeight.Bold)
            }
        }
        Text("Climb forever. Every 10th floor grants bonus rewards; every 25th drops an egg.", style = MaterialTheme.typography.bodySmall, color = LocalExtraColors.current.inkSoft)
        Spacer(Modifier.height(12.dp))
        PrimaryButton("Climb to floor ${repo.state.towerFloor + 1}", { repo.startTowerBattle(); nav.navigate("battle") }, modifier = Modifier.fillMaxWidth(), icon = Icons.Filled.TrendingUp)
    }
}

@Composable
private fun DungeonsTab(nav: NavController) {
    val repo = LocalRepo.current
    val rev by repo.rev.collectAsState()
    val state = repo.state
    GameCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Dungeons", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            ResourcePill(Icons.Filled.VpnKey, "${state.resources.keys}", Color(0xFFFFB300), "Keys")
        }
        Text("Keys refresh daily. Higher tiers need deeper zone progress.", style = MaterialTheme.typography.bodySmall, color = LocalExtraColors.current.inkSoft)
    }
    DungeonKind.entries.forEach { kind ->
        GameCard {
            Text(kind.displayName, style = MaterialTheme.typography.titleMedium, color = Color(kind.color))
            Text(kind.desc, style = MaterialTheme.typography.bodySmall, color = LocalExtraColors.current.inkSoft)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (0 until Dungeons.TIERS).forEach { tier ->
                    val open = Dungeons.unlocked(tier, state.highestZoneCleared())
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (open) Color(kind.color).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant).clickable(enabled = open) {
                            repo.startDungeonBattle(kind, tier); nav.navigate("battle")
                        }.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("T${tier + 1}", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = if (open) Color(kind.color) else LocalExtraColors.current.inkFaint)
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyTab() {
    val repo = LocalRepo.current
    val rev by repo.rev.collectAsState()
    val d = repo.state.daily
    GameCard {
        Text("Daily Challenge", style = MaterialTheme.typography.titleMedium)
        Text(repo.engine.dailyChallengeDesc(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(10.dp))
        LinearProgress(d.progress.toFloat() / d.target, Color(0xFFFFB300), Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        Text(if (d.claimed) "Completed! Rewards claimed." else "${d.progress} / ${d.target} · Reward: 1500 gold, 120 essence, 3 mutagen", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = if (d.claimed) Color(0xFF43A047) else LocalExtraColors.current.inkSoft)
    }
}