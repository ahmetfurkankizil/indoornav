package com.VecturAI.core.ar

import kotlinx.serialization.Serializable
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

/**
 * Platform-neutral alignment transform.
 *
 * Represents the transformation from building-local coordinates
 * to AR world coordinates after marker detection.
 *
 * For v1 (single floor, Y=0 plane), this is a 2D translation + Y-rotation:
 *   P_ar = rotate(P_bldg, rotationYDeg) + offset
 *
 * @property offsetX Translation X (AR_x - rotated_building_x)
 * @property offsetY Translation Y (vertical offset)
 * @property offsetZ Translation Z (AR_z - rotated_building_z)
 * @property rotationYDeg Y-axis rotation in degrees
 */
@Serializable
data class AlignmentTransform(
    val offsetX: Double = 0.0,
    val offsetY: Double = 0.0,
    val offsetZ: Double = 0.0,
    val rotationYDeg: Double = 0.0,
) {

    /**
     * Transform a building-local point to AR world coordinates.
     *
     * @return Triple(arX, arY, arZ)
     */
    fun transformPoint(buildingX: Double, buildingY: Double, buildingZ: Double): Triple<Double, Double, Double> {
        val radians = rotationYDeg * PI / 180.0
        val cosR = cos(radians)
        val sinR = sin(radians)

        // Rotate around Y axis, then translate
        val rotatedX = buildingX * cosR + buildingZ * sinR
        val rotatedZ = -buildingX * sinR + buildingZ * cosR

        return Triple(
            rotatedX + offsetX,
            buildingY + offsetY,
            rotatedZ + offsetZ,
        )
    }

    /**
     * Inverse transform: AR world point → building-local coordinates.
     *
     * Used for converting live camera position back to building coords
     * for route-relative progress estimation.
     *
     * Inverse of: P_ar = rotate(P_bldg, θ) + offset
     * Is:         P_bldg = rotate(P_ar - offset, -θ)
     *
     * @return Triple(buildingX, buildingY, buildingZ)
     */
    fun inverseTransformPoint(arX: Double, arY: Double, arZ: Double): Triple<Double, Double, Double> {
        val radians = -rotationYDeg * PI / 180.0
        val cosR = cos(radians)
        val sinR = sin(radians)

        // Subtract offset, then rotate by -θ
        val tx = arX - offsetX
        val tz = arZ - offsetZ

        return Triple(
            tx * cosR + tz * sinR,
            arY - offsetY,
            -tx * sinR + tz * cosR,
        )
    }

    /**
     * Transform a building-local direction vector to AR world.
     * (Translation not applied — direction only.)
     */
    fun transformDirection(dx: Double, dy: Double, dz: Double): Triple<Double, Double, Double> {
        val radians = rotationYDeg * PI / 180.0
        val cosR = cos(radians)
        val sinR = sin(radians)

        return Triple(
            dx * cosR + dz * sinR,
            dy,
            -dx * sinR + dz * cosR,
        )
    }

    companion object {
        /**
         * Compute alignment transform from a marker alignment result.
         *
         * T_ar_bldg = T_ar_marker × T_bldg_marker⁻¹
         */
        fun fromMarkerAlignment(result: MarkerAlignmentResult): AlignmentTransform {
            val rotDeg = result.markerArRotationYDeg - result.markerBuildingRotationYDeg
            val radians = rotDeg * PI / 180.0
            val cosR = cos(radians)
            val sinR = sin(radians)

            // Rotate the building marker position, then compute offset
            val rotatedBldgX = result.markerBuildingX * cosR + result.markerBuildingZ * sinR
            val rotatedBldgZ = -result.markerBuildingX * sinR + result.markerBuildingZ * cosR

            return AlignmentTransform(
                offsetX = result.markerArX - rotatedBldgX,
                offsetY = result.markerArY - result.markerBuildingY,
                offsetZ = result.markerArZ - rotatedBldgZ,
                rotationYDeg = rotDeg,
            )
        }
    }
}
