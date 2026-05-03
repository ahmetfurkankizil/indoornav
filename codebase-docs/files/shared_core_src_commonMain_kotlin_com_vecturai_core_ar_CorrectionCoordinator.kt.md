# File Dossier: CorrectionCoordinator.kt

## Path
`shared\core\src\commonMain\kotlin\com\Vectura AI\core\ar\CorrectionCoordinator.kt`

## Type
Authored Source

## Role
Authored Source for the shared component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
package com.Vectura AI.core.ar

import com.Vectura AI.core.domain.CheckpointMarker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

/**
 * Coordinates checkpoint-based alignment corrections.
 *
 * When the native AR layer observes a known checkpoint marker, it calls
 * [onCheckpointObserved] with the observation data. This coordinator:
 * 1. Resolves the marker's known building-local reference pose
 * 2. Computes what the alignment transform SHOULD be based on the observation
 * 3. Compares against the current alignment to compute a correction delta
 * 4. Applies a BOUNDED correction (max translation / rotation)
 * 5. Updates the alignment transform and notifies listeners
 *
 * Design: conservative, bounded corrections to avoid jarring jumps.
 */
class CorrectionCoordinator(
    /** Maximum translation correction per observation (meters). */
    private val maxTranslationMeters: Double 
```

## Status
Mapped (Pass 3 Normalization)
