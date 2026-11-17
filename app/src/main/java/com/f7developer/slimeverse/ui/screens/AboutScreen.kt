package com.f7developer.slimeverse.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.f7developer.slimeverse.BuildConfig
import com.f7developer.slimeverse.ui.components.*
import com.f7developer.slimeverse.ui.theme.Fredoka
import com.f7developer.slimeverse.ui.theme.LocalExtraColors

@Composable
fun AboutScreen(nav: NavController) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(nav, "About")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // F7 Developer identity (official brand mark)
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color(0xFF0A0A14)).padding(28.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    F7Mark(Modifier.width(160.dp).height(118.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("D E V E L O P E R", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 4.sp)
                }
            }
            Text("Slimeverse: Idle Evolution RPG", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground)
            Text("Version ${BuildConfig.VERSION_NAME}", fontSize = 13.sp, color = LocalExtraColors.current.inkSoft, fontWeight = FontWeight.Bold)
            Text(
                "A deep idle monster-breeding RPG. Collect expressive slimes, experiment with dominant and recessive genes, fuse hybrid species, evolve spectacular forms, and lead your squad through zones, dungeons and an endless tower. Rebirth and Ascend for long-term power. Built to be played for months, not minutes.",
                style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Justify, color = LocalExtraColors.current.inkSoft,
            )
            GameCard(Modifier.fillMaxWidth()) {
                Text("A game by F7 Developer", style = MaterialTheme.typography.titleMedium)
                Text("Crafted with care for players worldwide. No pay-to-win — every slime and every evolution is earned through play.", style = MaterialTheme.typography.bodySmall, color = LocalExtraColors.current.inkSoft)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                PrimaryButton("Rate app", { rateApp(context) }, modifier = Modifier.weight(1f), icon = Icons.Filled.Star)
                SoftButton("Share", { shareApp(context) }, modifier = Modifier.weight(1f), icon = Icons.Filled.Share)
            }
            Text("© 2026 F7 Developer. All rights reserved.", fontSize = 13.sp, color = LocalExtraColors.current.inkFaint)
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Official F7 brand mark, redrawn as vector paths from the brand SVG. */
@Composable
fun F7Mark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 510f
        val sy = size.height / 375f
        scale(sx, sy, pivot = Offset.Zero) {
            val f = Path().apply {
                moveTo(5f, 40f); quadraticBezierTo(5f, 5f, 40f, 5f)
                lineTo(268f, 5f); lineTo(235f, 67f); lineTo(75f, 67f)
                lineTo(75f, 135f); lineTo(223f, 135f); lineTo(190f, 195f)
                lineTo(75f, 195f); lineTo(75f, 305f); lineTo(5f, 370f); close()
            }
            val seven = Path().apply {
                moveTo(290f, 5f); lineTo(505f, 5f); lineTo(505f, 65f)
                lineTo(330f, 370f); lineTo(255f, 370f); lineTo(425f, 67f); lineTo(258f, 67f); close()
            }
            drawPath(f, Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF1EEF7), Color(0xFFD9D3E6))))
            drawPath(seven, Brush.verticalGradient(listOf(Color(0xFFFFE6A3), Color(0xFFF2BF5E), Color(0xFFC98A2C), Color(0xFF9A621D))))
        }
    }
}