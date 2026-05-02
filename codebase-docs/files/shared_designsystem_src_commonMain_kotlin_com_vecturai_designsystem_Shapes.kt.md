# File Dossier: Shapes.kt

## Path
`shared\designsystem\src\commonMain\kotlin\com\vecturai\designsystem\Shapes.kt`

## Type
Authored Source

## Role
Defines the shape scale (corner radii) for the VecturAI design system.

## Logic Overview
```kotlin
package com.vecturai.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object VecturaiShapes {
    val Small = RoundedCornerShape(8.dp)
    val Medium = RoundedCornerShape(14.dp)
    val Large = RoundedCornerShape(20.dp)
    val XLarge = RoundedCornerShape(28.dp)
    val Pill = RoundedCornerShape(50)
}
```

## Status
Mapped
