package com.vecturai.designsystem

import androidx.compose.ui.graphics.Color

/**
 * VecturAI color palette.
 *
 * A curated, premium color system designed for indoor navigation.
 * Uses a cool blue primary with warm accents for wayfinding elements.
 */
object VecturaiColors {
    // Primary — Cool blue for trust and wayfinding
    val Primary = Color(0xFF2563EB)
    val PrimaryLight = Color(0xFF60A5FA)
    val PrimaryDark = Color(0xFF1D4ED8)
    val OnPrimary = Color(0xFFFFFFFF)

    // Secondary — Teal for AR overlay accents
    val Secondary = Color(0xFF0D9488)
    val SecondaryLight = Color(0xFF2DD4BF)
    val OnSecondary = Color(0xFFFFFFFF)

    // Accent — Amber for attention/arrows
    val Accent = Color(0xFFF59E0B)
    val AccentLight = Color(0xFFFBBF24)
    val OnAccent = Color(0xFF1F2937)

    // Surface
    val Background = Color(0xFFF8FAFC)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFF1F5F9)
    val OnBackground = Color(0xFF0F172A)
    val OnSurface = Color(0xFF1E293B)
    val OnSurfaceVariant = Color(0xFF64748B)

    // Dark mode surfaces
    val BackgroundDark = Color(0xFF0F172A)
    val SurfaceDark = Color(0xFF1E293B)
    val SurfaceVariantDark = Color(0xFF334155)
    val OnBackgroundDark = Color(0xFFF1F5F9)
    val OnSurfaceDark = Color(0xFFE2E8F0)

    // Semantic
    val Success = Color(0xFF10B981)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFEF4444)
    val Info = Color(0xFF3B82F6)

    // Navigation-specific
    val ArrowColor = Color(0xFF2563EB)
    val PathColor = Color(0xFF60A5FA)
    val MarkerColor = Color(0xFF10B981)
    val DestinationColor = Color(0xFFF59E0B)
}
