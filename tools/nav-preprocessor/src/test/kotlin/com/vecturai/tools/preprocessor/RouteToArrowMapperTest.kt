package com.Vectura AI.tools.preprocessor

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.test.*

/**
 * Tests for the route-to-arrow placement mapper.
 *
 * Replicates the RouteToArrowMapper logic to verify arrow generation
 * independently of the KMP module compilation.
 */
class RouteToArrowMapperTest {

    // ── Test arrow mapper (mirrors shared RouteToArrowMapper) ──

    data class TestNode(val id: String, val x: Double, val y: Double, val z: Double)

    data class TestRenderConfig(
        val arrowSpacingMeters: Double = 1.5,
        val turnMarkerThresholdDegrees: Double = 30.0,
        val arrowHeightOffsetMeters: Double = 0.05,
    )

    enum class TestArrowType { FOLLOW, TURN_LEFT, TURN_RIGHT, U_TURN, DESTINATION }

    data class TestArrow(
        val id: String,
        val x: Double,
        val y: Double,
        val z: Double,
        val type: TestArrowType,
        val label: String? = null,
    )

    private fun mapRoute(pathNodes: List<TestNode>, config: TestRenderConfig = TestRenderConfig()): List<TestArrow> {
        if (pathNodes.size < 2) return emptyList()

        val arrows = mutableListOf<TestArrow>()
        var idx = 0

        for (i in 0 until pathNodes.size - 1) {
            val from = pathNodes[i]
            val to = pathNodes[i + 1]
            val dx = to.x - from.x
            val dz = to.z - from.z
            val edgeLen = sqrt(dx * dx + dz * dz)
            if (edgeLen < 0.001) continue

            // Check turn at this junction
            if (i > 0) {
                val prev = pathNodes[i - 1]
                val turn = computeTurnAngle(prev.x, prev.z, from.x, from.z, to.x, to.z)
                if (abs(turn) >= config.turnMarkerThresholdDegrees) {
                    val type = when {
                        turn > 135 -> TestArrowType.U_TURN
                        turn > 0 -> TestArrowType.TURN_RIGHT
                        turn < -135 -> TestArrowType.U_TURN
                        else -> TestArrowType.TURN_LEFT
                    }
                    arrows.add(TestArrow("a${idx++}", from.x, from.y + config.arrowHeightOffsetMeters, from.z, type))
                }
            }

            // Interpolate follow arrows
            var walked = 0.0
            while (walked < edgeLen) {
                val t = walked / edgeLen
                arrows.add(TestArrow(
                    "a${idx++}",
                    from.x + dx * t,
                    from.y + config.arrowHeightOffsetMeters,
                    from.z + dz * t,
                    TestArrowType.FOLLOW,
                ))
                walked += config.arrowSpacingMeters
            }
        }

        // Destination marker
        val last = pathNodes.last()
        arrows.add(TestArrow("dest", last.x, last.y + config.arrowHeightOffsetMeters, last.z, TestArrowType.DESTINATION))
        return arrows
    }

    private fun computeTurnAngle(
        prevX: Double, prevZ: Double,
        curX: Double, curZ: Double,
        nextX: Double, nextZ: Double,
    ): Double {
        val inAngle = kotlin.math.atan2(curZ - prevZ, curX - prevX)
        val outAngle = kotlin.math.atan2(nextZ - curZ, nextX - curX)
        var turn = Math.toDegrees(outAngle - inAngle)
        if (turn > 180) turn -= 360
        if (turn < -180) turn += 360
        return turn
    }

    // ── Tests ───────────────────────────────────────

    @Test
    fun `straight corridor produces evenly spaced arrows`() {
        val nodes = listOf(
            TestNode("a", 0.0, 0.0, 0.0),
            TestNode("b", 6.0, 0.0, 0.0),
        )
        val arrows = mapRoute(nodes, TestRenderConfig(arrowSpacingMeters = 1.5))
        
        // Follow arrows: at 0, 1.5, 3.0, 4.5 = 4 arrows + 1 destination
        val followArrows = arrows.filter { it.type == TestArrowType.FOLLOW }
        assertEquals(4, followArrows.size)
        
        val destArrows = arrows.filter { it.type == TestArrowType.DESTINATION }
        assertEquals(1, destArrows.size)
        assertEquals(6.0, destArrows.first().x, 0.001)
    }

