# File Dossier: OffRouteDetector.kt

## Path
`shared\core\src\commonMain\kotlin\com\Vectura AI\core\ar\OffRouteDetector.kt`

## Type
Authored Source

## Role
Authored Source for the shared component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
package com.Vectura AI.core.ar

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Detects off-route conditions based on multiple signals.
 *
 * Signals:
 * - Lateral deviation from route polyline
 * - Stale pose updates (time since last pose)
 * - Tracking-limited state from native AR
 * - Repeated large correction magnitudes
 *
 * Produces an [OffRouteStatus] and [RecoveryRecommendation].
 * Does NOT auto-cancel sessions — recommendations are passive.
 */
class OffRouteDetector(
    /** Minor drift threshold (meters). */
    private val minorDriftThreshold: Double = 2.0,
    /** Low confidence threshold (meters). */
    private val lowConfidenceThreshold: Double = 4.0,
    /** Likely off-route threshold (meters). */
    private val offRouteThreshold: Double = 6.0,
    /** Stale pose threshold (milliseconds). */
    private val stalePoseThresholdMs: Long = 3000L,
    /** Large correction threshold (meters) — repeated large corrections suggest instability. */
    private val largeCor
```

## Status
Mapped (Pass 3 Normalization)
