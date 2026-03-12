package com.vecturai.tools.preprocessor.analysis

import com.vecturai.tools.preprocessor.glb.Vec3
import kotlin.math.roundToInt

/**
 * Estimates the dominant floor plane from a cloud of mesh vertices.
 *
 * Strategy: build a Y-histogram, find the peak bin → that's the floor Y.
 * Vertices within ±tolerance of floor Y are considered "floor-level".
 *
 * This is NOT real RANSAC plane-fitting — it assumes floors are roughly
 * horizontal (Y-up convention) which is true for Polycam/LiDAR room scans.
 */
class FloorPlaneEstimator(
    private val binWidth: Double = 0.05,
    private val floorTolerance: Double = 0.15,
) {

    data class FloorEstimate(
        /** Estimated Y-coordinate of the floor plane. */
        val floorY: Double,
        /** Fraction of total vertices in the floor band (0.0–1.0). */
        val confidence: Double,
        /** Number of vertices classified as floor-level. */
        val floorVertexCount: Int,
        /** Total vertices analyzed. */
        val totalVertexCount: Int,
        /** All vertices within the floor tolerance band. */
        val floorVertices: List<Vec3>,
    )

    /**
     * Estimate the floor plane from vertex positions.
     *
     * @param vertices All mesh vertices (Y-up coordinate system)
     * @return FloorEstimate or null if insufficient data
     */
    fun estimate(vertices: List<Vec3>): FloorEstimate? {
        if (vertices.size < 10) return null

        // Build Y-histogram
        val yValues = vertices.map { it.y.toDouble() }
        val minY = yValues.min()
        val maxY = yValues.max()

        if (maxY - minY < binWidth) {
            // All vertices at same height — trivial case
            val avgY = yValues.average()
            return FloorEstimate(
                floorY = avgY,
                confidence = 1.0,
                floorVertexCount = vertices.size,
                totalVertexCount = vertices.size,
                floorVertices = vertices,
            )
        }

        val binCount = ((maxY - minY) / binWidth).roundToInt().coerceAtLeast(1)
        val bins = IntArray(binCount + 1)

        for (y in yValues) {
            val binIndex = ((y - minY) / binWidth).roundToInt().coerceIn(0, binCount)
            bins[binIndex]++
        }

        // Find the bin with the most vertices
        val peakBinIndex = bins.indices.maxByOrNull { bins[it] } ?: return null
        val peakY = minY + peakBinIndex * binWidth

        // Collect floor-level vertices
        val floorVertices = vertices.filter {
            val dy = it.y.toDouble() - peakY
            dy >= -floorTolerance && dy <= floorTolerance
        }

        val confidence = floorVertices.size.toDouble() / vertices.size

        return FloorEstimate(
            floorY = peakY,
            confidence = confidence,
            floorVertexCount = floorVertices.size,
            totalVertexCount = vertices.size,
            floorVertices = floorVertices,
        )
    }
}
