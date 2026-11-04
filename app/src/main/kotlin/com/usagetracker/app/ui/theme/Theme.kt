package com.usagetracker.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = AccentGold,
    onPrimary = Color.White,
    primaryContainer = CardAltBg,
    onPrimaryContainer = TextPrimary,
    secondary = AccentOlive,
    onSecondary = Color.White,
    secondaryContainer = SurfaceVariant,
    onSecondaryContainer = TextPrimary,
    tertiary = AccentGoldDark,
    onTertiary = Color.White,
    background = CreamBg,
    onBackground = TextPrimary,
    surface = CardBg,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceTint = AccentGold,
    outline = BorderSubtle,
    outlineVariant = BorderSubtle,
    error = Danger,
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = AccentGoldDarkTheme,
    onPrimary = Color(0xFF2A1F17),
    primaryContainer = CardAltBgDark,
    onPrimaryContainer = TextPrimaryDark,
    secondary = AccentOlive,
    onSecondary = Color.White,
    secondaryContainer = SurfaceVariantDark,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = AccentGoldDarkTheme,
    onTertiary = Color(0xFF2A1F17),
    background = CreamBgDark,
    onBackground = TextPrimaryDark,
    surface = CardBgDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    surfaceTint = AccentGoldDarkTheme,
    outline = BorderSubtleDark,
    outlineVariant = BorderSubtleDark,
    error = Danger,
    onError = Color.White,
)

@Composable
fun UsageTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.background.toArgb()
            window.navigationBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = UsageTrackerTypography,
        shapes = UsageTrackerShapes,
        content = content
    )
}
