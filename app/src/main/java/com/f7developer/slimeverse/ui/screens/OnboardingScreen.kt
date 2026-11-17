package com.f7developer.slimeverse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.f7developer.slimeverse.game.Locus
import com.f7developer.slimeverse.ui.LocalRepo
import com.f7developer.slimeverse.ui.components.PrimaryButton
import com.f7developer.slimeverse.ui.components.SoftButton
import com.f7developer.slimeverse.ui.render.Mood
import com.f7developer.slimeverse.ui.render.SlimeView
import com.f7developer.slimeverse.ui.theme.LocalExtraColors

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val repo = LocalRepo.current
    val rev by repo.rev.collectAsState()
    var step by remember { mutableStateOf(0) }
    var happy by remember { mutableStateOf(false) }
    var hatched by remember { mutableStateOf(false) }
    val total = 5

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                SoftButton("Skip", onDone, tint = LocalExtraColors.current.inkSoft)
            }
            Spacer(Modifier.weight(1f))
            when (step) {
                0 -> {
                    SlimeView(mascotVisual, Modifier.size(180.dp).clickable { happy = !happy }, mood = if (happy) Mood.HAPPY else Mood.IDLE, radiusFactor = 0.34f)
                    Spacer(Modifier.height(16.dp))
                    Text("Welcome to Slimeverse", style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground)
                    Text("Collect adorable slimes, splice their genes, evolve spectacular forms, and lead your squad through an endless idle RPG. Tap the slime to say hi!", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = LocalExtraColors.current.inkSoft)
                }
                1 -> {
                    val egg = repo.state.eggs.firstOrNull()
                    Icon(Icons.Filled.Egg, null, tint = Color(0xFFEC407A), modifier = Modifier.size(120.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Hatch your first egg", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    Text("Eggs hatch over time — even while you're away. Hatch this one now to meet a new slime.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = LocalExtraColors.current.inkSoft)
                    Spacer(Modifier.height(16.dp))
                    if (egg != null && !hatched) {
                        PrimaryButton("Hatch now", { repo.act { repo.engine.hatchEgg(egg.uid, instant = true); hatched = true } }, icon = Icons.Filled.AutoAwesome)
                    } else {
                        Text("Hatched! Check your collection.", color = Color(0xFF43A047), fontWeight = FontWeight.Bold)
                    }
                }
                2 -> {
                    val s = repo.state.slimes.firstOrNull()
                    if (s != null) {
                        SlimeView(s.visual(), Modifier.size(150.dp), radiusFactor = 0.34f)
                        Spacer(Modifier.height(12.dp))
                        Text("Every slime has a genome", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                        Text("8 genes, each with a dominant and recessive allele. Dominant traits show; recessive traits can appear in offspring. Fuse and breed to discover rare combinations like Prismatic.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = LocalExtraColors.current.inkSoft)
                        Spacer(Modifier.height(10.dp))
                        Text("Example: ${s.genome.expressed(Locus.SIZE).displayName} size · ${s.genome.expressed(Locus.TEMPER).displayName} temper", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                3 -> {
                    Icon(Icons.Filled.Shield, null, tint = Color(0xFF7C4DFF), modifier = Modifier.size(100.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Build a squad of 5", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    Text("Two slimes guard the front row; three support from the back. Match elements for resonance bonuses and combine traits for powerful synergies. Your starter squad is already set.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = LocalExtraColors.current.inkSoft)
                }
                else -> {
                    Icon(Icons.Filled.Bolt, null, tint = Color(0xFFFFB300), modifier = Modifier.size(100.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Battle, idle, ascend", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    Text("Battles run automatically. Your squad keeps farming while you're offline. Clear zones, climb the tower, then Rebirth and Ascend for permanent power and new elements.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = LocalExtraColors.current.inkSoft)
                }
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(total) { i ->
                    Box(Modifier.size(if (i == step) 10.dp else 7.dp).clip(CircleShape).background(if (i == step) MaterialTheme.colorScheme.primary else LocalExtraColors.current.line))
                }
            }
            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                if (step == total - 1) "Start playing" else "Next",
                { if (step == total - 1) onDone() else step++ },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}