package com.f7developer.slimeverse.ui.render

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.f7developer.slimeverse.game.Element
import com.f7developer.slimeverse.game.Family
import com.f7developer.slimeverse.game.UnitVisual
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class Mood { IDLE, HAPPY, HURT, ATTACK, SLEEP, KO }

data class SlimeAnim(
    val bounce: Float = 0f,
    val blink: Float = 1f,
    val squash: Float = 0f,
    val mood: Mood = Mood.IDLE,
    val flash: Float = 0f,
    val alpha: Float = 1f,
    val lunge: Float = 0f,
    val hue: Float = 0f,
    val time: Float = 0f,
)

private fun c(argb: Long) = Color(argb)

private fun lighten(col: Color, f: Float): Color = Color(
    red = (col.red + (1 - col.red) * f).coerceIn(0f, 1f),
    green = (col.green + (1 - col.green) * f).coerceIn(0f, 1f),
    blue = (col.blue + (1 - col.blue) * f).coerceIn(0f, 1f),
    alpha = col.alpha,
)

private fun darken(col: Color, f: Float): Color = Color(
    red = col.red * (1 - f), green = col.green * (1 - f), blue = col.blue * (1 - f), alpha = col.alpha,
)

/** Family-specific radius modulation so each family has a genuinely different silhouette. */
private fun bodyRadius(family: Family, theta: Float, seed: Int): Float {
    val p1 = (seed % 7) * 0.9f
    val p2 = (seed % 5) * 1.3f
    val base = 1f + 0.035f * sin(3 * theta + p1) + 0.02f * sin(5 * theta + p2)
    return when (family) {
        Family.BLOB -> base
        Family.FANG -> base + 0.05f * sin(2 * theta) + if (sin(theta) < -0.6f) 0.06f else 0f
        Family.SHARD -> 1f + 0.10f * kotlin.math.abs(sin(4 * theta + p1)) + 0.04f * sin(7 * theta)
        Family.WING -> base + 0.03f * sin(6 * theta)
        Family.ROYAL -> base + 0.02f * sin(8 * theta)
        Family.MECH -> {
            // superellipse-ish: flatten sides
            val k = 0.10f * (cos(4 * theta))
            1f + k
        }
        Family.WISP -> base + if (sin(theta) > 0.5f) 0.18f * sin(theta) else 0f - if (sin(theta) < -0.7f) 0.10f else 0f
        Family.RUNE -> 1f + 0.06f * cos(6 * theta + p1)
        Family.SHROOM -> base + if (sin(theta) > 0.3f) 0.10f else 0f
        Family.FIN -> base + 0.06f * sin(theta + PI.toFloat() / 2)
        Family.MANTIS -> base + 0.08f * sin(theta) - 0.04f * cos(2 * theta)
        Family.GOLEM -> 1f + 0.07f * kotlin.math.abs(cos(3 * theta + p1))
        Family.SERAPH -> base + 0.025f * sin(9 * theta)
        Family.CHIMERA -> base + 0.07f * kotlin.math.abs(sin(3 * theta + p1))
        Family.TITAN -> 1f + 0.09f * kotlin.math.abs(cos(4 * theta))
        Family.PHANTOM -> base + if (sin(theta) < -0.3f) 0.12f * sin(6 * theta) else 0f
    }
}

