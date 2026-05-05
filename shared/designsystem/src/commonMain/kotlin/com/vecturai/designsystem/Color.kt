package com.VecturAI.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object VecturAIColors {
    val Primary = Color(0xFF2563EB)
    val PrimaryLight = Color(0xFF60A5FA)
    val PrimaryDark = Color(0xFF1D4ED8)
    val OnPrimary = Color(0xFFFFFFFF)

    val Secondary = Color(0xFF0D9488)
    val SecondaryLight = Color(0xFF2DD4BF)
    val OnSecondary = Color(0xFFFFFFFF)

    val Accent = Color(0xFFF59E0B)
    val AccentLight = Color(0xFFFBBF24)
    val OnAccent = Color(0xFF1F2937)

    val Background = Color(0xFFF8FAFC)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFF1F5F9)
    val OnBackground = Color(0xFF0F172A)
    val OnSurface = Color(0xFF1E293B)
    val OnSurfaceVariant = Color(0xFF64748B)

    val BackgroundDark = Color(0xFF0F172A)
    val SurfaceDark = Color(0xFF1E293B)
    val SurfaceVariantDark = Color(0xFF334155)
    val OnBackgroundDark = Color(0xFFF1F5F9)
    val OnSurfaceDark = Color(0xFFE2E8F0)

    val Success = Color(0xFF10B981)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFEF4444)
    val Info = Color(0xFF3B82F6)

    val ArrowColor = Color(0xFF2563EB)
    val PathColor = Color(0xFF60A5FA)
    val MarkerColor = Color(0xFF10B981)
    val DestinationColor = Color(0xFFF59E0B)

    val SurfaceCanvas = Color(0xFF070D18)
    val SurfaceElevated = Color(0xFF121A28)
    val SurfaceCard = Color(0xFF151F31)
    val SurfaceOverlay = Color(0xFF1B2436)

    val BorderSubtle = Color(0xFF233149)
    val BorderStrong = Color(0xFF2B3952)

    val TextPrimary = Color(0xFFF1F5F9)
    val TextSecondary = Color(0xFFB6BFCE)
    val TextMuted = Color(0xFF8E99AE)
    val TextDisabled = Color(0xFF566173)

    val AccentCyan = Color(0xFF22D3EE)
    val AccentGreen = Color(0xFF12C86A)
    val AccentAmber = Color(0xFFF59E0B)
    val AccentRed = Color(0xFFEF4444)

    val GradientStart = Color(0xFF1D4ED8)
    val GradientMid = Color(0xFF2563EB)
    val GradientEnd = Color(0xFF06B6D4)
}

object VecturAIBrush {
    val Primary: Brush
        @Composable
        get() = Brush.linearGradient(
            colors = listOf(
                VecturAIColors.GradientStart,
                VecturAIColors.GradientMid,
                VecturAIColors.GradientEnd,
            ),
        )
}
