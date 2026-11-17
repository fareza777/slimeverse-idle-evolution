package com.f7developer.slimeverse.ui.screens

import android.media.MediaPlayer
import android.view.SurfaceView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.f7developer.slimeverse.R
import com.f7developer.slimeverse.ui.render.Mood
import com.f7developer.slimeverse.ui.render.SlimeView
import com.f7developer.slimeverse.ui.theme.Fredoka
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onDone: () -> Unit) {
    var phase by remember { mutableStateOf(0) }
    var canSkip by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        delay(700); canSkip = true
        delay(1900); phase = 1
    }
    // F7 phase: video plays ~4s; safety timeout advances regardless.
    LaunchedEffect(phase) {
        if (phase == 1) { delay(4600); onDone() }
    }

    val transition = rememberInfiniteTransition(label = "splash")
    val pulse by transition.animateFloat(
        initialValue = 0.92f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse",
    )
    val dot by transition.animateFloat(
        initialValue = 0f, targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart), label = "dot",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (phase == 0) Color(0xFFFFF6EC) else Color(0xFF0A0A14))
            .clickable(enabled = canSkip) { onDone() },
        contentAlignment = Alignment.Center,
    ) {
        if (phase == 0) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SlimeView(mascotVisual, Modifier.size(180.dp).scale(pulse), mood = Mood.HAPPY, radiusFactor = 0.34f)
                Text("Slimeverse", fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 46.sp, color = Color(0xFF5E35D1))
                Text("IDLE EVOLUTION RPG", fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 3.sp, color = Color(0xFF9B93A3))
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { i ->
                        val active = dot.toInt() % 3 == i
                        Box(Modifier.size(if (active) 10.dp else 7.dp).clip(CircleShape).background(if (active) Color(0xFF7C4DFF) else Color(0xFFD8CCEC)))
                    }
                }
            }
        } else {
            F7SplashVideo(onFinished = {})
        }
    }
}

@Composable
private fun F7SplashVideo(onFinished: () -> Unit) {
    val context = LocalContext.current
    var failed by remember { mutableStateOf(false) }
    val player = remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            player.value?.release()
            player.value = null
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (!failed) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    SurfaceView(ctx).apply {
                        holder.addCallback(object : android.view.SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                                try {
                                    val mp = MediaPlayer.create(ctx, R.raw.f7_splash)
                                    mp.setSurface(holder.surface)
                                    mp.setVolume(0f, 0f)
                                    mp.setOnCompletionListener { onFinished() }
                                    mp.setOnErrorListener { _, _, _ -> failed = true; true }
                                    mp.start()
                                    player.value = mp
                                } catch (e: Exception) {
                                    failed = true
                                }
                            }
                            override fun surfaceChanged(holder: android.view.SurfaceHolder, f: Int, w: Int, h: Int) {}
                            override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {
                                player.value?.release(); player.value = null
                            }
                        })
                    }
                },
            )
        }
        if (failed) {
            Image(
                painter = painterResource(R.drawable.f7_splash_final),
                contentDescription = "F7 Developer",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}