private fun bodyPath(family: Family, r: Float, seed: Int, squash: Float): Path {
    val path = Path()
    val steps = 64
    val sx = 1f + squash
    val sy = 1f - squash
    for (i in 0..steps) {
        val theta = (i.toFloat() / steps) * 2f * PI.toFloat()
        val rad = r * bodyRadius(family, theta, seed)
        // theta 0 = right, PI/2 = down (screen). Sit flat at bottom.
        var x = cos(theta) * rad * sx
        var y = sin(theta) * rad * sy
        if (y > r * 0.55f) y = r * 0.55f + (y - r * 0.55f) * 0.35f // flatten bottom
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

fun DrawScope.drawSlime(
    cx: Float, cy: Float, r: Float, v: UnitVisual, anim: SlimeAnim,
) {
    if (anim.alpha <= 0.01f) return
    val base = c(v.element.color)
    val glow = c(v.element.glow)
    val seed = v.seed
    val sizeScale = when (v.size) { "giant" -> 1.18f; "tiny" -> 0.82f; else -> 1f } * (1f + v.tier * 0.10f)
    val rr = r * sizeScale
    val bounceY = -kotlin.math.abs(sin(anim.bounce * PI.toFloat())) * rr * 0.10f
    val squash = anim.squash + sin(anim.bounce * 2 * PI.toFloat()) * 0.035f

    translate(cx + anim.lunge * rr, cy + bounceY) {
        scale(1f, 1f) {
            // shadow
            drawOval(
                color = Color.Black.copy(alpha = 0.10f * anim.alpha),
                topLeft = Offset(-rr * 0.8f, rr * 0.62f - bounceY),
                size = Size(rr * 1.6f, rr * 0.34f),
            )
            // aura glow
            val auraOn = v.aura != "none" || v.prismatic || v.rarity.order >= 3
            if (auraOn) {
                val auraCol = when {
                    v.prismatic -> Color.hsv((anim.time * 40f) % 360f, 0.55f, 1f)
                    v.aura == "ember" -> c(0xFFFF7043)
                    v.aura == "frost" -> c(0xFF81D4FA)
                    v.aura == "volt" -> c(0xFFFFCA28)
                    v.aura == "bloom" -> c(0xFF66BB6A)
                    v.aura == "umbra" -> c(0xFF7E57C2)
                    v.aura == "lumen" -> c(0xFFFFD54F)
                    else -> glow
                }
                val pulse = 0.18f + 0.08f * sin(anim.time * 3f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(auraCol.copy(alpha = pulse * anim.alpha), auraCol.copy(alpha = 0f)),
                        center = Offset.Zero, radius = rr * 1.7f,
                    ),
                    radius = rr * 1.7f,
                )
            }

            // wings behind (WING / SERAPH)
            if (v.family == Family.WING || v.family == Family.SERAPH) {
                drawWings(rr, glow, anim)
            }
            // phantom tail wisps
            if (v.family == Family.PHANTOM) {
                val tail = Path()
                tail.moveTo(-rr * 0.5f, rr * 0.4f)
                tail.quadraticBezierTo(-rr * 0.2f, rr * 1.1f + sin(anim.time * 4) * rr * 0.1f, 0f, rr * 0.5f)
                tail.quadraticBezierTo(rr * 0.2f, rr * 1.15f + cos(anim.time * 4) * rr * 0.1f, rr * 0.5f, rr * 0.4f)
                drawPath(tail, brush = Brush.verticalGradient(listOf(base.copy(alpha = 0.7f * anim.alpha), base.copy(alpha = 0f))))
            }

            val body = bodyPath(v.family, rr, seed, squash)
            val top = lighten(base, 0.35f)
            val bottom = darken(base, 0.18f)
            val bodyBrush = if (v.prismatic) {
                val h = (anim.time * 40f) % 360f
                Brush.verticalGradient(
                    listOf(Color.hsv(h, 0.45f, 1f), Color.hsv((h + 60) % 360, 0.55f, 0.95f), Color.hsv((h + 120) % 360, 0.6f, 0.85f)),
                )
            } else {
                Brush.verticalGradient(listOf(top, base, bottom), startY = -rr, endY = rr)
            }
            drawPath(body, brush = bodyBrush, alpha = anim.alpha)

            // hide texture + pattern clipped to body
            clipPath(body) {
                drawPattern(v, rr, base, anim)
                drawHide(v, rr, base, anim)
                // glossy highlight
                drawOval(
                    brush = Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.55f * anim.alpha), Color.White.copy(alpha = 0f)),
                        center = Offset(-rr * 0.35f, -rr * 0.45f), radius = rr * 0.55f,
                    ),
                    topLeft = Offset(-rr * 0.75f, -rr * 0.85f),
                    size = Size(rr * 0.9f, rr * 0.7f),
                )
                // hurt flash
                if (anim.flash > 0f) drawPath(body, color = Color.White.copy(alpha = anim.flash * 0.7f))
            }

            // outline
            drawPath(body, color = darken(base, 0.35f).copy(alpha = 0.5f * anim.alpha), style = Stroke(width = rr * 0.04f))

            // family extras in front
            drawFamilyFront(v, rr, base, glow, anim)
            // crown
            drawCrown(v, rr, anim)
            // face
            drawFace(v, rr, anim)
        }
    }
}

