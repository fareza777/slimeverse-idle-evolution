package com.f7developer.slimeverse.ui.render

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.f7developer.slimeverse.game.ParticleStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** A living, animated background: gradient sky, glowing orb, drifting clouds,
 *  parallax hills and floating themed particles. Cheap enough for mid-range phones. */
@Composable
fun LivingBackground(
    modifier: Modifier = Modifier,
    top: Color,
    bottom: Color,
    accent: Color,
    particle: ParticleStyle = ParticleStyle.STAR,
    dark: Boolean = false,
    intensity: Float = 1f,
) {
    val t = rememberInfiniteTransition(label = "bg")
    val time by t.animateFloat(0f, 1000f, infiniteRepeatable(tween(1000000, easing = LinearEasing), RepeatMode.Restart), label = "t")
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        drawRect(Brush.verticalGradient(listOf(top, bottom)))
        // glowing sun / moon
        val orbX = w * 0.78f; val orbY = h * 0.16f
        val pulse = 0.9f + 0.1f * sin(time * 0.6f)
        drawCircle(
            brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.5f * intensity), accent.copy(alpha = 0.0f)), center = Offset(orbX, orbY), radius = h * 0.30f * pulse),
            radius = h * 0.30f * pulse, center = Offset(orbX, orbY),
        )
        drawCircle(Color.White.copy(alpha = (if (dark) 0.85f else 0.9f) * intensity), radius = h * 0.06f, center = Offset(orbX, orbY))
        // drifting clouds (size capped so fullscreen battles stay subtle)
        val cloudCol = Color.White.copy(alpha = (if (dark) 0.10f else 0.40f) * intensity)
        val cloudBase = (h * 0.035f).coerceAtMost(46f * density)
        for (i in 0 until 3) {
            val speed = 6f + i * 4f
            val cx = ((time * speed + i * w * 0.4f) % (w + 240f)) - 120f
            val cy = h * (0.10f + i * 0.07f)
            drawCloud(cx, cy, cloudBase * (1f + i * 0.15f), cloudCol)
        }
        // parallax hills
        drawHill(w, h, 0.78f, accent.copy(alpha = 0.20f * intensity), time * 2f)
        drawHill(w, h, 0.86f, accent.copy(alpha = 0.30f * intensity), time * 4f + 50f)
        // floating particles
        drawParticles(w, h, time, particle, accent, dark, intensity)
    }
}

private fun DrawScope.drawCloud(cx: Float, cy: Float, r: Float, col: Color) {
    drawCircle(col, r, Offset(cx, cy))
    drawCircle(col, r * 0.8f, Offset(cx - r * 0.9f, cy + r * 0.2f))
    drawCircle(col, r * 0.75f, Offset(cx + r * 0.9f, cy + r * 0.25f))
    drawCircle(col, r * 0.6f, Offset(cx + r * 0.2f, cy - r * 0.5f))
}

private fun DrawScope.drawHill(w: Float, h: Float, base: Float, col: Color, phase: Float) {
    val p = Path()
    p.moveTo(0f, h)
    p.lineTo(0f, h * base)
    val steps = 8
    for (i in 0..steps) {
        val x = w * i / steps
        val y = h * base + sin(i * 1.3f + phase * 0.02f) * h * 0.03f
        p.lineTo(x, y)
    }
    p.lineTo(w, h); p.close()
    drawPath(p, col)
}

