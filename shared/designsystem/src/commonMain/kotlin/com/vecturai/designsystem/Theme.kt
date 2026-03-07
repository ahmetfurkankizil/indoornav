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
    error = VecturaiColors.Error,
)

private val DarkColorScheme = darkColorScheme(
    primary = VecturaiColors.PrimaryLight,
    onPrimary = VecturaiColors.PrimaryDark,
    primaryContainer = VecturaiColors.Primary,
    secondary = VecturaiColors.SecondaryLight,
    onSecondary = VecturaiColors.PrimaryDark,
    secondaryContainer = VecturaiColors.Secondary,
    tertiary = VecturaiColors.AccentLight,
    onTertiary = VecturaiColors.OnAccent,
    background = VecturaiColors.BackgroundDark,
    onBackground = VecturaiColors.OnBackgroundDark,
    surface = VecturaiColors.SurfaceDark,
    onSurface = VecturaiColors.OnSurfaceDark,
    surfaceVariant = VecturaiColors.SurfaceVariantDark,
    onSurfaceVariant = VecturaiColors.OnSurfaceVariant,
    error = VecturaiColors.Error,
)

/**
 * VecturAI Material 3 theme.
 *
 * @param darkTheme Whether to use dark color scheme
 * @param content The themed content
 */
@Composable
fun VecturaiTheme(
    darkTheme: Boolean = false, // TODO: Follow system setting
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VecturaiTypography,
        content = content,
    )
}