private fun DrawScope.drawWings(rr: Float, glow: Color, anim: SlimeAnim) {
    val flap = sin(anim.time * 6f) * 0.25f
    for (side in listOf(-1f, 1f)) {
        rotate(side * (20f + flap * 30f), pivot = Offset(side * rr * 0.6f, -rr * 0.1f)) {
            val w = Path()
            w.moveTo(side * rr * 0.5f, -rr * 0.1f)
            w.quadraticBezierTo(side * rr * 1.5f, -rr * 0.9f, side * rr * 1.35f, -rr * 0.1f)
            w.quadraticBezierTo(side * rr * 1.2f, rr * 0.25f, side * rr * 0.5f, rr * 0.15f)
            w.close()
            drawPath(w, brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.85f * anim.alpha), glow.copy(alpha = 0.5f * anim.alpha))))
        }
    }
}

private fun DrawScope.drawPattern(v: UnitVisual, rr: Float, base: Color, anim: SlimeAnim) {
    val dark = darken(base, 0.25f).copy(alpha = 0.5f * anim.alpha)
    when (v.pattern) {
        "spots" -> {
            val rnd = kotlin.random.Random(v.seed)
            repeat(6) {
                val x = (rnd.nextFloat() - 0.5f) * rr * 1.4f
                val y = (rnd.nextFloat() - 0.3f) * rr * 1.2f
                drawCircle(dark, radius = rr * (0.08f + rnd.nextFloat() * 0.08f), center = Offset(x, y))
            }
        }
        "stripes" -> {
            for (i in -2..2) {
                drawLine(
                    dark, start = Offset(-rr + i * rr * 0.4f, -rr), end = Offset(i * rr * 0.4f, rr),
                    strokeWidth = rr * 0.12f, cap = StrokeCap.Round,
                )
            }
        }
        "galaxy" -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF1A1A3E).copy(alpha = 0.55f * anim.alpha), Color(0xFF3A2A6E).copy(alpha = 0.35f * anim.alpha))), topLeft = Offset(-rr, -rr), size = Size(rr * 2, rr * 2))
            val rnd = kotlin.random.Random(v.seed + 1)
            repeat(14) {
                val x = (rnd.nextFloat() - 0.5f) * rr * 1.8f
                val y = (rnd.nextFloat() - 0.5f) * rr * 1.6f
                drawCircle(Color.White.copy(alpha = (0.4f + 0.5f * sin(anim.time * 2 + it)) * anim.alpha), radius = rr * 0.025f, center = Offset(x, y))
            }
        }
        "rune" -> {
            val glow = c(0xFFFFE082).copy(alpha = (0.5f + 0.3f * sin(anim.time * 3)) * anim.alpha)
            val p = Path()
            p.moveTo(-rr * 0.4f, rr * 0.1f); p.lineTo(-rr * 0.1f, -rr * 0.2f); p.lineTo(rr * 0.2f, rr * 0.15f); p.lineTo(rr * 0.45f, -rr * 0.1f)
            drawPath(p, color = glow, style = Stroke(width = rr * 0.05f, cap = StrokeCap.Round))
        }
        else -> {}
    }
}

