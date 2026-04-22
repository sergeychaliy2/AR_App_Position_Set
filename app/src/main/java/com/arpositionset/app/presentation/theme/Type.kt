package com.arpositionset.app.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Display = FontFamily.Default

val ArTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = (-0.5).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp, lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = 0.1.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Medium,
        fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.15.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 22.sp, letterSpacing = 0.2.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 19.sp, letterSpacing = 0.25.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.6.sp,
    ),
)
