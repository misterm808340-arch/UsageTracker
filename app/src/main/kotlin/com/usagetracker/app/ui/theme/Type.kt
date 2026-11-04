package com.usagetracker.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Default = FontFamily.Default

val UsageTrackerTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.Bold,
        fontSize = 32.sp, lineHeight = 38.sp
    ),
    displayMedium = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.Bold,
        fontSize = 28.sp, lineHeight = 34.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp, lineHeight = 30.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 26.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp, lineHeight = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp
    ),
    titleSmall = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, lineHeight = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 18.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.Normal,
        fontSize = 11.sp, lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.Medium,
        fontSize = 10.sp, lineHeight = 14.sp
    ),
)
