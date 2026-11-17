package com.f7developer.slimeverse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.f7developer.slimeverse.ui.render.Mood
import com.f7developer.slimeverse.ui.render.SlimeView
import com.f7developer.slimeverse.ui.theme.LocalExtraColors

@Composable
fun SlimeDetailScreen(uid: Long, nav: NavController) {
    val repo = LocalRepo.current
    val rev by repo.rev.collectAsState()
    val s = repo.state.slime(uid)
    var showRelease by remember { mutableStateOf(false) }
    var showMutate by remember { mutableStateOf(false) }
    var showSquad by remember { mutableStateOf(false) }
    var evolveBranch by remember { mutableStateOf<String?>(null) }

    if (s == null) {
        LaunchedEffect(Unit) { nav.popBackStack() }
        return
    }
    val stats = Balance.statsOf(s, Prestige.ascensionMult(repo.state) * Prestige.stardustMult(repo.state), Prestige.globalStatMods(repo.state) + Prestige.researchStatMods(repo.state), Prestige.masteryOf(repo.state, s.element))
    val skill = Skills.scaled(s.def.skillId, s.tier, s.branch, Trait.ELEMENTAL_MASTER in s.traits)
    val xpNeed = Balance.xpToNext(s.level)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text(s.displayName, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f), maxLines = 1)
            IconButton(onClick = { repo.act { repo.state.slime(uid)?.locked = !s.locked } }) {
                Icon(if (s.locked) Icons.Filled.Lock else Icons.Filled.LockOpen, "Lock", tint = if (s.locked) Color(0xFFFFB300) else LocalExtraColors.current.inkFaint)
            }
        }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // portrait
            Box(Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(24.dp)).background(Color(s.element.color).copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                SlimeView(s.visual(), Modifier.size(200.dp), mood = Mood.HAPPY, radiusFactor = 0.34f)
                if (s.prismatic) Text("PRISMATIC", Modifier.align(Alignment.TopEnd).padding(12.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE040FB).copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 4.dp), color = Color(0xFFE040FB), fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                ElementBadge(s.element)
                if (s.secondary != null) ElementBadge(s.secondary!!, small = true)
                RarityBadge(s.rarity)
                Text("Tier ${s.tier}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LocalExtraColors.current.inkSoft)
                Spacer(Modifier.weight(1f))
                Text("${formatNum(repo.engine.slimePower(s))} pwr", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            }
            Text(s.def.family.displayName + " family · " + s.def.name, style = MaterialTheme.typography.bodySmall, color = LocalExtraColors.current.inkSoft)

            // stats
            GameCard {
                Text("Stats", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip("HP", formatNum(stats.hp.toLong()), Color(0xFF66BB6A), Modifier.weight(1f))
                    StatChip("ATK", formatNum(stats.atk.toLong()), Color(0xFFFF7043), Modifier.weight(1f))
                    StatChip("DEF", formatNum(stats.def.toLong()), Color(0xFF42A5F5), Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip("SPD", String.format("%.1f", stats.spd), Color(0xFFFFCA28), Modifier.weight(1f))
                    StatChip("CRIT", "${stats.crit.toInt()}%", Color(0xFFEC407A), Modifier.weight(1f))
                    StatChip("LEVEL", "${s.level}", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("XP ${formatNum(s.xp)} / ${formatNum(xpNeed)}", fontSize = 12.sp, color = LocalExtraColors.current.inkSoft, fontWeight = FontWeight.SemiBold)
                        LinearProgress(s.xp.toFloat() / xpNeed, MaterialTheme.colorScheme.primary, Modifier.fillMaxWidth().padding(top = 4.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    var feedCost = Balance.feedCost(s.level)
                    if (Trait.METABOLIC in s.traits) feedCost = (feedCost * 0.7).toLong()
                    SoftButton("Feed · ${formatNum(feedCost)}g", {
                        repo.act { if (!repo.engine.feed(uid)) repo.toast("Not enough gold or max level.") }
                    }, icon = Icons.Filled.Restaurant, tint = Color(0xFFFFB300), enabled = s.level < Balance.MAX_LEVEL)
                }
            }

            // evolution
            if (s.tier < 2) {
                val needLevel = if (s.tier == 0) Balance.EVOLVE_1_LEVEL else Balance.EVOLVE_2_LEVEL
                val options = Species.evolveOptions(s.def)
                GameCard {
                    Text("Evolution", style = MaterialTheme.typography.titleMedium)
                    Text("Requires level $needLevel · ${formatNum(Balance.evolveEssence(s.tier, s.rarity))} essence · ${formatNum(Balance.evolveGold(s.tier, s.rarity))} gold", fontSize = 12.sp, color = LocalExtraColors.current.inkSoft)
                    Spacer(Modifier.height(10.dp))
                    options.forEach { opt ->
                        val sel = evolveBranch == opt.branch
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant).clickable { evolveBranch = opt.branch }.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SlimeView(BattleFactory.visualOf(s.copy(tier = opt.tier, branch = opt.branch), uid.toInt()), Modifier.size(48.dp), radiusFactor = 0.32f, animate = false)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(opt.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(if (opt.branch == "a") "Bulwark path: HP & DEF" else "Striker path: ATK & SPD", fontSize = 11.5.sp, color = LocalExtraColors.current.inkSoft)
                            }
                            if (sel) Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    PrimaryButton(
                        if (s.level < needLevel) "Reach level $needLevel" else "Evolve",
                        {
                            repo.act {
                                val branch = evolveBranch ?: "a"
                                if (repo.engine.evolve(uid, branch)) repo.toast("Evolved into ${repo.state.slime(uid)?.displayName}!")
                                else repo.toast("Cannot evolve yet.")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = repo.engine.canEvolve(s),
                        icon = Icons.Filled.AutoAwesome,
                    )
                }
            }

            // skill
            GameCard {
                Text("Skill · ${skill.name}", style = MaterialTheme.typography.titleMedium)
                Text(skill.desc, style = MaterialTheme.typography.bodySmall, color = LocalExtraColors.current.inkSoft)
                Spacer(Modifier.height(6.dp))
                Text("Cooldown ${skill.cooldown} turns · Power ${String.format("%.2f", skill.power)}x", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            // traits
            if (s.traits.isNotEmpty()) {
                GameCard {
                    Text("Traits", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    s.traits.forEach { t ->
                        Text(t.displayName, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        Text(t.desc, fontSize = 12.sp, color = LocalExtraColors.current.inkSoft)
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }

            // genome
            GameCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Genome", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    SoftButton("Mutate · 3", { showMutate = true }, icon = Icons.Filled.Science, tint = Color(0xFFAB47BC))
                }
                Text("Dominant allele is expressed; recessive can pass to offspring.", fontSize = 11.5.sp, color = LocalExtraColors.current.inkSoft)
                Spacer(Modifier.height(8.dp))
                Locus.entries.forEach { locus ->
                    val pair = s.genome.pairs.getValue(locus)
                    val expressed = s.genome.expressed(locus)
                    val a = Genes.allele(pair.first); val b = Genes.allele(pair.second)
                    val recessive = if (expressed.id == a.id) b else a
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(locus.displayName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LocalExtraColors.current.inkSoft, modifier = Modifier.width(70.dp))
                        Box(Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text(expressed.displayName + (if (expressed.rare) " ★" else ""), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("(${recessive.displayName})", fontSize = 11.sp, color = LocalExtraColors.current.inkFaint)
                    }
                }
            }

            // actions
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SoftButton("Squad", { showSquad = true }, icon = Icons.Filled.Shield, modifier = Modifier.weight(1f))
                SoftButton("Habitat", {
                    repo.act {
                        val free = repo.state.habitats.indexOfFirst { it.assigned.size < Habitats.capacity(it.level) }
                        if (free >= 0) { repo.engine.assignHabitat(free, uid); repo.toast("Assigned to ${HabitatKind.entries[free].displayName}.") }
                        else repo.toast("All habitats full.")
                    }
                }, icon = Icons.Filled.Park, modifier = Modifier.weight(1f))
                SoftButton("Release", { showRelease = true }, icon = Icons.Filled.Delete, tint = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showRelease) {
        ConfirmDialog("Release ${s.displayName}?", "This slime will be released for essence. This cannot be undone.", "Release", {
            repo.act { val r = repo.engine.release(uid); if (r > 0) { repo.toast("Released for $r essence."); nav.popBackStack() } else repo.toast("Remove from squad first.") }
            showRelease = false
        }, { showRelease = false }, danger = true)
    }
    if (showMutate) {
        AlertDialog(
            onDismissRequest = { showMutate = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Mutate a gene", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Spend 3 mutagen to reroll one allele. Rare alleles are more likely.", fontSize = 12.5.sp, color = LocalExtraColors.current.inkSoft)
                    Locus.entries.forEach { locus ->
                        SoftButton(locus.displayName, {
                            repo.act { if (repo.engine.mutate(uid, locus)) repo.toast("Gene mutated!") else repo.toast("Need 3 mutagen.") }
                            showMutate = false
                        }, modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showMutate = false }) { Text("Close") } },
        )
    }
    if (showSquad) {
        AlertDialog(
            onDismissRequest = { showSquad = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Add to squad", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Slots 1–2 are the front row; 3–5 are the back row.", fontSize = 12.5.sp, color = LocalExtraColors.current.inkSoft)
                    (0..4).forEach { slot ->
                        val occupant = repo.state.slime(repo.state.squad[slot])
                        SoftButton("Slot ${slot + 1}${occupant?.let { " · ${it.displayName}" } ?: ""}", {
                            repo.act { repo.engine.setSquadSlot(slot, uid) }
                            showSquad = false
                        }, modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSquad = false }) { Text("Close") } },
        )
    }
}