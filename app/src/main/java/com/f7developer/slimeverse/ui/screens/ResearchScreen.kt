package com.f7developer.slimeverse.ui.screens

import androidx.compose.foundation.background
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
import com.f7developer.slimeverse.ui.theme.LocalExtraColors

@Composable
fun ResearchScreen(nav: NavController) {
    val repo = LocalRepo.current
    val rev by repo.rev.collectAsState()
    val state = repo.state
    var branch by remember { mutableStateOf(ResearchBranch.BATTLE) }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(nav, "Research Lab")
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ResourcePill(Icons.Filled.School, formatNum(state.resources.research), Color(0xFF42A5F5), "RP")
            ResourcePill(Icons.Filled.Paid, formatNum(state.resources.gold), Color(0xFFFFB300), "Gold")
        }
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ResearchBranch.entries.forEach { b ->
                FilterChip(branch == b, { branch = b }, { Text(b.displayName, fontSize = 12.sp) })
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Research.all.filter { it.branch == branch }.forEach { node ->
                val done = node.id in state.research
                val reqMet = node.requires == null || node.requires in state.research
                val affordable = state.resources.research >= node.rpCost && state.resources.gold >= node.goldCost
                GameCard(color = if (done) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(node.name, style = MaterialTheme.typography.titleMedium, color = if (done) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface)
                            Text(node.desc, fontSize = 12.5.sp, color = LocalExtraColors.current.inkSoft)
                            if (!done) Text("${node.rpCost} RP · ${formatNum(node.goldCost)} gold", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(branch.color))
                        }
                        if (done) Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF43A047))
                        else SoftButton("Research", {
                            repo.act { if (repo.engine.doResearch(node.id)) repo.toast("Researched ${node.name}!") else repo.toast("Locked or not affordable.") }
                        }, enabled = reqMet && affordable, tint = Color(branch.color))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun TalentsScreen(nav: NavController) {
    val repo = LocalRepo.current
    val rev by repo.rev.collectAsState()
    val state = repo.state
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(nav, "Talents & Mastery")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GameCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Breeder Talents", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    ResourcePill(Icons.Filled.Stars, "${state.resources.talentPoints}", Color(0xFFFFB300), "Points")
                }
                Text("Earn points from breeder levels and achievements.", fontSize = 12.sp, color = LocalExtraColors.current.inkSoft)
            }
            BreederTalents.all.forEach { t ->
                val level = state.talents[t.id] ?: 0
                GameCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${t.name} · Lv $level/${t.maxLevel}", style = MaterialTheme.typography.titleSmall)
                            Text(t.desc, fontSize = 12.sp, color = LocalExtraColors.current.inkSoft)
                        }
                        SoftButton("+${t.costPerLevel}", {
                            repo.act { if (!repo.engine.learnTalent(t.id)) repo.toast("No talent points.") }
                        }, enabled = level < t.maxLevel && state.resources.talentPoints >= t.costPerLevel, tint = Color(0xFF66BB6A))
                    }
                }
            }
            SectionHeader("Element Mastery", "Battle with an element to master it (+3% stats per level).")
            Element.entries.forEach { el ->
                val level = state.masteryLevel[el] ?: 0
                val xp = state.masteryXp[el] ?: 0L
                val need = Mastery.xpToLevel(level)
                GameCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ElementBadge(el)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Lv $level/${Mastery.MAX} · +${(level * 3)}% stats", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            if (level < Mastery.MAX) LinearProgress(xp.toFloat() / need, Color(el.color), Modifier.fillMaxWidth().padding(top = 4.dp), 6.dp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}