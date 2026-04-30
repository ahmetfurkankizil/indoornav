package com.vecturai.android.ar

import android.opengl.Matrix
import com.vecturai.android.data.ArrowPlacementData
import com.vecturai.android.data.ArrowPlacementType
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class ArRouteRenderer {
    enum class ArrowState {
        HIDDEN,
        ACTIVE,
        FADING,
    }

    data class VisibleArrow(
        val arrow: ArrowPlacementData,
        val worldX: Float,
        val worldY: Float,
        val worldZ: Float,
        val alpha: Float,
        val scale: Float,
    )

    data class ProjectedArrow(
        val id: String,
        val type: ArrowPlacementType,
        val xPx: Float,
        val yPx: Float,
        val alpha: Float,
        val scale: Float,
    )

    data class RenderableArrow3D(
        val arrowId: String,
        val type: ArrowPlacementType,
        val worldX: Float,
        val worldY: Float,
        val worldZ: Float,
        val floorWorldY: Float,
        val headingRad: Float,
        val alpha: Float,
        val scale: Float,
        val isImminent: Boolean,
    )

    private var offsetX: Double = 0.0
    private var offsetY: Double = 0.0
    private var offsetZ: Double = 0.0
    private var rotationYRad: Double = 0.0

    private var lookaheadDistance = 8.0
    private var fadeDistance = 3.0
    private var arrowHeightOffset = 0.05
    private var allArrows: List<ArrowPlacementData> = emptyList()
    private var arrowStates: Map<String, ArrowState> = emptyMap()
    private var visibleArrows: List<VisibleArrow> = emptyList()
    private val snapshotRef = AtomicReference<List<RenderableArrow3D>>(emptyList())

    val renderedArrowCount: Int get() = visibleArrows.size
    fun snapshot(): List<RenderableArrow3D> = snapshotRef.get()

    fun configureRendering(
        lookaheadDistanceMeters: Double,
        fadeDistanceMeters: Double = 3.0,
        arrowHeightOffsetMeters: Double = 0.05,
    ) {
        lookaheadDistance = lookaheadDistanceMeters
        fadeDistance = fadeDistanceMeters
        arrowHeightOffset = arrowHeightOffsetMeters
    }

    fun setAlignmentTransform(
        offsetX: Double,
        offsetY: Double,
        offsetZ: Double,
        rotationYDeg: Double,
    ) {
        this.offsetX = offsetX
        this.offsetY = offsetY
        this.offsetZ = offsetZ
        rotationYRad = Math.toRadians(rotationYDeg)
    }

    fun placeAllArrows(arrows: List<ArrowPlacementData>) {
        allArrows = arrows
        arrowStates = arrows.associate { it.id to ArrowState.HIDDEN }
        visibleArrows = emptyList()
        snapshotRef.set(emptyList())
    }

    fun updateArrows(arrows: List<ArrowPlacementData>) {
        placeAllArrows(arrows)
    }

    fun updateVisibility(userCumulativeDistance: Double): List<VisibleArrow> {
        val aheadLimit = userCumulativeDistance + lookaheadDistance
        val behindLimit = userCumulativeDistance - fadeDistance
        val nextStates = mutableMapOf<String, ArrowState>()
        val visible = mutableListOf<VisibleArrow>()

        for (arrow in allArrows) {
            val distance = arrow.cumulativeDistance
            val state = when {
                distance >= userCumulativeDistance && distance <= aheadLimit -> ArrowState.ACTIVE
                distance >= behindLimit && distance < userCumulativeDistance -> ArrowState.FADING
                else -> ArrowState.HIDDEN
            }
            nextStates[arrow.id] = state

            if (state == ArrowState.HIDDEN) continue
            val fadeT = if (state == ArrowState.FADING) {
                ((userCumulativeDistance - distance) / fadeDistance).coerceIn(0.0, 1.0)
            } else {
                0.0
            }
            val alpha = (1.0 - fadeT * 0.8).toFloat()
            val scale = (1.0 - fadeT * 0.3).toFloat()
            val pos = transformToAR(arrow.positionX, arrow.positionY, arrow.positionZ)
            visible.add(
                VisibleArrow(
                    arrow = arrow,
                    worldX = pos[0],
                    worldY = pos[1],
                    worldZ = pos[2],
                    alpha = alpha,
                    scale = scale,
                )
            )
        }

        arrowStates = nextStates
        visibleArrows = visible
        snapshotRef.set(buildRenderableSnapshot(visible, userCumulativeDistance))
        return visibleArrows
    }

    fun hideAllArrows() {
        arrowStates = allArrows.associate { it.id to ArrowState.HIDDEN }
        visibleArrows = emptyList()
        snapshotRef.set(emptyList())
    }

    fun clearArrows() {
        allArrows = emptyList()
        arrowStates = emptyMap()
        visibleArrows = emptyList()
        snapshotRef.set(emptyList())
    }

    private fun buildRenderableSnapshot(
        visible: List<VisibleArrow>,
        userCumulativeDistance: Double,
    ): List<RenderableArrow3D> {
        if (visible.isEmpty()) return emptyList()

        val imminentArrowId = allArrows
            .firstOrNull {
                it.cumulativeDistance >= userCumulativeDistance &&
                    it.type != ArrowPlacementType.FOLLOW
            }
            ?.id
            ?: allArrows.firstOrNull { it.cumulativeDistance >= userCumulativeDistance }?.id

        return visible.map { v ->
            val arForward = transformDirectionToAR(v.arrow.forwardDx, v.arrow.forwardDy, v.arrow.forwardDz)
            val headingRad = atan2(arForward[0], arForward[2])
            val floor = transformToAR(
                v.arrow.positionX,
                v.arrow.positionY - arrowHeightOffset,
                v.arrow.positionZ,
            )
            RenderableArrow3D(
                arrowId = v.arrow.id,
                type = v.arrow.type,
                worldX = v.worldX,
                worldY = v.worldY,
                worldZ = v.worldZ,
                floorWorldY = floor[1],
                headingRad = headingRad,
                alpha = v.alpha,
                scale = v.scale,
                isImminent = v.arrow.id == imminentArrowId,
            )
        }
    }

    fun projectVisibleArrows(
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        width: Int,
        height: Int,
    ): List<ProjectedArrow> {
        if (width <= 0 || height <= 0 || visibleArrows.isEmpty()) return emptyList()

        val viewProjection = FloatArray(16)
        Matrix.multiplyMM(viewProjection, 0, projectionMatrix, 0, viewMatrix, 0)

        return visibleArrows.mapNotNull { visible ->
            val clip = FloatArray(4)
            Matrix.multiplyMV(
                clip,
                0,
                viewProjection,
                0,
                floatArrayOf(visible.worldX, visible.worldY, visible.worldZ, 1f),
                0,
            )
            if (clip[3] <= 0.0001f) return@mapNotNull null

            val ndcX = clip[0] / clip[3]
            val ndcY = clip[1] / clip[3]
            if (ndcX !in -1.2f..1.2f || ndcY !in -1.2f..1.2f) return@mapNotNull null

            ProjectedArrow(
                id = visible.arrow.id,
                type = visible.arrow.type,
                xPx = (ndcX + 1f) * 0.5f * width,
                yPx = (1f - ndcY) * 0.5f * height,
                alpha = visible.alpha,
                scale = visible.scale,
            )
        }
    }

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

    fun transformDirectionToAR(dx: Double, dy: Double, dz: Double): FloatArray {
        val cosR = cos(rotationYRad)
        val sinR = sin(rotationYRad)
        return floatArrayOf(
            (dx * cosR + dz * sinR).toFloat(),
            dy.toFloat(),
            (-dx * sinR + dz * cosR).toFloat(),
        )
    }
}
