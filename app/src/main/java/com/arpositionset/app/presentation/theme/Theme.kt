package com.arpositionset.app.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkScheme = darkColorScheme(
    primary = ArColors.BrandGreen,
    onPrimary = ArColors.OnBrandGreen,
    primaryContainer = ArColors.BrandGreenSoft,
    onPrimaryContainer = ArColors.OnBrandGreen,
    secondary = ArColors.Turquoise,
    onSecondary = ArColors.OnSecondary,
    secondaryContainer = ArColors.SecondaryDeep,
    onSecondaryContainer = ArColors.OnBrandGreen,
    tertiary = ArColors.BrandRed,
    onTertiary = ArColors.OnBrandRed,
    background = ArColors.BackgroundDark,
    onBackground = ArColors.OnSurface,
    surface = ArColors.SurfaceDark,
    onSurface = ArColors.OnSurface,
    surfaceVariant = ArColors.SurfaceElevated,
    onSurfaceVariant = ArColors.OnSurfaceMuted,
    outline = ArColors.Outline,
    error = ArColors.Error,
)

private val LightScheme = lightColorScheme(
    primary = ArColors.BrandGreen,
    secondary = ArColors.Turquoise,
    tertiary = ArColors.BrandRed,
)

@Composable
fun ArPositionSetTheme(
    darkTheme: Boolean = isSystemInDarkTheme() || true, // AR is always dark to keep camera feed vivid
    dynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    content: @Composable () -> Unit,
) {
    val scheme = when {
        dynamicColor -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkScheme
        else -> LightScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            @Suppress("UNUSED_EXPRESSION") scheme.background.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = ArTypography,
        shapes = ArShapes,
        content = content,
    )
}
