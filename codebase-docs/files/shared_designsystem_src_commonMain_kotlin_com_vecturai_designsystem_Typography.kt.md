# File Dossier: Typography.kt

## Path
`shared\designsystem\src\commonMain\kotlin\com\Vectura AI\designsystem\Typography.kt`

## Type
Authored Source

## Role
Authored Source for the shared component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
package com.Vectura AI.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Vectura AI typography scale.
 *
 * Uses the system default font family (which maps to Roboto on Android
 * and SF Pro on iOS) for a native feel on each platform.
 *
 * TODO: Consider loading Inter or custom brand font via Compose Resources
 */
val Vectura AITypography = Typography(
    displayLarge = TextStyle(
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 40.sp,
        letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 34.sp,
    ),
    headlineLarge = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 30.sp,
    ),
    headlineMedium = TextStyle(
        fontSize = 20.sp,
        fontWeight = 
```

## Status
Mapped (Pass 3 Normalization)
