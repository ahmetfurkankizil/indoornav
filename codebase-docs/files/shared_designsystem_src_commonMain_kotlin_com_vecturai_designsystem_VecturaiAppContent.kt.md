# File Dossier: VecturaiAppContent.kt

## Path
`shared\designsystem\src\commonMain\kotlin\com\vecturai\designsystem\VecturaiAppContent.kt`

## Type
Authored Source

## Role
Authored Source for the shared component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
package com.vecturai.designsystem

import androidx.compose.runtime.Composable
import com.vecturai.designsystem.navigation.AppNavigation

/**
 * Root app content composable.
 *
 * This is the entry point for the Compose Multiplatform UI,
 * wrapping the theme and navigation structure.
 *
 * @param onNavigateToAr Callback to launch the native AR navigation screen
 */
@Composable
fun VecturaiAppContent(
    onNavigateToAr: () -> Unit = {},
) {
    VecturaiTheme {
        AppNavigation(
            onNavigateToAr = onNavigateToAr,
        )
    }
}

```

## Status
Mapped (Pass 3 Normalization)
