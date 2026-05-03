package com.Vectura AI.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Vectura AIColors.Primary,
    onPrimary = Vectura AIColors.OnPrimary,
    primaryContainer = Vectura AIColors.PrimaryLight,
    secondary = Vectura AIColors.Secondary,
    onSecondary = Vectura AIColors.OnSecondary,
    secondaryContainer = Vectura AIColors.SecondaryLight,
    tertiary = Vectura AIColors.Accent,
    onTertiary = Vectura AIColors.OnAccent,
    background = Vectura AIColors.Background,
    onBackground = Vectura AIColors.OnBackground,
    surface = Vectura AIColors.Surface,
    onSurface = Vectura AIColors.OnSurface,
    surfaceVariant = Vectura AIColors.SurfaceVariant,
    onSurfaceVariant = Vectura AIColors.OnSurfaceVariant,
    outline = Vectura AIColors.BorderSubtle,
    error = Vectura AIColors.Error,
)

private val DarkColorScheme = darkColorScheme(
    primary = Vectura AIColors.Primary,
    onPrimary = Vectura AIColors.OnPrimary,
    primaryContainer = Vectura AIColors.PrimaryDark,
    secondary = Vectura AIColors.AccentCyan,
    onSecondary = Vectura AIColors.SurfaceCanvas,
    secondaryContainer = Vectura AIColors.Secondary,
    tertiary = Vectura AIColors.AccentAmber,
    onTertiary = Vectura AIColors.OnAccent,
    background = Vectura AIColors.SurfaceCanvas,
    onBackground = Vectura AIColors.TextPrimary,
    surface = Vectura AIColors.SurfaceCard,
    onSurface = Vectura AIColors.TextPrimary,
    surfaceVariant = Vectura AIColors.SurfaceElevated,
    onSurfaceVariant = Vectura AIColors.TextMuted,
    outline = Vectura AIColors.BorderSubtle,
    error = Vectura AIColors.AccentRed,
)

@Composable
fun Vectura AITheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Vectura AITypography.material(),
        content = content,
    )
}
