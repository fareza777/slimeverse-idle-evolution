package com.f7developer.slimeverse.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.f7developer.slimeverse.game.*

@Composable
fun ScreenHeader(nav: NavController, title: String) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        Text(title, style = MaterialTheme.typography.headlineSmall)
    }
}

fun Slime.visual(): UnitVisual = BattleFactory.visualOf(this, uid.toInt())

fun elementColor(el: Element): Color = Color(el.color)
fun rarityColor(r: Rarity): Color = Color(r.color)

val mascotVisual = UnitVisual(
    family = Family.BLOB, element = Element.AQUA, secondary = null, tier = 1, rarity = Rarity.EPIC,
    size = "standard", temper = "playful", hide = "smooth", aura = "lumen", eyes = "round",
    crown = "none", pattern = "spots", core = "soft", prismatic = false, seed = 7,
)