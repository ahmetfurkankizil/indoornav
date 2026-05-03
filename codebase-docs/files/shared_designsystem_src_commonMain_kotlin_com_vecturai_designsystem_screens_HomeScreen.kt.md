# File Dossier: HomeScreen.kt

## Path
`shared\designsystem\src\commonMain\kotlin\com\Vectura AI\designsystem\screens\HomeScreen.kt`

## Type
Authored Source

## Role
Authored Source for the shared component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
package com.Vectura AI.designsystem.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.Vectura AI.designsystem.Vectura AIButton
import com.Vectura AI.designsystem.Vectura AICard
import com.Vectura AI.designsystem.Vectura AISectionHeader

/**
 * Home screen — the main landing screen of Vectura AI.
 *
 * Displays a welcome message, quick actions, and building information.
 *
 * TODO: Load actual building data from repository
 * TODO: Show cached building status
 * TODO: Display recent destinations for quick re-navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToSearch: () -> Unit = {},
  
```

## Status
Mapped (Pass 3 Normalization)
