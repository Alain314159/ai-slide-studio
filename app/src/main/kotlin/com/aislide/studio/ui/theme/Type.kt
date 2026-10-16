package com.aislide.studio.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Use system default fonts for v0.1 — Inter/Poppins loading via Google Fonts
// will be added in a later phase to keep v0.1 dependency-light.
private val SansSerif = FontFamily.SansSerif
private val Default = FontFamily.Default

// App typography
val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = SansSerif, fontWeight = FontWeight.Bold, fontSize = 57.sp),
    displayMedium = TextStyle(fontFamily = SansSerif, fontWeight = FontWeight.Bold, fontSize = 45.sp),
    displaySmall = TextStyle(fontFamily = SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 36.sp),
    headlineLarge = TextStyle(fontFamily = SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 28.sp),
    headlineSmall = TextStyle(fontFamily = SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 24.sp),
    titleLarge = TextStyle(fontFamily = SansSerif, fontWeight = FontWeight.Medium, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = Default, fontWeight = FontWeight.Medium, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = Default, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = Default, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = Default, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = Default, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = Default, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = Default, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = Default, fontWeight = FontWeight.Medium, fontSize = 11.sp)
)
