package com.vecturai.android.ar

import android.content.Context
import android.opengl.GLES20
import android.opengl.Matrix
import com.google.ar.core.Pose
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders navigation arrows in the ARCore scene.
 *
 * For v1, renders simple colored cubes/spheres at arrow positions.
 * Uses OpenGL ES 2.0 for rendering.
 *
 * Future: replace with proper 3D arrow models via Sceneform or Filament.
 */
class ArRouteRenderer {

    // Alignment transform
    private var offsetX: Double = 0.0
    private var offsetY: Double = 0.0
    private var offsetZ: Double = 0.0
    private var rotationYRad: Double = 0.0

    private var currentArrows: List<ArrowRenderData> = emptyList()

    /** Number of arrows currently set for rendering. */
    val renderedArrowCount: Int get() = currentArrows.size

    /**
     * Set the alignment transform (building-local → AR world).
     */
    fun setAlignmentTransform(
        offsetX: Double,
        offsetY: Double,
        offsetZ: Double,
        rotationYDeg: Double,
    ) {
        this.offsetX = offsetX
        this.offsetY = offsetY
        this.offsetZ = offsetZ
        this.rotationYRad = Math.toRadians(rotationYDeg)
    }

    /**
     * Update the arrow placements to render.
     */
    fun updateArrows(arrows: List<ArrowRenderData>) {
        currentArrows = arrows
    }

    /**
     * Clear all arrows.
     */
    fun clearArrows() {
        currentArrows = emptyList()
    }

    /**
     * Render arrows in the current GL frame.
     * Called from the GLSurfaceView render loop.
     *
     * For v1: renders debug points/cubes. A production renderer would
     * use Filament or Sceneform for proper 3D models.
     *
     * @param viewMatrix 4x4 view matrix from ARCore camera
     * @param projectionMatrix 4x4 projection matrix
     */
    fun draw(viewMatrix: FloatArray, projectionMatrix: FloatArray) {
        if (currentArrows.isEmpty()) return

        // For each arrow, compute AR-world position and draw a debug point
        for (arrow in currentArrows) {
            val arPos = transformToAR(arrow.positionX, arrow.positionY, arrow.positionZ)

            // In a full implementation, this would render a 3D mesh at arPos
            // For v1, we'll set up the model matrix for each arrow
            val modelMatrix = FloatArray(16)
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, arPos[0], arPos[1], arPos[2])

            // Scale based on arrow type
            val scale = when (arrow.type) {
                ArrowRenderType.FOLLOW -> 0.08f
                ArrowRenderType.TURN_LEFT, ArrowRenderType.TURN_RIGHT -> 0.12f
                ArrowRenderType.U_TURN -> 0.15f
                ArrowRenderType.DESTINATION -> 0.15f
            }
            Matrix.scaleM(modelMatrix, 0, scale, scale, scale)

            // TODO: Actually render geometry here
            // This requires a shader program and vertex buffers
            // For now, the arrows are "virtual" — they exist in the data model
            // and a proper renderer (Sceneform/Filament) would display them
        }
    }

    /**
     * Transform building-local point to AR world coordinates.
     */
    fun transformToAR(buildingX: Double, buildingY: Double, buildingZ: Double): FloatArray {
        val cosR = cos(rotationYRad)
        val sinR = sin(rotationYRad)

        val rotatedX = buildingX * cosR + buildingZ * sinR
        val rotatedZ = -buildingX * sinR + buildingZ * cosR

        return floatArrayOf(
            (rotatedX + offsetX).toFloat(),
            (buildingY + offsetY).toFloat(),
            (rotatedZ + offsetZ).toFloat(),
        )
    }

    /**
     * Get AR-world positions of all arrows for debug display.
     */
    fun getArrowWorldPositions(): List<FloatArray> {
        return currentArrows.map {
            transformToAR(it.positionX, it.positionY, it.positionZ)
        }
    }
}

/**
 * Arrow data for rendering (mirrors shared ArrowPlacement).
 */
data class ArrowRenderData(
    val id: String,
    val positionX: Double,
    val positionY: Double,
    val positionZ: Double,
    val forwardDx: Double,
    val forwardDy: Double,
    val forwardDz: Double,
    val type: ArrowRenderType,
    val label: String?,
)

enum class ArrowRenderType {
    FOLLOW, TURN_LEFT, TURN_RIGHT, U_TURN, DESTINATION,
}
