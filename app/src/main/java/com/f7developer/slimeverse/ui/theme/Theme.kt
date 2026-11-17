package com.f7developer.slimeverse.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = VioletSoft,
    onPrimaryContainer = VioletDeep,
    secondary = Coral,
    onSecondary = Color.White,
    secondaryContainer = CoralSoft,
    onSecondaryContainer = Color(0xFFBF360C),
    tertiary = Mint,
    onTertiary = Color.White,
    tertiaryContainer = MintSoft,
    onTertiaryContainer = Color(0xFF00695C),
    background = Cream,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = CreamDeep,
    onSurfaceVariant = InkSoft,
    outline = Line,
    outlineVariant = Line,
    error = BadRed,
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = VioletNight,
    onPrimary = Color(0xFF2A1A5E),
    primaryContainer = Color(0xFF3A2E63),
    onPrimaryContainer = Color(0xFFE4DBFF),
    secondary = Color(0xFFFF8A65),
    onSecondary = Color(0xFF4E1A08),
    secondaryContainer = Color(0xFF54301F),
    onSecondaryContainer = Color(0xFFFFDBCF),
    tertiary = Color(0xFF4DD0B5),
    onTertiary = Color(0xFF00382C),
    tertiaryContainer = Color(0xFF1D4A3F),
    onTertiaryContainer = Color(0xFFC9F5E8),
    background = NightBg,
    onBackground = NightInk,
    surface = NightSurface,
    onSurface = NightInk,
    surfaceVariant = NightSurface2,
    onSurfaceVariant = NightInkSoft,
    outline = NightLine,
    outlineVariant = NightLine,
    error = Color(0xFFEF9A9A),
    onError = Color(0xFF5F1414),
)

data class ExtraColors(
    val inkSoft: Color,
    val inkFaint: Color,
    val line: Color,
    val card: Color,
    val cardAlt: Color,
    val gold: Color,
    val good: Color,
    val bad: Color,
    val isLight: Boolean,
)

val LocalExtraColors = staticCompositionLocalOf {
    ExtraColors(InkSoft, InkFaint, Line, Paper, PaperWarm, GoldC, GoodGreen, BadRed, true)
}

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

enum class ThemeMode { LIGHT, DARK, SYSTEM }

@Composable
fun SlimeverseTheme(mode: ThemeMode = ThemeMode.LIGHT, content: @Composable () -> Unit) {
    val light = when (mode) {
        ThemeMode.LIGHT -> true
        ThemeMode.DARK -> false
        ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val extras = if (light) {
        ExtraColors(InkSoft, InkFaint, Line, Paper, PaperWarm, GoldC, GoodGreen, BadRed, true)
    } else {
        ExtraColors(NightInkSoft, Color(0xFF7E7590), NightLine, NightSurface, NightSurface2, Color(0xFFFFCA28), Color(0xFF66BB6A), Color(0xFFEF9A9A), false)
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalExtraColors provides extras) {
        MaterialTheme(
            colorScheme = if (light) LightColors else DarkColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}