private fun DrawScope.drawParticles(w: Float, h: Float, time: Float, style: ParticleStyle, accent: Color, dark: Boolean, intensity: Float) {
    val rnd = Random(7)
    val n = (18 * intensity).toInt()
    for (i in 0 until n) {
        val seedX = rnd.nextFloat(); val seedY = rnd.nextFloat(); val sp = 8f + rnd.nextFloat() * 20f
        val drift = sin(time * 0.5f + i) * 20f
        val x = (seedX * w + drift + w) % w
        val y = (h - ((time * sp + seedY * h) % (h + 40f))) + 20f
        val tw = 0.4f + 0.6f * kotlin.math.abs(sin(time * 2f + i * 1.7f))
        val col = when (style) {
            ParticleStyle.SNOW -> Color.White
            ParticleStyle.EMBER -> Color(0xFFFFAB91)
            ParticleStyle.LEAF -> Color(0xFFA5D6A7)
            ParticleStyle.BUBBLE -> Color(0xFFB3E5FC)
            ParticleStyle.SPARK -> Color(0xFFFFE082)
            ParticleStyle.WISP -> Color(0xFFB39DDB)
            ParticleStyle.SWIRL -> Color(0xFFB2EBF2)
            ParticleStyle.STAR -> if (dark) Color(0xFFFFE082) else accent
        }
        val r = (2f + rnd.nextFloat() * 3f)
        when (style) {
            ParticleStyle.BUBBLE -> drawCircle(col.copy(alpha = 0.5f * tw * intensity), r * 1.6f, Offset(x, y), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))
            ParticleStyle.STAR, ParticleStyle.SPARK -> {
                drawCircle(col.copy(alpha = 0.8f * tw * intensity), r, Offset(x, y))
                drawCircle(Color.White.copy(alpha = 0.5f * tw * intensity), r * 0.4f, Offset(x, y))
            }
            ParticleStyle.LEAF -> drawOval(col.copy(alpha = 0.6f * tw * intensity), topLeft = Offset(x - r, y - r * 0.6f), size = Size(r * 2, r * 1.2f))
            else -> drawCircle(col.copy(alpha = 0.6f * tw * intensity), r, Offset(x, y))
        }
    }
}

/** Animated idle-farm terrarium: squad slimes wander and bounce on a living meadow
 *  while resource orbs float up. The visual heart of the Nest screen. */
@Composable
fun HabitatScene(
    slimes: List<com.f7developer.slimeverse.game.UnitVisual>,
    modifier: Modifier = Modifier,
    producing: Boolean = true,
) {
    val t = rememberInfiniteTransition(label = "hab")
    val time by t.animateFloat(0f, 1000f, infiniteRepeatable(tween(1000000, easing = LinearEasing), RepeatMode.Restart), label = "t")
    Box(modifier = modifier.clip(RoundedCornerShape(24.dp))) {
        LivingBackground(Modifier.fillMaxSize(), top = Color(0xFFBDE8FF), bottom = Color(0xFFEAF7DC), accent = Color(0xFF66BB6A), particle = ParticleStyle.LEAF)
        // ground + grass + flowers + rising resource orbs
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            drawOval(Color(0xFF8BC34A).copy(alpha = 0.5f), topLeft = Offset(-w * 0.1f, h * 0.72f), size = Size(w * 1.2f, h * 0.4f))
            val rnd = Random(3)
            for (i in 0 until 16) {
                val gx = rnd.nextFloat() * w
                val gy = h * (0.78f + rnd.nextFloat() * 0.18f)
                drawLine(Color(0xFF558B2F).copy(alpha = 0.6f), Offset(gx, gy), Offset(gx + 3f, gy - 10f - rnd.nextFloat() * 8f), 2f)
            }
            for (i in 0 until 6) {
                val fx = rnd.nextFloat() * w; val fy = h * (0.8f + rnd.nextFloat() * 0.15f)
                drawCircle(Color(0xFFFFF176), 4f, Offset(fx, fy))
                drawCircle(Color(0xFFFF8A80), 2f, Offset(fx, fy))
            }
            if (producing) {
                for (i in 0 until 8) {
                    val sp = 20f + i * 6f
                    val ox = w * (0.15f + (i * 0.11f))
                    val oy = h - ((time * sp + i * 60f) % (h * 0.9f))
                    val a = (oy / h).coerceIn(0f, 1f)
                    val col = if (i % 2 == 0) Color(0xFFFFB300) else Color(0xFF26C6DA)
                    drawCircle(col.copy(alpha = 0.7f * a), 5f, Offset(ox + sin(time + i) * 8f, oy))
                    drawCircle(Color.White.copy(alpha = 0.5f * a), 2f, Offset(ox + sin(time + i) * 8f - 1f, oy - 1f))
                }
            }
        }
        // wandering slimes
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
            val wpx = constraints.maxWidth.toFloat()
            val hpx = constraints.maxHeight.toFloat()
            val dens = androidx.compose.ui.platform.LocalDensity.current.density
            val count = slimes.size.coerceAtMost(5)
            slimes.take(5).forEachIndexed { i, v ->
                val baseX = if (count <= 1) 0.5f else 0.16f + i * (0.68f / (count - 1))
                val wander = sin(time * (0.4f + i * 0.07f) + i * 2f) * 0.05f
                val xf = (baseX + wander).coerceIn(0.10f, 0.90f)
                val szDp = 66 + (i % 3) * 8
                val szPx = szDp * dens
                val bounce = -kotlin.math.abs(sin(time * 2.2f + i * 1.3f)) * 10f * dens
                val x = xf * wpx - szPx / 2f
                val y = hpx - szPx - 6f * dens + bounce
                SlimeView(
                    v,
                    Modifier.offset { IntOffset(x.toInt(), y.toInt()) }.size(szDp.dp),
                    mood = if (i % 2 == 0) Mood.HAPPY else Mood.IDLE,
                    radiusFactor = 0.34f,
                )
            }
        }
    }
}

