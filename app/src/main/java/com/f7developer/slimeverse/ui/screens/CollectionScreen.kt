package com.f7developer.slimeverse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.f7developer.slimeverse.game.*
import com.f7developer.slimeverse.ui.LocalRepo
import com.f7developer.slimeverse.ui.components.*
import com.f7developer.slimeverse.ui.render.SlimeView
import com.f7developer.slimeverse.ui.theme.LocalExtraColors

@Composable
fun CollectionScreen(nav: NavController) {
    val repo = LocalRepo.current
    val rev by repo.rev.collectAsState()
    val state = repo.state
    var elementFilter by remember { mutableStateOf<Element?>(null) }
    var rarityFilter by remember { mutableStateOf<Rarity?>(null) }
    var sort by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }

    val slimes = state.slimes
        .filter { elementFilter == null || it.element == elementFilter }
        .filter { rarityFilter == null || it.rarity == rarityFilter }
        .filter { query.isBlank() || it.displayName.contains(query, true) }
        .let { list ->
            when (sort) {
                0 -> list.sortedByDescending { repo.engine.slimePower(it) }
                1 -> list.sortedByDescending { it.level }
                2 -> list.sortedByDescending { it.rarity.order }
                else -> list.sortedByDescending { it.uid }
            }
        }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader("Slime Collection", "${state.slimes.size} slimes · ${state.codexSpecies.count { !it.contains(".t") }} species discovered")
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text("Search slimes…", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                FilterChip(elementFilter == null, { elementFilter = null }, { Text("All", fontSize = 13.sp) })
                Element.entries.take(5).forEach { el ->
                    FilterChip(elementFilter == el, { elementFilter = if (elementFilter == el) null else el }, { Text(el.short, fontSize = 13.sp) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf("Power", "Level", "Rarity", "Newest").forEachIndexed { i, label ->
                    FilterChip(sort == i, { sort = i }, { Text(label, fontSize = 13.sp) })
                }
            }
        }
        if (slimes.isEmpty()) {
            EmptyState(Icons.Filled.Pets, "No slimes match", "Hatch eggs, fuse, or breed to grow your collection.")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(slimes, key = { it.uid }) { s ->
                    SlimeCard(s, repo.engine.slimePower(s)) { nav.navigate("detail/${s.uid}") }
                }
            }
        }
    }
}

@Composable
fun SlimeCard(s: Slime, power: Long, onClick: () -> Unit) {
    val inSquad = LocalRepo.current.state.squad.contains(s.uid)
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(14.dp)).background(Color(s.element.color).copy(alpha = 0.10f))) {
                SlimeView(s.visual(), Modifier.fillMaxSize(), radiusFactor = 0.32f)
            }
            if (s.prismatic) {
                Icon(Icons.Filled.AutoAwesome, null, tint = Color(0xFFE040FB), modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(16.dp))
            }
            if (inSquad) {
                Box(Modifier.align(Alignment.TopStart).padding(4.dp).clip(CircleShape).background(Color(0xFF43A047)).padding(3.dp)) {
                    Icon(Icons.Filled.Shield, null, tint = Color.White, modifier = Modifier.size(10.dp))
                }
            }
            if (s.locked) {
                Icon(Icons.Filled.Lock, null, tint = Color(0xFFFFB300), modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).size(14.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(s.displayName, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(Color(s.rarity.color)))
            Text("Lv ${s.level}", fontSize = 13.5.sp, color = LocalExtraColors.current.inkSoft, fontWeight = FontWeight.SemiBold)
        }
        Text("${formatNum(power)} pwr", fontSize = 13.5.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
    }
}