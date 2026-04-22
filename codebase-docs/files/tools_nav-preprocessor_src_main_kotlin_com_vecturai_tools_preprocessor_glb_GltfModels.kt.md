# File Dossier: GltfModels.kt

## Path
`tools\nav-preprocessor\src\main\kotlin\com\vecturai\tools\preprocessor\glb\GltfModels.kt`

## Type
Authored Source

## Role
Authored Source for the tools component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
package com.vecturai.tools.preprocessor.glb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Minimal glTF 2.0 JSON model — only the subset needed for vertex extraction.
 *
 * Reference: https://registry.khronos.org/glTF/specs/2.0/glTF-2.0.html
 */

@Serializable
data class GltfJson(
    val meshes: List<GltfMesh> = emptyList(),
    val accessors: List<GltfAccessor> = emptyList(),
    val bufferViews: List<GltfBufferView> = emptyList(),
    val buffers: List<GltfBuffer> = emptyList(),
    val nodes: List<GltfNode> = emptyList(),
    val scenes: List<GltfScene> = emptyList(),
    val scene: Int? = null,
)

@Serializable
data class GltfMesh(
    val name: String? = null,
    val primitives: List<GltfPrimitive> = emptyList(),
)

@Serializable
data class GltfPrimitive(
    val attributes: Map<String, Int> = emptyMap(),
    val indices: Int? = null,
    val mode: Int = 4, // 4 = TRIANGLES
)

@Serializable
data class GltfAccessor(
    val bufferView: 
```

## Status
Mapped (Pass 3 Normalization)
