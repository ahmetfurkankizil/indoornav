package com.example.indoornavmvp.data

/**
 * Represents a navigation node placed by the admin in AR space.
 * Contains the 4x4 transformation matrix (16 floats) representing the pose.
 */
data class LocalNode(
    val id: Int,
    val label: String,
    val poseMatrix: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LocalNode

        if (id != other.id) return false
        if (label != other.label) return false
        if (!poseMatrix.contentEquals(other.poseMatrix)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + label.hashCode()
        result = 31 * result + poseMatrix.contentHashCode()
        return result
    }
}

/**
 * Data Transfer Object for JSON serialization of nodes.
 * Uses List<Float> instead of FloatArray for Gson compatibility.
 */
data class NodeDto(
    val id: Int,
    val label: String,
    val pose: List<Float>
) {
    fun toLocalNode(): LocalNode = LocalNode(
        id = id,
        label = label,
        poseMatrix = pose.toFloatArray()
    )

    companion object {
        fun fromLocalNode(node: LocalNode): NodeDto = NodeDto(
            id = node.id,
            label = node.label,
            pose = node.poseMatrix.toList()
        )
    }
}

