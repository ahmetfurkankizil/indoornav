package com.Vectura AI.android.ar

import com.Vectura AI.android.data.ArrowPlacementType
import kotlin.math.sqrt

internal object Arrow3DGeometry {
    const val COMPONENTS_PER_VERTEX = 7
    const val POSITION_OFFSET = 0
    const val NORMAL_OFFSET = 3
    const val GRADIENT_OFFSET = 6
    const val STRIDE_BYTES = COMPONENTS_PER_VERTEX * 4

    val arrowVertices: FloatArray
    val arrowIndices: ShortArray
    val shadowVertices: FloatArray = buildShadowFan()
    val shadowVertexCount: Int = shadowVertices.size / 3

    private val TOP_NORMAL = floatArrayOf(0f, 1f, 0f)
    private val BOTTOM_NORMAL = floatArrayOf(0f, -1f, 0f)

    init {
        val mesh = buildArrowMesh()
        arrowVertices = mesh.vertices
        arrowIndices = mesh.indices
    }

    data class ArrowColors(
        val top: FloatArray,
        val bottom: FloatArray,
    )

    fun colorsFor(type: ArrowPlacementType): ArrowColors = when (type) {
        ArrowPlacementType.FOLLOW -> FOLLOW_COLORS
        ArrowPlacementType.TURN_LEFT,
        ArrowPlacementType.TURN_RIGHT -> TURN_COLORS
        ArrowPlacementType.U_TURN -> U_TURN_COLORS
        ArrowPlacementType.DESTINATION -> DESTINATION_COLORS
    }

    private data class Mesh(
        val vertices: FloatArray,
        val indices: ShortArray,
    )

    private data class Point2(
        val x: Float,
        val z: Float,
    )

    private fun buildArrowMesh(): Mesh {
        // 0.44m x 0.50m x 0.04m chevron prism, with local +Z as route-forward.
        val rearLeft = Point2(-0.14f, -0.25f)
        val rearRight = Point2(0.14f, -0.25f)
        val shoulderRight = Point2(0.14f, 0.03f)
        val headRight = Point2(0.22f, 0.03f)
        val tip = Point2(0f, 0.25f)
        val headLeft = Point2(-0.22f, 0.03f)
        val shoulderLeft = Point2(-0.14f, 0.03f)
        val boundary = listOf(
            rearLeft,
            rearRight,
            shoulderRight,
            headRight,
            tip,
            headLeft,
            shoulderLeft,
        )

        val vertices = mutableListOf<Float>()
        val indices = mutableListOf<Short>()
        val topY = 0.04f

        addFaceTriangle(vertices, indices, rearLeft, rearRight, shoulderRight, topY, TOP_NORMAL, 1f)
        addFaceTriangle(vertices, indices, rearLeft, shoulderRight, shoulderLeft, topY, TOP_NORMAL, 1f)
        addFaceTriangle(vertices, indices, headLeft, headRight, tip, topY, TOP_NORMAL, 1f)

        addFaceTriangle(vertices, indices, rearRight, rearLeft, shoulderRight, 0f, BOTTOM_NORMAL, 0f)
        addFaceTriangle(vertices, indices, shoulderRight, rearLeft, shoulderLeft, 0f, BOTTOM_NORMAL, 0f)
        addFaceTriangle(vertices, indices, headRight, headLeft, tip, 0f, BOTTOM_NORMAL, 0f)

        boundary.indices.forEach { i ->
            val a = boundary[i]
            val b = boundary[(i + 1) % boundary.size]
            val normal = sideNormal(a, b)
            addSideQuad(vertices, indices, a, b, topY, normal)
        }

        return Mesh(
            vertices = vertices.toFloatArray(),
            indices = indices.toShortArray(),
        )
    }

    private fun addFaceTriangle(
        vertices: MutableList<Float>,
        indices: MutableList<Short>,
        a: Point2,
        b: Point2,
        c: Point2,
        y: Float,
        normal: FloatArray,
        gradient: Float,
    ) {
        val start = vertices.vertexCount()
        vertices.addVertex(a.x, y, a.z, normal, gradient)
        vertices.addVertex(b.x, y, b.z, normal, gradient)
        vertices.addVertex(c.x, y, c.z, normal, gradient)
        indices.add(start)
        indices.add((start + 1).toShort())
        indices.add((start + 2).toShort())
    }

    private fun addSideQuad(
        vertices: MutableList<Float>,
        indices: MutableList<Short>,
        a: Point2,
        b: Point2,
        topY: Float,
        normal: FloatArray,
    ) {
        val start = vertices.vertexCount()
        vertices.addVertex(a.x, 0f, a.z, normal, 0f)
        vertices.addVertex(b.x, 0f, b.z, normal, 0f)
        vertices.addVertex(b.x, topY, b.z, normal, 1f)
        vertices.addVertex(a.x, topY, a.z, normal, 1f)

        indices.add(start)
        indices.add((start + 1).toShort())
        indices.add((start + 2).toShort())
        indices.add(start)
        indices.add((start + 2).toShort())
        indices.add((start + 3).toShort())
    }

    private fun MutableList<Float>.addVertex(
        x: Float,
        y: Float,
        z: Float,
        normal: FloatArray,
        gradient: Float,
    ) {
        add(x)
        add(y)
        add(z)
        add(normal[0])
        add(normal[1])
        add(normal[2])
        add(gradient)
    }

    private fun MutableList<Float>.vertexCount(): Short =
        (size / COMPONENTS_PER_VERTEX).toShort()

    private fun sideNormal(a: Point2, b: Point2): FloatArray {
        val dx = b.x - a.x
        val dz = b.z - a.z
        val nx = dz
        val nz = -dx
        val len = sqrt(nx * nx + nz * nz).coerceAtLeast(0.0001f)
        return floatArrayOf(nx / len, 0f, nz / len)
    }

    private fun buildShadowFan(): FloatArray {
        val segments = 16
        val radius = 0.18f
        val vertices = FloatArray((segments + 2) * 3)
        vertices[0] = 0f
        vertices[1] = 0f
        vertices[2] = 0f
        for (i in 0..segments) {
            val angle = (i.toDouble() / segments.toDouble()) * Math.PI * 2.0
            val offset = 3 + i * 3
            vertices[offset] = (radius * kotlin.math.cos(angle)).toFloat()
            vertices[offset + 1] = 0f
            vertices[offset + 2] = (radius * kotlin.math.sin(angle)).toFloat()
        }
        return vertices
    }

    private fun rgb(hex: Int): FloatArray =
        floatArrayOf(
            ((hex shr 16) and 0xFF) / 255f,
            ((hex shr 8) and 0xFF) / 255f,
            (hex and 0xFF) / 255f,
        )

    private val FOLLOW_COLORS = ArrowColors(rgb(0x168BFF), rgb(0x0A3A66)) // Vibrant Blue
    private val TURN_COLORS = ArrowColors(rgb(0xFACC15), rgb(0xA16207))   // Vibrant Yellow (Amber-400)
    private val U_TURN_COLORS = ArrowColors(rgb(0xFB923C), rgb(0x9A3412)) // Orange
    private val DESTINATION_COLORS = ArrowColors(rgb(0x24E37A), rgb(0x064E3B)) // Emerald Green
}
