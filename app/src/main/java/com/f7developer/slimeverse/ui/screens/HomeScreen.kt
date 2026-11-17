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
fun HomeScreen(nav: NavController) {
    val repo = LocalRepo.current
    val rev by repo.rev.collectAsState()
    val state = repo.state
    var assignHabitat by remember { mutableStateOf(-1) }
    var showExpedition by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeader("Your Nest", "Squad power ${formatNum(repo.engine.squadPower())}")
        // Living idle-farm terrarium
        val herd = repo.state.squadSlimes().map { it.visual() }.ifEmpty { listOf(mascotVisual) }
        com.f7developer.slimeverse.ui.render.HabitatScene(
            herd,
            Modifier.fillMaxWidth().height(210.dp),
            producing = repo.state.habitats.any { it.assigned.isNotEmpty() } || repo.state.squadSlimes().isNotEmpty(),
        )
        // Hatchery
        GameCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Egg, null, tint = Color(0xFFEC407A))
                Spacer(Modifier.width(8.dp))
                Text("Hatchery", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                Text("${state.eggs.size} eggs", color = LocalExtraColors.current.inkSoft, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            if (state.eggs.isEmpty() && state.breedJobs.isEmpty()) {
                Text("No eggs yet. Win battles, fuse slimes, or breed a pair to find eggs.", style = MaterialTheme.typography.bodySmall, color = LocalExtraColors.current.inkSoft)
            } else {
                state.breedJobs.forEach { job ->
                    val remain = ((job.completesAt - System.currentTimeMillis()) / 60000f).coerceAtLeast(0f)
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Favorite, null, tint = Color(0xFFEC407A), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Breeding…", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.weight(1f))
                        Text("${fmt1(remain.toDouble())}m", fontSize = 13.5.sp, color = LocalExtraColors.current.inkSoft)
                    }
                }
                state.eggs.forEach { egg ->
                    val total = (egg.hatchAt - egg.startedAt).coerceAtLeast(1)
                    val prog = (1f - (egg.hatchAt - System.currentTimeMillis()).toFloat() / total).coerceIn(0f, 1f)
                    val ready = egg.hatchAt <= System.currentTimeMillis()
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color(egg.rarity.color).copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Egg, null, tint = Color(egg.rarity.color), modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(Species.base(egg.speciesId).name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, maxLines = 1)
                            LinearProgress(prog, Color(egg.rarity.color), Modifier.fillMaxWidth().padding(top = 4.dp), 6.dp)
                        }
                        Spacer(Modifier.width(8.dp))
                        if (ready) {
                            SoftButton("Hatch", { repo.act { repo.engine.hatchEgg(egg.uid) } }, tint = Color(0xFF43A047))
                        } else {
                            val cost = repo.engine.hatchCost(egg)
                            SoftButton("${formatNum(cost)}g", {
                                repo.act {
                                    if (state.resources.gold >= cost) { state.resources.gold -= cost; repo.engine.hatchEgg(egg.uid, instant = true) }
                                    else repo.toast("Not enough gold.")
                                }
                            }, tint = Color(0xFFFFB300))
                        }
                    }
                }
            }
        }

        // Habitats
        SectionHeader("Habitats", "Assign slimes to produce resources while you play or rest.")
        HabitatKind.entries.forEachIndexed { k, kind ->
            val h = state.habitats[k]
            val rate = repo.engine.habitatRate(k)
            GameCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color(kind.color).copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Park, null, tint = Color(kind.color), modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${kind.displayName} · Lv ${h.level}", style = MaterialTheme.typography.titleMedium)
                        Text("${kind.produces} · ${fmt1(rate)}/hr", fontSize = 13.5.sp, color = LocalExtraColors.current.inkSoft, fontWeight = FontWeight.SemiBold)
                    }
                    SoftButton("Upgrade", {
                        repo.act { if (!repo.engine.upgradeHabitat(k)) repo.toast("Need ${formatNum(Habitats.upgradeCost(h.level))} gold.") }
                    }, tint = Color(kind.color))
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                        items(h.assigned) { uid ->
                            val s = state.slime(uid)
                            if (s != null) {
                                Box(Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable { repo.act { repo.engine.unassignHabitat(k, uid) } }) {
                                    SlimeView(s.visual(), Modifier.fillMaxSize(), radiusFactor = 0.32f)
                                }
                            }
                        }
                        if (h.assigned.size < Habitats.capacity(h.level)) {
                            item {
                                Box(Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable { assignHabitat = k }, contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Add, "Assign", tint = LocalExtraColors.current.inkFaint)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(formatNum(h.stored.toLong()), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(kind.color))
                        SoftButton("Collect", { repo.act { val a = repo.engine.collectHabitat(k); if (a == 0L) repo.toast("Nothing stored yet.") } }, tint = Color(kind.color))
                    }
                }
            }
        }

        // Expeditions
        SectionHeader("Expeditions", "Send slimes on timed missions. Rewards arrive even offline.") {
            SoftButton("Send", { showExpedition = true }, icon = Icons.Filled.Send)
        }
        if (state.expeditions.isEmpty()) {
            GameCard { Text("No active expeditions. Send a squad to gather gold, essence, mutagen and research.", style = MaterialTheme.typography.bodySmall, color = LocalExtraColors.current.inkSoft) }
        } else {
            state.expeditions.forEach { exp ->
                val def = Expeditions.get(exp.defId)
                val total = (exp.endsAt - exp.startedAt).coerceAtLeast(1)
                val prog = (1f - (exp.endsAt - System.currentTimeMillis()).toFloat() / total).coerceIn(0f, 1f)
                val ready = exp.endsAt <= System.currentTimeMillis()
                GameCard {
                    com.f7developer.slimeverse.ui.render.ExpeditionMarch(
                        exp.slimeUids.mapNotNull { state.slime(it)?.visual() },
                        prog,
                        Modifier.fillMaxWidth().height(64.dp),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(def.name, style = MaterialTheme.typography.titleMedium)
                            Text("${exp.slimeUids.size} slimes · ${def.minutes}m", fontSize = 13.5.sp, color = LocalExtraColors.current.inkSoft)
                            LinearProgress(prog, MaterialTheme.colorScheme.tertiary, Modifier.fillMaxWidth().padding(top = 6.dp), 6.dp)
                        }
                        Spacer(Modifier.width(10.dp))
                        if (ready) SoftButton("Claim", { repo.act { repo.engine.claimExpedition(exp.uid) } }, tint = Color(0xFF43A047))
                        else Text("${((exp.endsAt - System.currentTimeMillis()) / 60000)}m left", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = LocalExtraColors.current.inkSoft)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (assignHabitat >= 0) {
        SlimePickerDialog(
            title = "Assign to ${HabitatKind.entries[assignHabitat].displayName}",
            filter = { it.habitat == -1 },
            onPick = { uid -> repo.act { repo.engine.assignHabitat(assignHabitat, uid) }; assignHabitat = -1 },
            onDismiss = { assignHabitat = -1 },
        )
    }
    if (showExpedition) {
        ExpeditionDialog(onDismiss = { showExpedition = false })
    }
}

@Composable
fun SlimePickerDialog(title: String, filter: (Slime) -> Boolean, onPick: (Long) -> Unit, onDismiss: () -> Unit, multi: Boolean = false, onMulti: ((List<Long>) -> Unit)? = null) {
    val repo = LocalRepo.current
    val rev by repo.rev.collectAsState()
    val candidates = repo.state.slimes.filter(filter).sortedByDescending { repo.engine.slimePower(it) }
    val selected = remember { mutableStateListOf<Long>() }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (candidates.isEmpty()) Text("No eligible slimes.", color = LocalExtraColors.current.inkSoft)
                candidates.forEach { s ->
                    val sel = s.uid in selected
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant).clickable {
                            if (multi) { if (sel) selected.remove(s.uid) else selected.add(s.uid) } else onPick(s.uid)
                        }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SlimeView(s.visual(), Modifier.size(40.dp), radiusFactor = 0.32f, animate = false)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.displayName, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, maxLines = 1)
                            Text("Lv ${s.level} · ${formatNum(repo.engine.slimePower(s))} pwr", fontSize = 13.sp, color = LocalExtraColors.current.inkSoft)
                        }
                        if (multi && sel) Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        confirmButton = {
            if (multi) TextButton(onClick = { onMulti?.invoke(selected.toList()); onDismiss() }) { Text("Send", fontWeight = FontWeight.Bold) }
            else TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun ExpeditionDialog(onDismiss: () -> Unit) {
    val repo = LocalRepo.current
    val rev by repo.rev.collectAsState()
    var defId by remember { mutableStateOf(Expeditions.all[0].id) }
    var picking by remember { mutableStateOf(false) }
    val def = Expeditions.get(defId)
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("New Expedition", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Expeditions.all.forEach { d ->
                    val sel = d.id == defId
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant).clickable { defId = d.id }.padding(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(d.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${d.minutes}m · min ${formatNum(d.minPower)} pwr", fontSize = 13.sp, color = LocalExtraColors.current.inkSoft)
                        }
                        if (sel) Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Text(def.desc, fontSize = 12.5.sp, color = LocalExtraColors.current.inkSoft)
            }
        },
        confirmButton = { TextButton(onClick = { picking = true }) { Text("Choose slimes", fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    if (picking) {
        SlimePickerDialog(
            title = "Send on ${def.name}",
            filter = { s -> s.habitat == -1 && repo.state.expeditions.none { s.uid in it.slimeUids } },
            onPick = {},
            multi = true,
            onMulti = { uids ->
                repo.act {
                    if (repo.engine.startExpedition(defId, uids)) repo.toast("Expedition launched!")
                    else repo.toast("Need ${formatNum(def.minPower)} total power and a free slot.")
                }
                onDismiss()
            },
            onDismiss = { picking = false },
        )
    }
}