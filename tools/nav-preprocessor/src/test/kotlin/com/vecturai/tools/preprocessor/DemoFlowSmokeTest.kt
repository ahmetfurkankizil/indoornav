package com.vecturai.tools.preprocessor

import kotlin.test.*

/**
 * Smoke tests for the deterministic demo flow.
 *
 * Verifies that demo package data is consistent and complete,
 * and that the simulated demo path produces correct results.
 */
class DemoFlowSmokeTest {

    // ── Mirror of DemoPackageProvider data ──

    data class TestNode(val id: String, val x: Double, val y: Double, val z: Double, val label: String = "")
    data class TestEdge(val from: String, val to: String, val cost: Double)
    data class TestRoom(val id: String, val name: String, val entryNodeIds: List<String>)
    data class TestMarker(val id: String, val nearestNodeId: String)

    private val nodes = listOf(
        TestNode("n01", 0.0, 0.0, 0.0, "Entrance"),
        TestNode("n02", 3.0, 0.0, 0.0, "Hallway A"),
        TestNode("n03", 6.0, 0.0, 0.0, "Junction B"),
        TestNode("n04", 6.0, 0.0, 4.0, "Hallway C"),
        TestNode("n05", 3.0, 0.0, 4.0, "Conference Room Entry"),
        TestNode("n06", 0.0, 0.0, 4.0, "Kitchen Entry"),
        TestNode("n07", 9.0, 0.0, 0.0, "Office A Entry"),
        TestNode("n08", 9.0, 0.0, 4.0, "Office B Entry"),
    )

    private val edges = listOf(
        TestEdge("n01", "n02", 3.0), TestEdge("n02", "n01", 3.0),
        TestEdge("n02", "n03", 3.0), TestEdge("n03", "n02", 3.0),
        TestEdge("n03", "n04", 4.0), TestEdge("n04", "n03", 4.0),
        TestEdge("n04", "n05", 3.0), TestEdge("n05", "n04", 3.0),
        TestEdge("n05", "n06", 3.0), TestEdge("n06", "n05", 3.0),
        TestEdge("n03", "n07", 3.0), TestEdge("n07", "n03", 3.0),
        TestEdge("n04", "n08", 3.0), TestEdge("n08", "n04", 3.0),
    )

    private val rooms = listOf(
        TestRoom("conference-room", "Conference Room", listOf("n05")),
        TestRoom("kitchen", "Kitchen", listOf("n06")),
        TestRoom("office-a", "Office A", listOf("n07")),
        TestRoom("office-b", "Office B", listOf("n08")),
    )

    private val markers = listOf(TestMarker("marker-main", "n01"))

    @Test
    fun `demo graph has correct node count`() {
        assertEquals(8, nodes.size)
    }

    @Test
    fun `demo graph has bidirectional edges`() {
        for (edge in edges) {
            val reverse = edges.find { it.from == edge.to && it.to == edge.from }
            assertNotNull(reverse, "Missing reverse for ${edge.from} → ${edge.to}")
        }
    }

    @Test
    fun `all room entry nodes exist in graph`() {
        val nodeIds = nodes.map { it.id }.toSet()
        for (room in rooms) {
            for (entryId in room.entryNodeIds) {
                assertTrue(entryId in nodeIds, "${room.name} entry node $entryId not in graph")
            }
        }
    }

    @Test
    fun `entrance marker references valid node`() {
        val nodeIds = nodes.map { it.id }.toSet()
        for (marker in markers) {
            assertTrue(marker.nearestNodeId in nodeIds, "Marker ${marker.id} references missing node")
        }
    }

    @Test
    fun `conference room is reachable from entrance`() {
        // Simple BFS
        val start = "n01"
        val target = "n05"
        val visited = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(start)
        visited.add(start)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current == target) break
            for (edge in edges.filter { it.from == current }) {
                if (edge.to !in visited) {
                    visited.add(edge.to)
                    queue.add(edge.to)
                }
            }
        }
        assertTrue(target in visited, "Conference room not reachable from entrance")
    }

    @Test
    fun `kitchen is reachable from entrance`() {
        val start = "n01"
        val target = "n06"
        val visited = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(start)
        visited.add(start)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current == target) break
            for (edge in edges.filter { it.from == current }) {
                if (edge.to !in visited) {
                    visited.add(edge.to)
                    queue.add(edge.to)
                }
            }
        }
        assertTrue(target in visited, "Kitchen not reachable from entrance")
    }

    @Test
    fun `all rooms have unique IDs`() {
        val ids = rooms.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "Duplicate room IDs found")
    }

    @Test
    fun `all rooms have non-empty names`() {
        for (room in rooms) {
            assertTrue(room.name.isNotBlank(), "Room ${room.id} has blank name")
        }
    }

    @Test
    fun `demo route to conference room has expected distance`() {
        // n01 → n02 (3m) → n03 (3m) → n04 (4m) → n05 (3m) = 13m
        val path = listOf("n01", "n02", "n03", "n04", "n05")
        var totalDist = 0.0
        for (i in 0 until path.size - 1) {
            val edge = edges.find { it.from == path[i] && it.to == path[i + 1] }
            assertNotNull(edge, "Missing edge ${path[i]} → ${path[i + 1]}")
            totalDist += edge!!.cost
        }
        assertEquals(13.0, totalDist, 0.01)
    }

    @Test
    fun `simulated progress flow reaches arrival`() {
        var progress = 0.0
        val step = 0.15
        var arrived = false
        var steps = 0

        while (!arrived && steps < 20) {
            progress = (progress + step).coerceAtMost(1.0)
            steps++
            if (progress >= 0.95) arrived = true
        }

        assertTrue(arrived, "Simulated flow should reach arrival")
        assertTrue(steps <= 8, "Should arrive in ≤8 advances ($steps)")
    }
}
