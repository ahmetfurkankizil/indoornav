package com.vecturai.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = VecturaiColors.Primary,
    onPrimary = VecturaiColors.OnPrimary,
    primaryContainer = VecturaiColors.PrimaryLight,
    secondary = VecturaiColors.Secondary,
    onSecondary = VecturaiColors.OnSecondary,
    secondaryContainer = VecturaiColors.SecondaryLight,
    tertiary = VecturaiColors.Accent,
    onTertiary = VecturaiColors.OnAccent,
    background = VecturaiColors.Background,
    onBackground = VecturaiColors.OnBackground,
    surface = VecturaiColors.Surface,
    onSurface = VecturaiColors.OnSurface,
    surfaceVariant = VecturaiColors.SurfaceVariant,
    onSurfaceVariant = VecturaiColors.OnSurfaceVariant,
    outline = VecturaiColors.BorderSubtle,
    error = VecturaiColors.Error,
)

private val DarkColorScheme = darkColorScheme(
    primary = VecturaiColors.Primary,
    onPrimary = VecturaiColors.OnPrimary,
    primaryContainer = VecturaiColors.PrimaryDark,
    secondary = VecturaiColors.AccentCyan,
    onSecondary = VecturaiColors.SurfaceCanvas,
    secondaryContainer = VecturaiColors.Secondary,
    tertiary = VecturaiColors.AccentAmber,
    onTertiary = VecturaiColors.OnAccent,
    background = VecturaiColors.SurfaceCanvas,
    onBackground = VecturaiColors.TextPrimary,
    surface = VecturaiColors.SurfaceCard,
    onSurface = VecturaiColors.TextPrimary,
    surfaceVariant = VecturaiColors.SurfaceElevated,
    onSurfaceVariant = VecturaiColors.TextMuted,
    outline = VecturaiColors.BorderSubtle,
    error = VecturaiColors.AccentRed,
)

@Composable
fun VecturaiTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = VecturaiTypography.material(),
        content = content,
    )
}
