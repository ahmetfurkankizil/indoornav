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
    val bufferView: Int? = null,
    val byteOffset: Int = 0,
    val componentType: Int, // 5126 = FLOAT
    val count: Int,
    val type: String, // "VEC3", "SCALAR", etc.
    val max: List<Double>? = null,
    val min: List<Double>? = null,
)

@Serializable
data class GltfBufferView(
    val buffer: Int = 0,
    val byteOffset: Int = 0,
    val byteLength: Int,
    val byteStride: Int? = null,
    val target: Int? = null,
)

@Serializable
data class GltfBuffer(
    val byteLength: Int,
    val uri: String? = null, // null for GLB embedded binary
)

@Serializable
data class GltfNode(
    val name: String? = null,
    val mesh: Int? = null,
    val children: List<Int>? = null,
    val translation: List<Double>? = null,
    val rotation: List<Double>? = null,
    val scale: List<Double>? = null,
    val matrix: List<Double>? = null,
)

@Serializable
data class GltfScene(
    val name: String? = null,
    val nodes: List<Int>? = null,
)

// ── Geometry result types ────────────────────────────

data class Vec3(val x: Float, val y: Float, val z: Float)

data class BoundingBox3D(
    val minX: Float, val minY: Float, val minZ: Float,
    val maxX: Float, val maxY: Float, val maxZ: Float,
) {
    val extentX: Float get() = maxX - minX
    val extentY: Float get() = maxY - minY
    val extentZ: Float get() = maxZ - minZ
}

/**
 * Parsed GLB data: the JSON chunk as a typed model, plus the raw binary chunk.
 */
data class GlbData(
    val json: GltfJson,
    val binChunk: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GlbData) return false
        return json == other.json && binChunk.contentEquals(other.binChunk)
    }

    override fun hashCode(): Int {
        return 31 * json.hashCode() + binChunk.contentHashCode()
    }
}
