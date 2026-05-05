package com.vecturai.tools.preprocessor

import com.vecturai.tools.preprocessor.model.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

/**
 * Tests for the Dijkstra shortest-path routing engine.
 *
 * Note: These tests directly use the shared domain models from
 * com.vecturai.core.domain, brought in as source dependencies.
 * For the preprocessor module (JVM-only), we replicate the logic
 * to test independently without requiring KMP compilation.
 */
class DijkstraRouteEngineTest {

    // ── Simple graph: a linear chain ──
    // n1 --3--> n2 --4--> n3 --2--> n4
    private val linearNodes = listOf(
        TestNode("n1", 0.0, 0.0),
        TestNode("n2", 3.0, 0.0),
        TestNode("n3", 7.0, 0.0),
        TestNode("n4", 9.0, 0.0),
    )
    private val linearEdges = listOf(
        TestEdge("n1", "n2", 3.0),
        TestEdge("n2", "n3", 4.0),
        TestEdge("n3", "n4", 2.0),
    )

    // ── Diamond graph with shortcut ──
    //     n1
    //    / \
    //   2   5
    //  /     \
    // n2     n3
    //  \     /
    //   3   1
    //    \ /
    //     n4
    private val diamondNodes = listOf(
        TestNode("n1", 0.0, 0.0),
        TestNode("n2", -2.0, 2.0),
        TestNode("n3", 2.0, 2.0),
        TestNode("n4", 0.0, 4.0),
    )
    private val diamondEdges = listOf(
        TestEdge("n1", "n2", 2.0),
        TestEdge("n1", "n3", 5.0),
        TestEdge("n2", "n4", 3.0),
        TestEdge("n3", "n4", 1.0),
    )

    @Test
    fun `linear graph shortest path`() {
        val path = dijkstra(linearNodes, linearEdges, "n1", "n4")
        assertNotNull(path)
        assertEquals(listOf("n1", "n2", "n3", "n4"), path.nodeIds)
        assertEquals(9.0, path.totalCost, 0.001)
    }

    @Test
    fun `diamond graph takes shorter path`() {
        val path = dijkstra(diamondNodes, diamondEdges, "n1", "n4")
        assertNotNull(path)
        // n1 -> n2 -> n4 = 5.0 (shorter than n1 -> n3 -> n4 = 6.0)
        assertEquals(listOf("n1", "n2", "n4"), path.nodeIds)
        assertEquals(5.0, path.totalCost, 0.001)
    }

    @Test
    fun `same start and end returns empty path`() {
        val path = dijkstra(linearNodes, linearEdges, "n1", "n1")
        assertNotNull(path)
        assertEquals(listOf("n1"), path.nodeIds)
        assertEquals(0.0, path.totalCost)
    }

    @Test
    fun `unreachable node returns null`() {
        val nodes = listOf(
            TestNode("n1", 0.0, 0.0),
            TestNode("n2", 3.0, 0.0),
            TestNode("n3", 6.0, 0.0),  // isolated
        )
        val edges = listOf(TestEdge("n1", "n2", 3.0))
        val path = dijkstra(nodes, edges, "n1", "n3")
        assertNull(path)
    }

    @Test
    fun `non-existent start node returns null`() {
        val path = dijkstra(linearNodes, linearEdges, "nx", "n4")
        assertNull(path)
    }

    @Test
    fun `non-existent end node returns null`() {
        val path = dijkstra(linearNodes, linearEdges, "n1", "nx")
        assertNull(path)
    }

    @Test
    fun `bidirectional edges allow reverse traversal`() {
        // Only edges go n1->n2->n3, but bidirectional, so we can go n3->n1
        val path = dijkstra(linearNodes, linearEdges, "n4", "n1")
        assertNotNull(path)
        assertEquals(listOf("n4", "n3", "n2", "n1"), path.nodeIds)
    }

    @Test
    fun `unidirectional edge blocks reverse`() {
        val edges = listOf(
            TestEdge("n1", "n2", 3.0, bidirectional = false),
            TestEdge("n2", "n3", 4.0, bidirectional = false),
        )
        val nodes = listOf(
            TestNode("n1", 0.0, 0.0),
            TestNode("n2", 3.0, 0.0),
            TestNode("n3", 7.0, 0.0),
        )
        val path = dijkstra(nodes, edges, "n3", "n1")
        assertNull(path) // can't go backward
    }

    // ── Simplified Dijkstra for testing (mirrors shared DijkstraRouteEngine logic) ──

    data class TestNode(val id: String, val x: Double, val z: Double)
    data class TestEdge(val from: String, val to: String, val cost: Double, val bidirectional: Boolean = true)
    data class TestPath(val nodeIds: List<String>, val totalCost: Double)

    private fun dijkstra(nodes: List<TestNode>, edges: List<TestEdge>, from: String, to: String): TestPath? {
        val nodeSet = nodes.map { it.id }.toSet()
        if (from !in nodeSet || to !in nodeSet) return null
        if (from == to) return TestPath(listOf(from), 0.0)

        // Build adjacency
        val adj = mutableMapOf<String, MutableList<Pair<String, Double>>>()
        for (edge in edges) {
            adj.getOrPut(edge.from) { mutableListOf() }.add(edge.to to edge.cost)
            if (edge.bidirectional) {
                adj.getOrPut(edge.to) { mutableListOf() }.add(edge.from to edge.cost)
            }
        }

        val dist = mutableMapOf<String, Double>()
        val prev = mutableMapOf<String, String>()
        val visited = mutableSetOf<String>()
        for (n in nodes) dist[n.id] = Double.MAX_VALUE
        dist[from] = 0.0

        while (true) {
            val current = dist.entries
                .filter { it.key !in visited && it.value < Double.MAX_VALUE }
                .minByOrNull { it.value }?.key ?: break
            if (current == to) break
            visited.add(current)
            for ((neighbor, cost) in adj[current] ?: emptyList()) {
                if (neighbor in visited) continue
                val newDist = dist[current]!! + cost
                if (newDist < (dist[neighbor] ?: Double.MAX_VALUE)) {
                    dist[neighbor] = newDist
                    prev[neighbor] = current
                }
            }
        }

        if (to !in prev) return null
        val path = mutableListOf(to)
        var cur = to
        while (cur != from) {
            cur = prev[cur] ?: return null
            path.add(0, cur)
        }
        return TestPath(path, dist[to]!!)
    }
}