/** Slimes marching along a trail as an expedition progresses — the "send slime" animation. */
@Composable
fun ExpeditionMarch(
    visuals: List<com.f7developer.slimeverse.game.UnitVisual>,
    prog: Float,
    modifier: Modifier = Modifier,
) {
    val t = rememberInfiniteTransition(label = "march")
    val time by t.animateFloat(0f, 1000f, infiniteRepeatable(tween(1000000, easing = LinearEasing), RepeatMode.Restart), label = "t")
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier) {
        val wpx = constraints.maxWidth.toFloat()
        val hpx = constraints.maxHeight.toFloat()
        val dens = androidx.compose.ui.platform.LocalDensity.current.density
        Canvas(Modifier.fillMaxSize()) {
            val y = hpx * 0.72f
            var x = 12f * dens
            while (x < wpx - 12f * dens) {
                drawLine(Color(0xFFA1887F).copy(alpha = 0.5f), Offset(x, y), Offset(x + 8f * dens, y), 3f * dens, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                x += 16f * dens
            }
            drawCircle(Color(0xFF8BC34A).copy(alpha = 0.6f), 8f * dens, Offset(10f * dens, y))
            val fx = wpx - 12f * dens
            drawLine(Color(0xFF5D4037), Offset(fx, y), Offset(fx, y - 22f * dens), 3f * dens)
            val flag = Path()
            flag.moveTo(fx, y - 22f * dens); flag.lineTo(fx + 14f * dens, y - 17f * dens); flag.lineTo(fx, y - 12f * dens); flag.close()
            drawPath(flag, Color(0xFFEC407A))
        }
        val lead = (0.06f + prog.coerceIn(0f, 1f) * 0.82f)
        visuals.take(3).forEachIndexed { i, v ->
            val xf = (lead - i * 0.06f).coerceIn(0.04f, 0.92f)
            val bounce = -kotlin.math.abs(sin(time * 3f + i * 1.1f)) * 6f * dens
            val sz = 42
            val x = xf * wpx - sz * dens / 2f
            val y = hpx * 0.72f - sz * dens + 4f * dens + bounce
            SlimeView(v, Modifier.offset { IntOffset(x.toInt(), y.toInt()) }.size(sz.dp), mood = Mood.HAPPY, radiusFactor = 0.34f)
        }
    }
}