# File Dossier: AuthoringConfig.kt

## Path
`tools\nav-preprocessor\src\main\kotlin\com\vecturai\tools\preprocessor\model\AuthoringConfig.kt`

## Type
Authored Source

## Role
Authored Source for the tools component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
package com.vecturai.tools.preprocessor.model

import kotlinx.serialization.Serializable

/**
 * Human-authored building annotation config — input to the preprocessor.
 *
 * All coordinates use the building-local coordinate system:
 * - meters, Y-up, right-handed
 * - origin chosen by the author
 */
@Serializable
data class AuthoringConfig(
    val buildingId: String,
    val buildingName: String,
    val floorId: String = "ground",
    val asset: AssetReference,
    val tags: List<String> = emptyList(),
    val entranceMarkers: List<AuthoringMarker>,
    val checkpointMarkers: List<AuthoringCheckpointMarker> = emptyList(),
    val nodes: List<AuthoringNode>,
    val edges: List<AuthoringEdge>,
    val rooms: List<AuthoringRoom>,
    val routeRendering: AuthoringRouteRendering = AuthoringRouteRendering(),
    val graphMetadata: GraphMetadata? = null,
)

@Serializable
data class AssetReference(
    val glbFile: String,
    val sourceApp: String = "polycam",
    val scanDate: String? = n
```

## Status
Mapped (Pass 3 Normalization)
