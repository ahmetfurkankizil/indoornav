# File Dossier: Typography.kt

## Path
`shared\designsystem\src\commonMain\kotlin\com\vecturai\designsystem\Typography.kt`

## Type
Authored Source

## Role
Authored Source for the shared component.

## Logic Overview
```kotlin
package com.vecturai.designsystem

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object VecturaiTypography {
    val NumericDisplay = TextStyle(
        fontSize = 64.sp,
        fontWeight = FontWeight.ExtraBold,
        lineHeight = 64.sp,
        fontFeatureSettings = "tnum",
    )

    val NumericLarge = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.ExtraBold,
        lineHeight = 32.sp,
        fontFeatureSettings = "tnum",
    )

    val Overline = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        lineHeight = 14.sp,
        letterSpacing = 1.4.sp,
    )

    @Composable
    fun material(): Typography {
        // Loads Inter font family via Compose Resources
        // and applies it to all Material 3 typography slots.
    }
}
```


## Status
Mapped (Pass 3 Normalization)
