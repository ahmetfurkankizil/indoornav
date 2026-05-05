package com.vecturai.tools.preprocessor.glb

import com.vecturai.tools.preprocessor.ValidationException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Extracts mesh vertex positions from parsed GLB data.
 *
 * Walks meshes → primitives → POSITION accessor → bufferView → reads
 * float32 triples from the binary chunk.
 */
class GlbGeometryExtractor {

    companion object {
        const val COMPONENT_TYPE_FLOAT = 5126
        const val FLOAT_BYTE_SIZE = 4
        const val VEC3_COMPONENT_COUNT = 3
        const val VEC3_BYTE_SIZE = VEC3_COMPONENT_COUNT * FLOAT_BYTE_SIZE // 12
    }

    data class GeometryResult(
        val vertices: List<Vec3>,
        val boundingBox: BoundingBox3D?,
        val meshCount: Int,
        val primitiveCount: Int,
    )

    /**
     * Extract all vertex positions from the GLB data.
     *
     * @return GeometryResult with all vertices and computed bounding box
     */
    fun extract(glbData: GlbData): GeometryResult {
        val gltf = glbData.json
        val bin = glbData.binChunk
        val allVertices = mutableListOf<Vec3>()
        var primitiveCount = 0

        for (mesh in gltf.meshes) {
            for (primitive in mesh.primitives) {
                val positionAccessorIndex = primitive.attributes["POSITION"] ?: continue
                primitiveCount++

                val accessor = gltf.accessors.getOrNull(positionAccessorIndex) ?: continue
                if (accessor.type != "VEC3") continue
                if (accessor.componentType != COMPONENT_TYPE_FLOAT) {
                    // We only support float32 positions for now
                    continue
                }

                val bufferViewIndex = accessor.bufferView ?: continue
                val bufferView = gltf.bufferViews.getOrNull(bufferViewIndex) ?: continue

                val stride = bufferView.byteStride ?: VEC3_BYTE_SIZE
                val baseOffset = bufferView.byteOffset + accessor.byteOffset

                if (baseOffset + accessor.count * stride > bin.size) {
                    // Buffer overrun — skip this primitive rather than crashing
                    continue
                }

                val buffer = ByteBuffer.wrap(bin).order(ByteOrder.LITTLE_ENDIAN)

                for (i in 0 until accessor.count) {
                    val offset = baseOffset + i * stride
                    buffer.position(offset)
                    val x = buffer.getFloat()
                    val y = buffer.getFloat()
                    val z = buffer.getFloat()
                    allVertices.add(Vec3(x, y, z))
                }
            }
        }

        val bbox = if (allVertices.isNotEmpty()) computeBoundingBox(allVertices) else null

        return GeometryResult(
            vertices = allVertices,
            boundingBox = bbox,
            meshCount = gltf.meshes.size,
            primitiveCount = primitiveCount,
        )
    }

    private fun computeBoundingBox(vertices: List<Vec3>): BoundingBox3D {
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var minZ = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE; var maxY = Float.MIN_VALUE; var maxZ = Float.MIN_VALUE

        for (v in vertices) {
            if (v.x < minX) minX = v.x; if (v.x > maxX) maxX = v.x
            if (v.y < minY) minY = v.y; if (v.y > maxY) maxY = v.y
            if (v.z < minZ) minZ = v.z; if (v.z > maxZ) maxZ = v.z
        }

        return BoundingBox3D(minX, minY, minZ, maxX, maxY, maxZ)
    }
}
