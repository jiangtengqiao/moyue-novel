package com.novel.reader.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = InkBlack,
    onPrimary = PaperWhite,
    primaryContainer = PaperLight,
    onPrimaryContainer = InkBlack,
    secondary = AccentGold,
    onSecondary = PaperWhite,
    secondaryContainer = AccentGoldLight,
    onSecondaryContainer = InkBlack,
    tertiary = AccentGoldDark,
    background = PaperWhite,
    onBackground = InkBlack,
    surface = PaperWhite,
    onSurface = InkBlack,
    surfaceVariant = PaperLight,
    onSurfaceVariant = InkLight,
    outline = InkLight.copy(alpha = 0.3f),
    outlineVariant = InkLight.copy(alpha = 0.15f),
    error = StatusError,
    onError = PaperWhite,
)

private val DarkColorScheme = darkColorScheme(
    primary = PaperWhite,
    onPrimary = InkBlack,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = PaperWhite,
    secondary = AccentGoldLight,
    onSecondary = InkBlack,
    secondaryContainer = AccentGoldDark,
    onSecondaryContainer = PaperWhite,
    tertiary = AccentGold,
    background = DarkBg,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOnSurfaceVariant.copy(alpha = 0.3f),
    outlineVariant = DarkOnSurfaceVariant.copy(alpha = 0.15f),
    error = StatusError,
    onError = PaperWhite,
)

@Composable
fun MoYueTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MoYueTypography,
        content = content,
    )
}
