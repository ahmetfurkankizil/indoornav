package com.example.vecturai.graph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PathfinderTest {
    @Test
    fun shortestPathPrefersLowestDistanceRoute() {
        val graph = MapGraph(
            buildingName = "test",
            createdAtEpochMs = 0L,
            nodes = listOf(
                node("a"),
                node("b"),
                node("c"),
                node("d")
            ),
            edges = listOf(
                MapEdge("a", "b", 5f),
                MapEdge("a", "c", 1f),
                MapEdge("c", "d", 1f),
                MapEdge("d", "b", 1f)
            )
        )

        val path = Pathfinder(graph).shortestPath("a", "b")

        assertEquals(listOf("a", "c", "d", "b"), path?.map { it.id })
    }

    @Test
    fun shortestPathReturnsNullWhenDisconnected() {
        val graph = MapGraph(
            buildingName = "test",
            createdAtEpochMs = 0L,
            nodes = listOf(node("a"), node("b")),
            edges = emptyList()
        )

        assertNull(Pathfinder(graph).shortestPath("a", "b"))
    }

    @Test
    fun shortestPathHonorsOneWayEdges() {
        val graph = MapGraph(
            buildingName = "test",
            createdAtEpochMs = 0L,
            nodes = listOf(node("a"), node("b")),
            edges = listOf(
                MapEdge("a", "b", 1f, bidirectional = false)
            )
        )

        assertEquals(listOf("a", "b"), Pathfinder(graph).shortestPath("a", "b")?.map { it.id })
        assertNull(Pathfinder(graph).shortestPath("b", "a"))
    }

    @Test
    fun shortestPathPenalizesStairs() {
        val graph = MapGraph(
            buildingName = "test",
            createdAtEpochMs = 0L,
            nodes = listOf(
                node("a", x = 0f, z = 0f),
                node("b", label = "landing", x = 1f, z = 0.1f),
                node("c", x = 2f, z = 0f)
            ),
            edges = listOf(
                MapEdge("a", "c", 2f, kind = EdgeKind.STAIRS),
                MapEdge("a", "b", 1.1f),
                MapEdge("b", "c", 1.1f)
            )
        )

        assertEquals(listOf("a", "b", "c"), Pathfinder(graph).shortestPath("a", "c")?.map { it.id })
    }

    @Test
    fun smoothingKeepsCornerWithoutDirectEdge() {
        val graph = MapGraph(
            buildingName = "test",
            createdAtEpochMs = 0L,
            nodes = listOf(
                node("a", x = 0f, z = 0f),
                node("b", x = 1f, z = 0.2f),
                node("c", x = 2f, z = 0f),
                node("d", x = 3f, z = 0.2f)
            ),
            edges = listOf(
                MapEdge("a", "b", 1f),
                MapEdge("b", "c", 1f),
                MapEdge("c", "d", 1f)
            )
        )

        assertEquals(listOf("a", "b", "c", "d"), Pathfinder(graph).shortestPath("a", "d")?.map { it.id })
    }

    @Test
    fun directTriangleEdgeWinsWhenShorter() {
        val graph = MapGraph(
            buildingName = "test",
            createdAtEpochMs = 0L,
            nodes = listOf(
                node("a", x = 0f, z = 0f),
                node("b", x = 1f, z = 1f),
                node("c", x = 2f, z = 0f)
            ),
            edges = listOf(
                MapEdge("a", "b", 1.2f),
                MapEdge("b", "c", 1.2f),
                MapEdge("a", "c", 1.8f)
            )
        )

        assertEquals(listOf("a", "c"), Pathfinder(graph).shortestPath("a", "c")?.map { it.id })
    }

    @Test
    fun missingDirectEdgePreservesIntermediateCorner() {
        val graph = MapGraph(
            buildingName = "test",
            createdAtEpochMs = 0L,
            nodes = listOf(
                node("a", x = 0f, z = 0f),
                node("b", x = 1f, z = 1f),
                node("c", x = 2f, z = 0f)
            ),
            edges = listOf(
                MapEdge("a", "b", 1f),
                MapEdge("b", "c", 1f)
            )
        )

        assertEquals(listOf("a", "b", "c"), Pathfinder(graph).shortestPath("a", "c")?.map { it.id })
    }

    private fun node(
        id: String,
        label: String? = null,
        x: Float = 0f,
        y: Float = 0f,
        z: Float = 0f
    ): MapNode =
        MapNode(
            id = id,
            cloudAnchorId = "cloud-$id",
            label = label,
            xMeters = x,
            yMeters = y,
            zMeters = z
        )
}
