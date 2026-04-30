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

    private fun node(id: String): MapNode =
        MapNode(
            id = id,
            cloudAnchorId = "cloud-$id",
            xMeters = 0f,
            yMeters = 0f,
            zMeters = 0f
        )
}
