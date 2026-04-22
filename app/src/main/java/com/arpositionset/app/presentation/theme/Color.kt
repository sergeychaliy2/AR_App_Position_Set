package com.arpositionset.app.presentation.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Корпоративная палитра.
 * Основные: тёмно-зелёный (Pantone 329C) + красный (Pantone 192C).
 * Дополнительные: бирюзовый, светло-зелёный, серебристый, голубой, жёлтый, оранжевый.
 * Всегда в связке с основными.
 */
object ArColors {
    // ---- Основные корпоративные ----
    /** Pantone 329C — фирменный тёмно-зелёный. Используем как Primary. */
    val BrandGreen = Color(0xFF005A5A)
    val BrandGreenSoft = Color(0xFF16756F)
    val OnBrandGreen = Color(0xFFFFFFFF)

    /** Pantone 192C — фирменный красный. Акцентные действия, «delete». */
    val BrandRed = Color(0xFFE62142)
    val BrandRedSoft = Color(0xFFFF5472)
    val OnBrandRed = Color(0xFFFFFFFF)

    // ---- Дополнительные корпоративные ----
    val Turquoise = Color(0xFF00988E)   // Pantone 3272C
    val LightGreen = Color(0xFFB1D181)  // Pantone 358C
    val Silver = Color(0xFFC2C1C1)      // Pantone 877C
    val SkyBlue = Color(0xFF006AB2)     // Pantone 300C
    val Yellow = Color(0xFFE2B044)      // Pantone 143C
    val Orange = Color(0xFFD98815)      // Pantone 144C

    // ---- Совместимость со старыми именами, чтобы не переписывать весь UI ----
    val PrimaryViolet = BrandGreen
    val PrimaryDeep = Color(0xFF00332F)
    val OnPrimary = OnBrandGreen

    val SecondaryCyan = Turquoise
    val SecondaryDeep = Color(0xFF004B47)
    val OnSecondary = Color(0xFFFFFFFF)

    val Tertiary = BrandRed
    val OnTertiary = OnBrandRed

    // ---- Поверхности (dark, чтобы AR-фон читался) ----
    val BackgroundDark = Color(0xFF06171B)
    val SurfaceDark = Color(0xFF0B2327)
    val SurfaceElevated = Color(0xFF133236)
    val SurfaceGlass = Color(0x8C0B2327)
    val OnSurface = Color(0xFFE8F2F1)
    val OnSurfaceMuted = Color(0xFFA6BCBB)
    val Outline = Color(0x3DB1D181)

    // ---- Статусные ----
    val Success = LightGreen
    val Warning = Yellow
    val Error = BrandRed

    // ---- Градиенты ----
    val GradientAccent = Brush.linearGradient(
        colors = listOf(BrandGreen, Turquoise),
    )
    val GradientAttention = Brush.linearGradient(
        colors = listOf(BrandRed, Orange),
    )
    val GradientBackdrop = Brush.verticalGradient(
        colors = listOf(Color(0xCC06171B), Color(0x6606171B), Color(0x00000000)),
    )
    val GradientGlass = Brush.verticalGradient(
        colors = listOf(Color(0x66133236), Color(0x330B2327)),
    )
}
