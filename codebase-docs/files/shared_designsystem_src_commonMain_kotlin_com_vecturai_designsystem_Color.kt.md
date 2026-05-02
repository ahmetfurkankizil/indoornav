# File Dossier: Color.kt

## Path
`shared\designsystem\src\commonMain\kotlin\com\vecturai\designsystem\Color.kt`

## Type
Authored Source

## Role
Authored Source for the shared component.

## Logic Overview
```kotlin
package com.vecturai.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object VecturaiColors {
    // ... legacy colors ...

    // New Polish Tokens (Phase 12)
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

object VecturaiBrush {
    val Primary: Brush
        @Composable
        get() = Brush.linearGradient(
            colors = listOf(
                VecturaiColors.GradientStart,
                VecturaiColors.GradientMid,
                VecturaiColors.GradientEnd,
            ),
        )
}
```


## Status
Mapped (Pass 3 Normalization)