private fun DrawScope.drawHide(v: UnitVisual, rr: Float, base: Color, anim: SlimeAnim) {
    when (v.hide) {
        "spiky" -> {
            val col = darken(base, 0.3f).copy(alpha = anim.alpha)
            for (i in 0 until 10) {
                val theta = PI.toFloat() + (i.toFloat() / 9) * PI.toFloat()
                val x = cos(theta) * rr * 0.95f
                val y = sin(theta) * rr * 0.95f
                val p = Path()
                p.moveTo(x - rr * 0.08f, y)
                p.lineTo(x + cos(theta) * rr * 0.22f, y + sin(theta) * rr * 0.22f)
                p.lineTo(x + rr * 0.08f, y)
                p.close()
                drawPath(p, col)
            }
        }
        "crystal" -> {
            val col = lighten(base, 0.5f).copy(alpha = 0.6f * anim.alpha)
            drawLine(col, Offset(-rr * 0.5f, -rr * 0.2f), Offset(rr * 0.1f, rr * 0.4f), rr * 0.03f)
            drawLine(col, Offset(rr * 0.2f, -rr * 0.5f), Offset(rr * 0.5f, rr * 0.1f), rr * 0.03f)
            // gem shards on top
            for (i in -1..1) {
                val p = Path()
                val bx = i * rr * 0.35f
                p.moveTo(bx - rr * 0.1f, -rr * 0.7f)
                p.lineTo(bx, -rr * 1.05f)
                p.lineTo(bx + rr * 0.1f, -rr * 0.7f)
                p.close()
                drawPath(p, lighten(base, 0.4f).copy(alpha = anim.alpha))
            }
        }
        "fuzzy" -> {
            val col = lighten(base, 0.3f).copy(alpha = 0.7f * anim.alpha)
            val rnd = kotlin.random.Random(v.seed + 2)
            repeat(26) {
                val theta = rnd.nextFloat() * 2 * PI.toFloat()
                val x = cos(theta) * rr * 0.98f
                val y = sin(theta) * rr * 0.98f
                drawLine(col, Offset(x, y), Offset(x * 1.12f, y * 1.12f), rr * 0.03f, cap = StrokeCap.Round)
            }
        }
        else -> {}
    }
}

