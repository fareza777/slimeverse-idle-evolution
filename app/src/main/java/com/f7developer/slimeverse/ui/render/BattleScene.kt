package com.f7developer.slimeverse.ui.render

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f7developer.slimeverse.data.ActiveBattle
import com.f7developer.slimeverse.data.BattleMode
import com.f7developer.slimeverse.data.BattleOutcome
import com.f7developer.slimeverse.game.*
import com.f7developer.slimeverse.ui.components.PrimaryButton
import com.f7developer.slimeverse.ui.components.SoftButton
import com.f7developer.slimeverse.ui.components.formatNum
import kotlinx.coroutines.flow.SharedFlow
import kotlin.math.sin
import kotlin.random.Random

private data class FloatText(val x: Float, val y: Float, val text: String, val color: Color, val birth: Long, val big: Boolean)
private data class Burst(val x: Float, val y: Float, val color: Color, val birth: Long, val seed: Int)
private data class Projectile(val x0: Float, val y0: Float, val x1: Float, val y1: Float, val color: Color, val birth: Long)

private data class UnitAnim(var flashUntil: Long = 0, var lungeUntil: Long = 0, var lungeDir: Float = 1f, var mood: Mood = Mood.IDLE, var deadAt: Long = 0)

@Composable
fun BattleScene(
    ab: ActiveBattle,
    events: SharedFlow<List<BattleEvent>>,
    outcome: BattleOutcome?,
    speed: Int,
    maxSpeed: Int,
    onSpeed: (Int) -> Unit,
    onExit: () -> Unit,
    onContinue: () -> Unit,
    showDamage: Boolean,
) {
    val zone = Zones.get(ab.zone)
    var now by remember { mutableStateOf(0L) }
    val anims = remember { mutableStateMapOf<Int, UnitAnim>() }
    val floats = remember { mutableStateListOf<FloatText>() }
    val bursts = remember { mutableStateListOf<Burst>() }
    val projectiles = remember { mutableStateListOf<Projectile>() }
    val shake = remember { mutableStateOf(0f) }
    val unitByIndex = remember(ab.seed) { ab.battle.units.associateBy { it.index } }

    LaunchedEffect(Unit) {
        while (true) {
            androidx.compose.runtime.withFrameMillis { now = it }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val density = LocalDensity.current.density

        fun unitPos(index: Int): Offset {
            val isPlayer = index < 100
            return if (isPlayer) {
                when (index) {
                    0 -> Offset(w * 0.27f, h * 0.40f)
                    1 -> Offset(w * 0.27f, h * 0.66f)
                    2 -> Offset(w * 0.12f, h * 0.28f)
                    3 -> Offset(w * 0.12f, h * 0.52f)
                    else -> Offset(w * 0.12f, h * 0.76f)
                }
            } else {
                val i = index - 100
                when {
                    i == 0 -> Offset(w * 0.73f, h * 0.42f)
                    i == 1 -> Offset(w * 0.73f, h * 0.68f)
                    i == 2 -> Offset(w * 0.88f, h * 0.30f)
                    i == 3 -> Offset(w * 0.88f, h * 0.54f)
                    else -> Offset(w * 0.88f, h * 0.78f)
                }
            }
        }

        LaunchedEffect(ab.seed) {
            events.collect { list ->
                for (e in list) {
                    when (e) {
                        is BattleEvent.Attack -> {
                            val from = unitPos(e.from); val to = unitPos(e.to)
                            val a = anims.getOrPut(e.from) { UnitAnim() }
                            a.lungeUntil = now + 220; a.lungeDir = if (e.from < 100) 1f else -1f; a.mood = Mood.ATTACK
                            val t = anims.getOrPut(e.to) { UnitAnim() }
                            t.flashUntil = now + 180; t.mood = Mood.HURT
                            projectiles.add(Projectile(from.x, from.y, to.x, to.y, Color(e.element.color), now))
                            bursts.add(Burst(to.x, to.y, Color(e.element.color), now + 160, e.to))
                            if (showDamage) floats.add(FloatText(to.x, to.y - 30 * density, formatNum(e.dmg.toLong()), if (e.crit) Color(0xFFFFB300) else Color.White, now + 160, e.crit))
                            if (e.crit) shake.value = 6f
                            if (e.killed) { t.deadAt = now + 160; t.mood = Mood.KO }
                        }
                        is BattleEvent.SkillCast -> {
                            val from = unitPos(e.from)
                            val a = anims.getOrPut(e.from) { UnitAnim() }
                            a.lungeUntil = now + 260; a.lungeDir = if (e.from < 100) 1f else -1f; a.mood = Mood.ATTACK
                            val col = when (e.effect) { "heal" -> Color(0xFF66BB6A); "shield" -> Color(0xFF42A5F5); else -> Color(unitByIndex[e.from]?.visual?.element?.color ?: 0xFF7C4DFF) }
                            e.targets.forEachIndexed { i, ti ->
                                val to = unitPos(ti)
                                val t = anims.getOrPut(ti) { UnitAnim() }
                                if (e.effect == "damage") {
                                    t.flashUntil = now + 200; t.mood = Mood.HURT
                                    bursts.add(Burst(to.x, to.y, col, now + 120, ti + i))
                                    if (showDamage && i < e.amounts.size) floats.add(FloatText(to.x, to.y - 30 * density, formatNum(e.amounts[i].toLong()), if (e.crits.getOrElse(i) { false }) Color(0xFFFFB300) else Color.White, now + 120, e.crits.getOrElse(i) { false }))
                                    if (e.killed.getOrElse(i) { false }) { t.deadAt = now + 120; t.mood = Mood.KO }
                                } else {
                                    t.mood = Mood.HAPPY
                                    bursts.add(Burst(to.x, to.y, col, now + 100, ti + i))
                                    val prefix = if (e.effect == "heal") "+" else ""
                                    if (i < e.amounts.size) floats.add(FloatText(to.x, to.y - 30 * density, prefix + formatNum(e.amounts[i].toLong()), col, now + 100, false))
                                }
                            }
                            if (e.effect == "damage") shake.value = 8f
                        }
                        is BattleEvent.Heal -> {
                            val to = unitPos(e.to)
                            if (showDamage) floats.add(FloatText(to.x, to.y - 26 * density, "+" + formatNum(e.amount.toLong()), Color(0xFF66BB6A), now, false))
                        }
                        is BattleEvent.Shield -> {
                            val to = unitPos(e.to)
                            bursts.add(Burst(to.x, to.y, Color(0xFF42A5F5), now, e.to))
                        }
                        is BattleEvent.Dot -> {
                            val to = unitPos(e.unit)
                            if (showDamage) floats.add(FloatText(to.x, to.y - 20 * density, formatNum(e.dmg.toLong()), Color(0xFFAB47BC), now, false))
                        }
                        is BattleEvent.Death -> {
                            val t = anims.getOrPut(e.unit) { UnitAnim() }
                            t.deadAt = now; t.mood = Mood.KO
                            val p = unitPos(e.unit)
                            bursts.add(Burst(p.x, p.y, Color(0xFFB0BEC5), now, e.unit + 99))
                        }
                        is BattleEvent.StatusApplied -> {}
                        is BattleEvent.End -> {}
                    }
                }
            }
        }

        // prune expired vfx
        LaunchedEffect(now) {
            floats.removeAll { now - it.birth > 950 }
            bursts.removeAll { now - it.birth > 450 }
            projectiles.removeAll { now - it.birth > 200 }
            if (shake.value > 0) shake.value = (shake.value - 0.6f).coerceAtLeast(0f)
            anims.forEach { (idx, a) ->
                if (a.deadAt == 0L) {
                    if (now > a.lungeUntil && a.mood == Mood.ATTACK) a.mood = Mood.IDLE
                    if (now > a.flashUntil && a.mood == Mood.HURT) a.mood = Mood.IDLE
                    if (a.mood == Mood.HAPPY && now > a.flashUntil + 400) a.mood = Mood.IDLE
                }
            }
        }

        val shakeX = if (shake.value > 0) (Random(now).nextFloat() - 0.5f) * shake.value * density else 0f
        val shakeY = if (shake.value > 0) (Random(now + 1).nextFloat() - 0.5f) * shake.value * density else 0f

        // animated living background (drifting clouds, parallax hills, themed particles)
        LivingBackground(
            Modifier.fillMaxSize(),
            top = Color(zone.bgTop), bottom = Color(zone.bgBottom), accent = Color(zone.accent),
            particle = zone.elements.first().particle, intensity = 1f,
        )
        Canvas(Modifier.fillMaxSize()) {
            drawOval(Color(zone.accent).copy(alpha = 0.18f), topLeft = Offset(-size.width * 0.1f, size.height * 0.80f), size = androidx.compose.ui.geometry.Size(size.width * 1.2f, size.height * 0.3f))
        }

        // units
        ab.battle.units.forEach { u ->
            val pos = unitPos(u.index)
            val a = anims[u.index] ?: UnitAnim()
            val dead = a.deadAt > 0 && now >= a.deadAt
            val alpha = if (dead) (1f - (now - a.deadAt) / 600f).coerceIn(0f, 1f) else 1f
            if (alpha <= 0f) return@forEach
            val lunge = if (now < a.lungeUntil) sin((1f - (a.lungeUntil - now) / 220f) * Math.PI.toFloat()) * 18f * density * a.lungeDir else 0f
            val flash = if (now < a.flashUntil) 0.6f else 0f
            val unitSize = (if (u.visual.rarity.order >= 4 || u.visual.tier >= 2) 84 else 72).dp
            Box(
                modifier = Modifier
                    .offset { androidx.compose.ui.unit.IntOffset((pos.x - unitSize.value * density / 2 + lunge + shakeX).toInt(), (pos.y - unitSize.value * density / 2 + shakeY).toInt()) }
                    .size(unitSize),
                contentAlignment = Alignment.Center,
            ) {
                SlimeView(u.visual, Modifier.fillMaxSize(), mood = a.mood, radiusFactor = 0.34f)
                if (flash > 0) {
                    Box(Modifier.fillMaxSize(0.7f).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = flash * 0.5f)))
                }
            }
            // hp bar + name below
            Column(
                modifier = Modifier
                    .offset { androidx.compose.ui.unit.IntOffset((pos.x - 36 * density + shakeX).toInt(), (pos.y + unitSize.value * density * 0.42f + shakeY).toInt()) }
                    .width(72.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.Black.copy(alpha = 0.25f))) {
                    val frac = (u.hp / u.maxHp).toFloat().coerceIn(0f, 1f)
                    Box(Modifier.fillMaxHeight().fillMaxWidth(frac).clip(RoundedCornerShape(3.dp)).background(if (u.isPlayer) Color(0xFF66BB6A) else Color(0xFFEF5350)))
                }
                if (u.shield > 0) {
                    Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF42A5F5).copy(alpha = 0.8f)))
                }
                Text(u.name, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E2A33).copy(alpha = 0.85f), maxLines = 1, lineHeight = 13.sp)
            }
        }

        // vfx overlay
        Canvas(Modifier.fillMaxSize()) {
            projectiles.forEach { p ->
                val t = ((now - p.birth) / 180f).coerceIn(0f, 1f)
                if (t < 1f) {
                    val x = p.x0 + (p.x1 - p.x0) * t
                    val y = p.y0 + (p.y1 - p.y0) * t - sin(t * Math.PI.toFloat()) * 30 * density
                    drawCircle(p.color.copy(alpha = 0.9f), radius = 7 * density, center = Offset(x, y))
                    drawCircle(Color.White.copy(alpha = 0.7f), radius = 3 * density, center = Offset(x, y))
                }
            }
            bursts.forEach { b ->
                val t = ((now - b.birth) / 420f).coerceIn(0f, 1f)
                if (now >= b.birth && t < 1f) {
                    val rnd = Random(b.seed)
                    repeat(10) { i ->
                        val ang = rnd.nextFloat() * 6.28f
                        val dist = t * 34 * density
                        val x = b.x + kotlin.math.cos(ang) * dist
                        val y = b.y + kotlin.math.sin(ang) * dist
                        drawCircle(b.color.copy(alpha = (1 - t) * 0.9f), radius = (1 - t) * 5 * density + 1, center = Offset(x, y))
                    }
                    drawCircle(b.color.copy(alpha = (1 - t) * 0.4f), radius = t * 26 * density, center = Offset(b.x, b.y), style = Stroke(2 * density))
                }
            }
        }

        // floating damage / heal numbers as positioned text
        floats.forEach { f ->
            val t = ((now - f.birth) / 950f).coerceIn(0f, 1f)
            if (now >= f.birth && t < 1f) {
                val alpha = (1 - t * t).coerceIn(0f, 1f)
                val yOff = f.y - t * 40 * density
                Text(
                    f.text,
                    modifier = Modifier.offset { androidx.compose.ui.unit.IntOffset(f.x.toInt() - 40, yOff.toInt() - 20) }.width(80.dp),
                    color = f.color.copy(alpha = alpha),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (f.big) 22.sp else 16.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge.copy(
                        shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 1f), 3f),
                    ),
                )
            }
        }

        // top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp).statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onExit, modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.7f))) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Exit", tint = Color(0xFF2E2A33))
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    when (ab.mode) {
                        BattleMode.STAGE -> "${zone.name} · Stage ${ab.stage}"
                        BattleMode.TOWER -> "Tower · Floor ${ab.tier}"
                        BattleMode.DUNGEON -> "${ab.dungeon?.displayName} · Tier ${ab.tier + 1}"
                    },
                    fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF2E2A33),
                )
            }
            if (maxSpeed > 1) {
                SoftButton("${speed}x", { onSpeed(if (speed >= maxSpeed) 1 else speed + 1) }, tint = Color(0xFF5E35D1), icon = Icons.Filled.FastForward)
            }
        }

        // outcome overlay
        if (outcome != null) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier.clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surface).padding(28.dp).widthIn(max = 320.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(if (outcome.win) "Victory!" else "Defeat", style = MaterialTheme.typography.displaySmall, color = if (outcome.win) Color(0xFF43A047) else Color(0xFFE53935))
                    if (outcome.win && outcome.loot != null) {
                        val l = outcome.loot
                        Text("Rewards", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (l.gold > 0) Text("+${formatNum(l.gold)} gold", color = Color(0xFFFFB300), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (l.essence > 0) Text("+${formatNum(l.essence)} essence", color = Color(0xFF26C6DA), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (l.mutagen > 0) Text("+${l.mutagen} mutagen", color = Color(0xFFAB47BC), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (l.research > 0) Text("+${l.research} RP", color = Color(0xFF42A5F5), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        if (l.egg != null) Text("An egg was found!", color = Color(0xFFEC407A), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("+${formatNum(l.xp)} XP to squad", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    } else if (!outcome.win) {
                        Text("Your squad was overwhelmed. Level up, fuse stronger slimes, or adjust your formation.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                    PrimaryButton(if (outcome.win) "Continue" else "Retreat", onContinue, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}