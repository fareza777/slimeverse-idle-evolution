@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.f7developer.slimeverse.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.f7developer.slimeverse.R

val Fredoka = FontFamily(
    Font(R.font.fredoka, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.fredoka, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.fredoka, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.fredoka, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

val Nunito = FontFamily(
    Font(R.font.nunito, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.nunito, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.nunito, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.nunito, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.nunito, FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
)

val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 46.sp),
    displayMedium = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp),
    displaySmall = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineLarge = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp),
    headlineMedium = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    headlineSmall = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 25.sp),
    titleLarge = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Medium, fontSize = 12.5.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 15.sp),
)