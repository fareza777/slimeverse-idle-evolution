package com.f7developer.slimeverse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
fun FuseScreen(nav: NavController) {
    val repo = LocalRepo.current
    val rev by repo.rev.collectAsState()
    var tab by remember { mutableStateOf(0) }
    var uidA by remember { mutableStateOf<Long?>(null) }
    var uidB by remember { mutableStateOf<Long?>(null) }
    var picking by remember { mutableStateOf(0) } // 0 none, 1 A, 2 B
    var useMutagen by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Slime?>(null) }

    val a = repo.state.slime(uidA)
    val b = repo.state.slime(uidB)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeader("Gene Lab", "Splice, breed and mutate to discover new forms.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Fusion", "Breeding", "Auto-Rules").forEachIndexed { i, label ->
                FilterChip(tab == i, { tab = i }, { Text(label, fontSize = 13.sp) })
            }
        }

        when (tab) {
            0 -> {
                GameCard {
                    Text("Fusion Chamber", style = MaterialTheme.typography.titleLarge)
                    Text("Consumes both parents. The child inherits a mixed genome, a hybrid family, and a chance at higher rarity.", style = MaterialTheme.typography.bodySmall, color = LocalExtraColors.current.inkSoft)
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ParentSlot(a, "Parent A", Modifier.weight(1f)) { picking = 1 }
                        Icon(Icons.Filled.Add, null, tint = LocalExtraColors.current.inkFaint)
                        ParentSlot(b, "Parent B", Modifier.weight(1f)) { picking = 2 }
                    }
                    if (a != null && b != null) {
                        Spacer(Modifier.height(14.dp))
                        val (fam, el) = repo.engine.fusionPreview(a, b)
                        GameCard(color = MaterialTheme.colorScheme.surfaceVariant, corner = 14.dp) {
                            Text("Likely outcome", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("${fam.displayName} family · ${el.displayName} element", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text("Rarity: ${Rarity.of(maxOf(a.rarity.order, b.rarity.order)).displayName}+ · Mutation ${fmt0((Prestige.mutationChance(repo.state) + if (useMutagen) 0.2 else 0.0) * 100)}%", fontSize = 13.sp, color = LocalExtraColors.current.inkSoft)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(useMutagen, { useMutagen = it })
                            Text("Use 2 mutagen (+20% mutation, +rarity odds)", fontSize = 12.5.sp, color = LocalExtraColors.current.inkSoft)
                        }
                        Spacer(Modifier.height(8.dp))
                        val cost = Balance.fusionGold(a, b)
                        PrimaryButton("Fuse · ${formatNum(cost)} gold", {
                            repo.act {
                                val child = repo.engine.fuse(a.uid, b.uid, useMutagen)
                                if (child != null) { result = child; uidA = null; uidB = null }
                                else repo.toast("Not enough gold.")
                            }
                        }, modifier = Modifier.fillMaxWidth(), icon = Icons.Filled.Science)
                    }
                }
                FusionCodexCard()
            }
            1 -> {
                GameCard {
                    Text("Breeding Nest", style = MaterialTheme.typography.titleLarge)
                    Text("Parents are kept. The egg inherits one allele per gene from each parent (Mendelian), with a chance of mutation.", style = MaterialTheme.typography.bodySmall, color = LocalExtraColors.current.inkSoft)
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ParentSlot(a, "Mother", Modifier.weight(1f)) { picking = 1 }
                        Icon(Icons.Filled.Favorite, null, tint = Color(0xFFEC407A))
                        ParentSlot(b, "Father", Modifier.weight(1f)) { picking = 2 }
                    }
                    if (a != null && b != null) {
                        Spacer(Modifier.height(12.dp))
                        val (g, e) = repo.engine.breedCost(a, b)
                        Text("Queue ${repo.state.breedJobs.size}/${Prestige.breedQueueCap(repo.state)} · gestation 2m then hatch", fontSize = 13.5.sp, color = LocalExtraColors.current.inkSoft)
                        Spacer(Modifier.height(8.dp))
                        PrimaryButton("Breed · ${formatNum(g)}g + ${formatNum(e)} essence", {
                            repo.act {
                                val job = repo.engine.breed(a.uid, b.uid)
                                if (job != null) { repo.toast("Egg conceived!"); uidA = null; uidB = null }
                                else repo.toast("Queue full or not affordable.")
                            }
                        }, modifier = Modifier.fillMaxWidth(), icon = Icons.Filled.Favorite, color = Color(0xFFEC407A))
                    }
                }
                if ("r3" in repo.state.research) {
                    GameCard { Text("Inheritance preview unlocked: each gene shows a 50/50 split from each parent, plus mutation odds.", style = MaterialTheme.typography.bodySmall, color = LocalExtraColors.current.inkSoft) }
                }
            }
            else -> {
                val rules = repo.state.autoFuse
                GameCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Auto-Fusion Rules", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Switch(rules.enabled, { repo.act { repo.state.autoFuse.enabled = it } })
                    }
                    Text("Automatically fuse surplus duplicates. Protected slimes are never consumed.", style = MaterialTheme.typography.bodySmall, color = LocalExtraColors.current.inkSoft)
                    Spacer(Modifier.height(12.dp))
                    Text("Keep at most ${rules.maxCopies} copies of a species", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Slider(rules.maxCopies.toFloat(), { repo.act { repo.state.autoFuse.maxCopies = it.toInt() } }, valueRange = 1f..6f, steps = 4)
                    Text("Never fuse rarity ≥ ${Rarity.of(rules.keepRarityAtLeast).displayName}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Slider(rules.keepRarityAtLeast.toFloat(), { repo.act { repo.state.autoFuse.keepRarityAtLeast = it.toInt() } }, valueRange = 1f..6f, steps = 4)
                    RuleToggle("Protect Prismatics", rules.keepPrismatic) { repo.act { repo.state.autoFuse.keepPrismatic = it } }
                    RuleToggle("Protect locked slimes", rules.keepLocked) { repo.act { repo.state.autoFuse.keepLocked = it } }
                    RuleToggle("Protect squad members", rules.keepSquad) { repo.act { repo.state.autoFuse.keepSquad = it } }
                    if (!("a4" in repo.state.research) && !Prestige.automationUnlocked(repo.state)) {
                        Text("Unlock via Automation research (Auto-Fuse Rules).", fontSize = 13.5.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (picking > 0) {
        SlimePickerDialog(
            title = if (picking == 1) "Choose first parent" else "Choose second parent",
            filter = { it.uid != (if (picking == 1) uidB else uidA) },
            onPick = { uid -> if (picking == 1) uidA = uid else uidB = uid; picking = 0 },
            onDismiss = { picking = 0 },
        )
    }
    result?.let { child ->
        AlertDialog(
            onDismissRequest = { result = null },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("A new slime!", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    SlimeView(child.visual(), Modifier.size(150.dp), mood = com.f7developer.slimeverse.ui.render.Mood.HAPPY, radiusFactor = 0.34f)
                    Text(child.displayName, style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ElementBadge(child.element); RarityBadge(child.rarity)
                    }
                    if (child.def.family.hybrid) Text("Hybrid family discovered!", color = Color(0xFFE040FB), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { val u = child.uid; result = null; nav.navigate("detail/$u") }) { Text("View", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { result = null }) { Text("Keep breeding") } },
        )
    }
}

@Composable
private fun ParentSlot(s: Slime?, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable(onClick = onClick).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (s != null) {
            SlimeView(s.visual(), Modifier.size(72.dp), radiusFactor = 0.32f, animate = false)
            Text(s.displayName, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, maxLines = 1)
            Text("Lv ${s.level}", fontSize = 12.5.sp, color = LocalExtraColors.current.inkSoft)
        } else {
            Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) { Icon(Icons.Filled.AddCircleOutline, null, tint = LocalExtraColors.current.inkFaint, modifier = Modifier.size(36.dp)) }
            Text(label, fontSize = 13.5.sp, color = LocalExtraColors.current.inkSoft, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun RuleToggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 13.sp, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
        Switch(value, onChange)
    }
}

@Composable
private fun FusionCodexCard() {
    val repo = LocalRepo.current
    val rev by repo.rev.collectAsState()
    val discovered = repo.state.codexFusions.size
    val total = Families.fusionPairs.size
    GameCard {
        Text("Fusion Codex · $discovered / $total", style = MaterialTheme.typography.titleMedium)
        Text("Discover which family pairs produce hybrids. Same-family fusions boost rarity.", style = MaterialTheme.typography.bodySmall, color = LocalExtraColors.current.inkSoft)
        Spacer(Modifier.height(8.dp))
        LinearProgress(discovered.toFloat() / total, Color(0xFFE040FB), Modifier.fillMaxWidth())
    }
}