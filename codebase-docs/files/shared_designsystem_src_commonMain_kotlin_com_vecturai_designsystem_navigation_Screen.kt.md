# File Dossier: Screen.kt

## Path
`shared\designsystem\src\commonMain\kotlin\com\VecturAI\designsystem\navigation\Screen.kt`

## Type
Authored Source

## Role
Authored Source for the shared component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
package com.VecturAI.designsystem.navigation

/**
 * Sealed class defining all navigable screens in the app.
 *
 * AR navigation is NOT included here — it is launched as a native
 * activity/view and does not participate in Compose navigation.
 */
sealed class Screen(val route: String, val title: String) {
    data object Home : Screen("home", "VecturAI")
    data object Search : Screen("search", "Search")
    data object RoutePreview : Screen("route_preview", "Route Preview")
    data object History : Screen("history", "History")
    data object Settings : Screen("settings", "Settings")
}

```

## Status
Mapped (Pass 3 Normalization)
