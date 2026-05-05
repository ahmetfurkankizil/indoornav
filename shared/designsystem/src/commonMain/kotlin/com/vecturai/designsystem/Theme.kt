package com.VecturAI.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = VecturAIColors.Primary,
    onPrimary = VecturAIColors.OnPrimary,
    primaryContainer = VecturAIColors.PrimaryLight,
    secondary = VecturAIColors.Secondary,
    onSecondary = VecturAIColors.OnSecondary,
    secondaryContainer = VecturAIColors.SecondaryLight,
    tertiary = VecturAIColors.Accent,
    onTertiary = VecturAIColors.OnAccent,
    background = VecturAIColors.Background,
    onBackground = VecturAIColors.OnBackground,
    surface = VecturAIColors.Surface,
    onSurface = VecturAIColors.OnSurface,
    surfaceVariant = VecturAIColors.SurfaceVariant,
    onSurfaceVariant = VecturAIColors.OnSurfaceVariant,
    outline = VecturAIColors.BorderSubtle,
    error = VecturAIColors.Error,
)

private val DarkColorScheme = darkColorScheme(
    primary = VecturAIColors.Primary,
    onPrimary = VecturAIColors.OnPrimary,
    primaryContainer = VecturAIColors.PrimaryDark,
    secondary = VecturAIColors.AccentCyan,
    onSecondary = VecturAIColors.SurfaceCanvas,
    secondaryContainer = VecturAIColors.Secondary,
    tertiary = VecturAIColors.AccentAmber,
    onTertiary = VecturAIColors.OnAccent,
    background = VecturAIColors.SurfaceCanvas,
    onBackground = VecturAIColors.TextPrimary,
    surface = VecturAIColors.SurfaceCard,
    onSurface = VecturAIColors.TextPrimary,
    surfaceVariant = VecturAIColors.SurfaceElevated,
    onSurfaceVariant = VecturAIColors.TextMuted,
    outline = VecturAIColors.BorderSubtle,
    error = VecturAIColors.AccentRed,
)

@Composable
fun VecturAITheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = VecturAITypography.material(),
        content = content,
    )
}