private fun DrawScope.drawFamilyFront(v: UnitVisual, rr: Float, base: Color, glow: Color, anim: SlimeAnim) {
    val dark = darken(base, 0.3f).copy(alpha = anim.alpha)
    val light = lighten(base, 0.4f).copy(alpha = anim.alpha)
    when (v.family) {
        Family.FANG -> {
            // ears
            for (side in listOf(-1f, 1f)) {
                val p = Path()
                p.moveTo(side * rr * 0.45f, -rr * 0.6f)
                p.lineTo(side * rr * 0.75f, -rr * 1.05f)
                p.lineTo(side * rr * 0.8f, -rr * 0.45f)
                p.close()
                drawPath(p, base.copy(alpha = anim.alpha))
            }
        }
        Family.MECH -> {
            drawRoundRect(dark, topLeft = Offset(-rr * 0.7f, -rr * 0.1f), size = Size(rr * 1.4f, rr * 0.5f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(rr * 0.1f), style = Stroke(rr * 0.04f))
            drawCircle(light, rr * 0.08f, Offset(-rr * 0.55f, rr * 0.15f))
            drawCircle(light, rr * 0.08f, Offset(rr * 0.55f, rr * 0.15f))
        }
        Family.SHROOM -> {
            // mushroom cap
            val cap = Path()
            cap.moveTo(-rr * 0.95f, -rr * 0.45f)
            cap.quadraticBezierTo(0f, -rr * 1.35f, rr * 0.95f, -rr * 0.45f)
            cap.quadraticBezierTo(0f, -rr * 0.25f, -rr * 0.95f, -rr * 0.45f)
            cap.close()
            drawPath(cap, darken(base, 0.1f).copy(alpha = anim.alpha))
            drawCircle(Color.White.copy(alpha = 0.7f * anim.alpha), rr * 0.12f, Offset(-rr * 0.35f, -rr * 0.7f))
            drawCircle(Color.White.copy(alpha = 0.7f * anim.alpha), rr * 0.09f, Offset(rr * 0.3f, -rr * 0.75f))
        }
        Family.FIN -> {
            // side fins + tail
            for (side in listOf(-1f, 1f)) {
                val p = Path()
                p.moveTo(side * rr * 0.7f, 0f)
                p.quadraticBezierTo(side * rr * 1.25f, -rr * 0.2f, side * rr * 1.05f, rr * 0.3f)
                p.quadraticBezierTo(side * rr * 0.85f, rr * 0.2f, side * rr * 0.7f, rr * 0.15f)
                p.close()
                drawPath(p, light)
            }
        }
        Family.MANTIS -> {
            for (side in listOf(-1f, 1f)) {
                val p = Path()
                p.moveTo(side * rr * 0.6f, -rr * 0.1f)
                p.lineTo(side * rr * 1.2f, -rr * 0.5f + sin(anim.time * 5) * rr * 0.05f)
                p.lineTo(side * rr * 0.75f, rr * 0.2f)
                p.close()
                drawPath(p, dark)
            }
        }
        Family.GOLEM, Family.TITAN -> {
            drawRoundRect(dark, topLeft = Offset(-rr * 0.5f, rr * 0.05f), size = Size(rr, rr * 0.4f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(rr * 0.08f), style = Stroke(rr * 0.05f))
            drawCircle(light, rr * 0.1f, Offset(-rr * 0.6f, -rr * 0.5f))
            drawCircle(light, rr * 0.08f, Offset(rr * 0.55f, -rr * 0.55f))
        }
        Family.CHIMERA -> {
            // two extra head bumps
            drawCircle(base.copy(alpha = anim.alpha), rr * 0.28f, Offset(-rr * 0.6f, -rr * 0.55f))
            drawCircle(base.copy(alpha = anim.alpha), rr * 0.28f, Offset(rr * 0.6f, -rr * 0.55f))
            drawCircle(Color(0xFF2E2A33).copy(alpha = anim.alpha), rr * 0.05f, Offset(-rr * 0.6f, -rr * 0.58f))
            drawCircle(Color(0xFF2E2A33).copy(alpha = anim.alpha), rr * 0.05f, Offset(rr * 0.6f, -rr * 0.58f))
        }
        Family.WISP -> {
            // flame tip
            val p = Path()
            p.moveTo(-rr * 0.2f, -rr * 0.7f)
            p.quadraticBezierTo(0f, -rr * 1.4f - sin(anim.time * 6) * rr * 0.1f, rr * 0.2f, -rr * 0.7f)
            p.close()
            drawPath(p, glow.copy(alpha = 0.8f * anim.alpha))
        }
        Family.RUNE -> {
            drawCircle(glow.copy(alpha = (0.4f + 0.3f * sin(anim.time * 2)) * anim.alpha), rr * 0.12f, Offset(0f, rr * 0.35f))
        }
        else -> {}
    }
}

private fun DrawScope.drawCrown(v: UnitVisual, rr: Float, anim: SlimeAnim) {
    val gold = c(0xFFFFCA28).copy(alpha = anim.alpha)
    val dark = Color(0xFF5D4037).copy(alpha = anim.alpha)
    val green = c(0xFF66BB6A).copy(alpha = anim.alpha)
    when (v.crown) {
        "horns" -> for (side in listOf(-1f, 1f)) {
            val p = Path()
            p.moveTo(side * rr * 0.35f, -rr * 0.75f)
            p.quadraticBezierTo(side * rr * 0.65f, -rr * 1.2f, side * rr * 0.3f, -rr * 1.25f)
            p.quadraticBezierTo(side * rr * 0.4f, -rr * 0.95f, side * rr * 0.15f, -rr * 0.8f)
            p.close()
            drawPath(p, c(0xFFFFE0B2).copy(alpha = anim.alpha))
        }
        "halo" -> {
            drawOval(gold, topLeft = Offset(-rr * 0.4f, -rr * 1.35f + sin(anim.time * 2) * rr * 0.04f), size = Size(rr * 0.8f, rr * 0.22f), style = Stroke(rr * 0.07f))
        }
        "antennae" -> for (side in listOf(-1f, 1f)) {
            drawLine(dark, Offset(side * rr * 0.2f, -rr * 0.75f), Offset(side * rr * 0.4f, -rr * 1.15f), rr * 0.04f, cap = StrokeCap.Round)
            drawCircle(gold, rr * 0.09f, Offset(side * rr * 0.4f, -rr * 1.15f))
        }
        "cap" -> {
            val p = Path()
            p.moveTo(-rr * 0.45f, -rr * 0.7f)
            p.lineTo(-rr * 0.3f, -rr * 1.1f); p.lineTo(-rr * 0.1f, -rr * 0.8f)
            p.lineTo(0f, -rr * 1.2f); p.lineTo(rr * 0.1f, -rr * 0.8f)
            p.lineTo(rr * 0.3f, -rr * 1.1f); p.lineTo(rr * 0.45f, -rr * 0.7f)
            p.close()
            drawPath(p, gold)
            drawCircle(c(0xFFEC407A).copy(alpha = anim.alpha), rr * 0.07f, Offset(0f, -rr * 0.95f))
        }
        "leaves" -> {
            drawLine(green, Offset(0f, -rr * 0.75f), Offset(0f, -rr * 1.05f), rr * 0.05f, cap = StrokeCap.Round)
            for (side in listOf(-1f, 1f)) {
                val p = Path()
                p.moveTo(0f, -rr * 0.95f)
                p.quadraticBezierTo(side * rr * 0.4f, -rr * 1.25f, side * rr * 0.35f, -rr * 0.9f)
                p.quadraticBezierTo(side * rr * 0.15f, -rr * 0.85f, 0f, -rr * 0.95f)
                p.close()
                drawPath(p, green)
            }
        }
        else -> {}
    }
}

private fun DrawScope.drawFace(v: UnitVisual, rr: Float, anim: SlimeAnim) {
    val ink = Color(0xFF2E2A33).copy(alpha = anim.alpha)
    val white = Color.White.copy(alpha = anim.alpha)
    val eyeY = -rr * 0.12f
    val eyeDX = rr * 0.30f
    val eyeR = rr * 0.16f
    val blink = if (anim.mood == Mood.SLEEP || anim.mood == Mood.KO) 0.1f else anim.blink
    val hurt = anim.mood == Mood.HURT
    val attack = anim.mood == Mood.ATTACK

    fun eye(x: Float, big: Float = 1f) {
        val h = eyeR * 1.25f * blink * big
        if (h < eyeR * 0.15f) {
            drawLine(ink, Offset(x - eyeR * big, eyeY), Offset(x + eyeR * big, eyeY), rr * 0.04f, cap = StrokeCap.Round)
            return
        }
        when (v.eyes) {
            "visor" -> {
                drawRoundRect(c(0xFF26C6DA).copy(alpha = 0.85f * anim.alpha), topLeft = Offset(x - eyeR * big, eyeY - h * 0.5f), size = Size(eyeR * 2 * big, h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.5f))
            }
            "sleepy" -> {
                drawOval(white, topLeft = Offset(x - eyeR * big, eyeY - h * 0.4f), size = Size(eyeR * 2 * big, h * 0.8f))
                drawOval(ink, topLeft = Offset(x - eyeR * 0.5f * big, eyeY - h * 0.1f), size = Size(eyeR * big, h * 0.5f))
                drawLine(ink, Offset(x - eyeR * big, eyeY - h * 0.4f), Offset(x + eyeR * big, eyeY - h * 0.4f), rr * 0.035f, cap = StrokeCap.Round)
            }
            else -> {
                drawOval(white, topLeft = Offset(x - eyeR * big, eyeY - h), size = Size(eyeR * 2 * big, h * 2))
                val pr = eyeR * 0.55f * big
                val px = x + if (attack) rr * 0.03f else 0f
                if (v.eyes == "star") {
                    drawStar(ink, px, eyeY, pr)
                } else {
                    drawCircle(ink, pr, Offset(px, eyeY + h * 0.1f))
                    drawCircle(white, pr * 0.35f, Offset(px - pr * 0.3f, eyeY - pr * 0.2f))
                }
            }
        }
    }

    if (v.eyes == "cyclops") {
        eye(0f, 1.7f)
    } else if (v.eyes == "visor") {
        drawRoundRect(c(0xFF263238).copy(alpha = anim.alpha), topLeft = Offset(-eyeDX - eyeR, eyeY - eyeR * 0.7f), size = Size(eyeDX * 2 + eyeR * 2, eyeR * 1.4f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(eyeR * 0.7f))
        eye(-eyeDX, 0.8f); eye(eyeDX, 0.8f)
    } else {
        eye(-eyeDX); eye(eyeDX)
    }

    // brows for fierce
    if (v.temper == "fierce" && !hurt) {
        drawLine(ink, Offset(-eyeDX - eyeR * 0.8f, eyeY - eyeR * 1.5f), Offset(-eyeDX + eyeR * 0.6f, eyeY - eyeR * 1.0f), rr * 0.045f, cap = StrokeCap.Round)
        drawLine(ink, Offset(eyeDX + eyeR * 0.8f, eyeY - eyeR * 1.5f), Offset(eyeDX - eyeR * 0.6f, eyeY - eyeR * 1.0f), rr * 0.045f, cap = StrokeCap.Round)
    }

    // mouth
    val my = rr * 0.28f
    when {
        anim.mood == Mood.KO -> {
            drawLine(ink, Offset(-rr * 0.12f, my), Offset(rr * 0.12f, my + rr * 0.05f), rr * 0.04f, cap = StrokeCap.Round)
        }
        hurt -> {
            drawOval(ink, topLeft = Offset(-rr * 0.1f, my - rr * 0.05f), size = Size(rr * 0.2f, rr * 0.16f))
        }
        attack || v.temper == "fierce" -> {
            val p = Path()
            p.moveTo(-rr * 0.18f, my)
            p.quadraticBezierTo(0f, my + rr * 0.22f, rr * 0.18f, my)
            p.close()
            drawPath(p, ink)
            // fang
            val f = Path()
            f.moveTo(-rr * 0.1f, my); f.lineTo(-rr * 0.05f, my + rr * 0.1f); f.lineTo(0f, my); f.close()
            drawPath(f, white)
        }
        v.temper == "playful" || anim.mood == Mood.HAPPY -> {
            val p = Path()
            p.moveTo(-rr * 0.16f, my)
            p.quadraticBezierTo(0f, my + rr * 0.2f, rr * 0.16f, my)
            p.close()
            drawPath(p, ink)
            drawOval(c(0xFFEC407A).copy(alpha = 0.7f * anim.alpha), topLeft = Offset(-rr * 0.07f, my + rr * 0.06f), size = Size(rr * 0.14f, rr * 0.08f))
        }
        else -> {
            val p = Path()
            p.moveTo(-rr * 0.14f, my)
            p.quadraticBezierTo(0f, my + rr * 0.12f, rr * 0.14f, my)
            drawPath(p, ink, style = Stroke(rr * 0.04f, cap = StrokeCap.Round))
        }
    }
    // blush
    if (anim.mood == Mood.HAPPY || v.temper == "playful") {
        drawCircle(c(0xFFFF8A80).copy(alpha = 0.4f * anim.alpha), rr * 0.09f, Offset(-eyeDX - rr * 0.12f, rr * 0.12f))
        drawCircle(c(0xFFFF8A80).copy(alpha = 0.4f * anim.alpha), rr * 0.09f, Offset(eyeDX + rr * 0.12f, rr * 0.12f))
    }
}

private fun DrawScope.drawStar(col: Color, cx: Float, cy: Float, r: Float) {
    val p = Path()
    for (i in 0 until 10) {
        val ang = -PI.toFloat() / 2 + i * PI.toFloat() / 5
        val rad = if (i % 2 == 0) r else r * 0.45f
        val x = cx + cos(ang) * rad
        val y = cy + sin(ang) * rad
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    drawPath(p, col)
}

/** Animated standalone slime portrait used across the UI. */
@Composable
fun SlimeView(
    visual: UnitVisual,
    modifier: Modifier = Modifier,
    mood: Mood = Mood.IDLE,
    animate: Boolean = true,
    radiusFactor: Float = 0.36f,
) {
    val transition = rememberInfiniteTransition(label = "slime")
    val bounce by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart), label = "bounce",
    )
    val time by transition.animateFloat(
        initialValue = 0f, targetValue = 3600f,
        animationSpec = infiniteRepeatable(tween(3600000, easing = LinearEasing), RepeatMode.Restart), label = "time",
    )
    val blinkCycle by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart), label = "blink",
    )
    val blink = when {
        blinkCycle > 0.96f -> 0.1f
        blinkCycle > 0.93f -> 0.5f
        else -> 1f
    }
    Canvas(modifier = modifier) {
        val r = size.minDimension * radiusFactor
        val anim = if (animate) SlimeAnim(bounce = bounce, blink = blink, mood = mood, time = time)
        else SlimeAnim(blink = 1f, mood = mood, time = 10f)
        drawSlime(size.width / 2f, size.height / 2f + r * 0.1f, r, visual, anim)
    }
}