    @Test
    fun `90-degree right turn produces turn marker`() {
        val nodes = listOf(
            TestNode("a", 0.0, 0.0, 0.0),
            TestNode("b", 3.0, 0.0, 0.0),
            TestNode("c", 3.0, 0.0, 3.0),
        )
        val arrows = mapRoute(nodes)
        val turnArrows = arrows.filter { it.type == TestArrowType.TURN_RIGHT || it.type == TestArrowType.TURN_LEFT }
        assertTrue(turnArrows.isNotEmpty(), "Should have a turn marker")
        // Turn should be at node b (3,0,0)
        assertEquals(3.0, turnArrows.first().x, 0.001)
    }

    @Test
    fun `90-degree left turn produces turn marker`() {
        val nodes = listOf(
            TestNode("a", 0.0, 0.0, 0.0),
            TestNode("b", 3.0, 0.0, 0.0),
            TestNode("c", 3.0, 0.0, -3.0),
        )
        val arrows = mapRoute(nodes)
        val turnArrows = arrows.filter { it.type == TestArrowType.TURN_LEFT || it.type == TestArrowType.TURN_RIGHT }
        assertTrue(turnArrows.isNotEmpty(), "Should have a turn marker")
    }

    @Test
    fun `multiple turns produce multiple markers`() {
        val nodes = listOf(
            TestNode("a", 0.0, 0.0, 0.0),
            TestNode("b", 3.0, 0.0, 0.0),
            TestNode("c", 3.0, 0.0, 3.0),
            TestNode("d", 0.0, 0.0, 3.0),
        )
        val arrows = mapRoute(nodes)
        val turnArrows = arrows.filter { it.type != TestArrowType.FOLLOW && it.type != TestArrowType.DESTINATION }
        assertEquals(2, turnArrows.size, "Should have 2 turn markers")
    }

    @Test
    fun `very short route still produces destination`() {
        val nodes = listOf(
            TestNode("a", 0.0, 0.0, 0.0),
            TestNode("b", 0.5, 0.0, 0.0),
        )
        val arrows = mapRoute(nodes, TestRenderConfig(arrowSpacingMeters = 1.5))
        // Edge is 0.5m, spacing is 1.5m, so 1 follow arrow at start + 1 dest
        val follows = arrows.filter { it.type == TestArrowType.FOLLOW }
        assertEquals(1, follows.size) // at position 0.0
        assertTrue(arrows.any { it.type == TestArrowType.DESTINATION })
    }

    @Test
    fun `empty path produces no arrows`() {
        val arrows = mapRoute(emptyList())
        assertTrue(arrows.isEmpty())
    }

    @Test
    fun `single node path produces no arrows`() {
        val arrows = mapRoute(listOf(TestNode("a", 0.0, 0.0, 0.0)))
        assertTrue(arrows.isEmpty())
    }

    @Test
    fun `arrows have correct Y offset`() {
        val nodes = listOf(
            TestNode("a", 0.0, 0.0, 0.0),
            TestNode("b", 3.0, 0.0, 0.0),
        )
        val config = TestRenderConfig(arrowHeightOffsetMeters = 0.1)
        val arrows = mapRoute(nodes, config)
        assertTrue(arrows.all { it.y == 0.1 }, "All arrows should have Y offset 0.1")
    }

    @Test
    fun `U-turn detected for 180-degree turn`() {
        val nodes = listOf(
            TestNode("a", 0.0, 0.0, 0.0),
            TestNode("b", 3.0, 0.0, 0.0),
            TestNode("c", 0.0, 0.0, 0.0),  // going back
        )
        val arrows = mapRoute(nodes)
        val uTurns = arrows.filter { it.type == TestArrowType.U_TURN }
        assertTrue(uTurns.isNotEmpty(), "Should detect U-turn")
    }
}
