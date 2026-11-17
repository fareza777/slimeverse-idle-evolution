package com.f7developer.slimeverse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.f7developer.slimeverse.game.Prestige
import com.f7developer.slimeverse.ui.LocalRepo
import com.f7developer.slimeverse.ui.components.*
import com.f7developer.slimeverse.ui.theme.LocalExtraColors

@Composable
fun MoreScreen(nav: NavController) {
    val repo = LocalRepo.current
    val rev by repo.rev.collectAsState()
    val state = repo.state
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Progression", "Breeder Lv ${state.breederLevel} · ${state.resources.talentPoints} talent points")
        GameCard {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip("Rebirths", "${state.rebirths}", Color(0xFFFFB300), Modifier.weight(1f))
                StatChip("Ascensions", "${state.ascensions}", Color(0xFFE040FB), Modifier.weight(1f))
                StatChip("Species", "${state.codexSpecies.count { !it.contains(".t") }}", Color(0xFF26C6DA), Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip("Stardust", formatNum(state.lifetimeStardust), Color(0xFFFFCA28), Modifier.weight(1f))
                StatChip("Aether", formatNum(state.lifetimeAether), Color(0xFFE040FB), Modifier.weight(1f))
                StatChip("Tower", "${state.towerBest}", Color(0xFF7C4DFF), Modifier.weight(1f))
            }
        }
        MenuItem(Icons.Filled.School, "Research Lab", "${state.research.size}/24 unlocked", Color(0xFF42A5F5)) { nav.navigate("research") }
        MenuItem(Icons.Filled.Psychology, "Talents & Mastery", "${state.resources.talentPoints} points to spend", Color(0xFF66BB6A)) { nav.navigate("talents") }
        MenuItem(Icons.Filled.MenuBook, "Evolution Codex", "${state.codexSpecies.size} forms recorded", Color(0xFF26C6DA)) { nav.navigate("codex") }
        MenuItem(Icons.Filled.EmojiEvents, "Achievements", "${state.achievementsClaimed.size} claimed", Color(0xFFFFB300)) { nav.navigate("achievements") }
        MenuItem(Icons.Filled.AutoAwesome, "Rebirth & Ascension", if (repo.engine.canAscend()) "Ascension ready!" else "Prestige layers", Color(0xFFE040FB)) { nav.navigate("ascension") }
        MenuItem(Icons.Filled.Settings, "Settings", "Theme, sound, save data", Color(0xFF90A4AE)) { nav.navigate("settings") }
        MenuItem(Icons.Filled.Info, "About", "F7 Developer", Color(0xFF7C4DFF)) { nav.navigate("about") }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MenuItem(icon: ImageVector, title: String, subtitle: String, tint: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GameCard(modifier = Modifier.fillMaxWidth(), corner = 18.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(subtitle, fontSize = 12.sp, color = LocalExtraColors.current.inkSoft, fontWeight = FontWeight.SemiBold)
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = LocalExtraColors.current.inkFaint, modifier = Modifier.size(16.dp))
            }
        }
    }
}