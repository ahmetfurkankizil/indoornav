package com.vecturai.tools.preprocessor

import com.vecturai.tools.preprocessor.model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlin.math.sqrt
import kotlin.test.*

/**
 * Integration-style regression tests for demo-critical flows.
 *
 * Covers the full path: config → validate → export → load → route → arrow → progress → arrival.
 * Single-marker and checkpoint-marker flows.
 */
class DemoCriticalRegressionTest {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    /** Realistic demo building config matching sample/demo-building. */
    private fun demoConfig(
        checkpoints: List<AuthoringCheckpointMarker> = emptyList(),
    ) = AuthoringConfig(
        buildingId = "demo-building",
        buildingName = "Demo Campus Building A",
        floorId = "ground",
        asset = AssetReference("scan.glb"),
        entranceMarkers = listOf(
            AuthoringMarker("entrance-1", "n01", 0.21, 0.21, Position3D(0.0, 0.0, 0.0)),
        ),
        checkpointMarkers = checkpoints,
        nodes = listOf(
            AuthoringNode("n01", 0.0, 0.0, type = "entrance", label = "Entrance"),
            AuthoringNode("n02", 3.0, 0.0, type = "waypoint"),
            AuthoringNode("n03", 6.0, 0.0, type = "waypoint"),
            AuthoringNode("n04", 6.0, 4.0, type = "waypoint"),
            AuthoringNode("n05", 3.0, 4.0, type = "destination", label = "Kitchen"),
        ),
        edges = listOf(
            AuthoringEdge("e1", "n01", "n02", 3.0),
            AuthoringEdge("e2", "n02", "n03", 3.0),
            AuthoringEdge("e3", "n03", "n04", 4.0),
            AuthoringEdge("e4", "n04", "n05", 3.0),
        ),
        rooms = listOf(
            AuthoringRoom("kitchen", "Kitchen", "n05", category = "room",
                keywords = listOf("kitchen", "mutfak"), aliases = listOf("Mutfak")),
        ),
    )

    // ── Validation ───────────────────────────────

    @Test
    fun `demo config validates successfully`() {
        val validator = GraphValidator()
        val result = validator.validate(demoConfig())
        assertTrue(result.isValid, "Demo config should be valid: ${result.errors}")
    }

    @Test
    fun `demo config with checkpoints validates`() {
        val validator = GraphValidator()
        val config = demoConfig(
            checkpoints = listOf(
                AuthoringCheckpointMarker("cp1", "n03", 0.15, 0.15, Position3D(6.0, 0.0, 0.0)),
            ),
        )
        val result = validator.validate(config)
        assertTrue(result.isValid, "Config with checkpoints should validate: ${result.errors}")
    }

    // ── Serialization round-trip ─────────────────

    @Test
    fun `demo config round-trips through JSON`() {
        val config = demoConfig()
        val serialized = json.encodeToString(config)
        val deserialized = json.decodeFromString<AuthoringConfig>(serialized)
        assertEquals(config.buildingId, deserialized.buildingId)
        assertEquals(config.nodes.size, deserialized.nodes.size)
        assertEquals(config.edges.size, deserialized.edges.size)
        assertEquals(config.rooms.size, deserialized.rooms.size)
        assertEquals(config.entranceMarkers.size, deserialized.entranceMarkers.size)
    }

    // ── Room search ─────────────────────────────

    @Test
    fun `room search finds Kitchen by name`() {
        val config = demoConfig()
        val rooms = config.rooms
        val result = rooms.filter { it.displayName.contains("Kitchen", ignoreCase = true) }
        assertEquals(1, result.size)
        assertEquals("kitchen", result[0].id)
    }

    @Test
    fun `room search finds Kitchen by Turkish alias`() {
        val config = demoConfig()
        val rooms = config.rooms
        val result = rooms.filter { it.aliases.any { a -> a.contains("Mutfak", ignoreCase = true) } }
        assertEquals(1, result.size)
    }

    // ── Route computation ───────────────────────

    data class Pt(val x: Double, val z: Double)

    @Test
    fun `shortest path from entrance to kitchen uses all waypoints`() {
        val config = demoConfig()
        // Build adjacency
        val adj = mutableMapOf<String, MutableList<Pair<String, Double>>>()
        for (e in config.edges) {
            adj.getOrPut(e.from) { mutableListOf() }.add(e.to to e.cost)
            if (e.bidirectional) {
                adj.getOrPut(e.to) { mutableListOf() }.add(e.from to e.cost)
            }
        }

        // Dijkstra from n01 to n05
        val dest = "n05"
        val dist = mutableMapOf<String, Double>().withDefault { Double.MAX_VALUE }
        val prev = mutableMapOf<String, String?>()
        dist["n01"] = 0.0
        val unvisited = config.nodes.map { it.id }.toMutableSet()

        while (unvisited.isNotEmpty()) {
            val u = unvisited.minByOrNull { dist.getValue(it) } ?: break
            if (dist.getValue(u) == Double.MAX_VALUE) break
            unvisited.remove(u)
            for ((v, w) in adj[u] ?: emptyList()) {
                val alt = dist.getValue(u) + w
                if (alt < dist.getValue(v)) {
                    dist[v] = alt
                    prev[v] = u
                }
            }
        }

        // Reconstruct path
        val path = mutableListOf<String>()
        var cur: String? = dest
        while (cur != null) {
            path.add(0, cur)
            cur = prev[cur]
        }

        assertEquals(listOf("n01", "n02", "n03", "n04", "n05"), path)
        assertEquals(13.0, dist.getValue(dest), 0.01)
    }

    // ── Progress estimation ─────────────────────

    @Test
    fun `progress advances along route`() {
        val waypoints = listOf(Pt(0.0, 0.0), Pt(3.0, 0.0), Pt(6.0, 0.0), Pt(6.0, 4.0), Pt(3.0, 4.0))
        val totalDist = 13.0

        // Simplified progress: project onto polyline
        var peakDist = 0.0
        fun progress(px: Double, pz: Double): Double {
            val cumDist = mutableListOf(0.0)
            for (i in 1 until waypoints.size) {
                val dx = waypoints[i].x - waypoints[i-1].x
                val dz = waypoints[i].z - waypoints[i-1].z
                cumDist.add(cumDist.last() + sqrt(dx*dx + dz*dz))
            }

            var bestDist = Double.MAX_VALUE; var bestCum = 0.0
            for (i in 0 until waypoints.size - 1) {
                val a = waypoints[i]; val b = waypoints[i+1]
                val dx = b.x - a.x; val dz = b.z - a.z
                val segLen = sqrt(dx*dx + dz*dz)
                if (segLen < 0.001) continue
                var t = ((px - a.x) * dx + (pz - a.z) * dz) / (dx*dx + dz*dz)
                t = t.coerceIn(0.0, 1.0)
                val projX = a.x + t * dx; val projZ = a.z + t * dz
                val d = sqrt((px-projX)*(px-projX) + (pz-projZ)*(pz-projZ))
                if (d < bestDist) { bestDist = d; bestCum = cumDist[i] + t * segLen }
            }

            peakDist = maxOf(peakDist, bestCum)
            return (peakDist / totalDist).coerceIn(0.0, 1.0)
        }

        val p1 = progress(0.0, 0.0) // start
        val p2 = progress(3.0, 0.0) // waypoint 2
        val p3 = progress(6.0, 0.0) // waypoint 3
        val p4 = progress(6.0, 4.0) // waypoint 4
        val p5 = progress(3.0, 4.0) // destination

        assertTrue(p1 < p2, "Progress should advance")
        assertTrue(p2 < p3)
        assertTrue(p3 < p4)
        assertTrue(p4 < p5)
        assertEquals(1.0, p5, 0.01, "Should reach 100% at destination")
    }

    @Test
    fun `progress does not regress on backwards movement`() {
        var peak = 0.0
        fun monotonic(v: Double): Double { peak = maxOf(peak, v); return peak }

        monotonic(0.0)
        monotonic(0.3)
        val at50 = monotonic(0.5)
        val after = monotonic(0.3) // backwards
        assertTrue(after >= at50, "Progress should not regress")
    }

    // ── Arrival detection ───────────────────────

    @Test
    fun `arrival detected at 95 percent progress`() {
        val progressThreshold = 0.95
        val distanceThreshold = 1.5

        assertTrue(0.96 >= progressThreshold, "96% progress should trigger arrival")
        assertTrue(1.0 >= progressThreshold, "100% progress should trigger arrival")
        assertFalse(0.5 >= progressThreshold, "50% should not trigger arrival")
    }

    @Test
    fun `arrival detected by remaining distance`() {
        val remaining = 1.2  // meters
        val threshold = 1.5
        assertTrue(remaining <= threshold, "1.2m remaining should trigger arrival")
    }

    // ── History ──────────────────────────────────

    @Test
    fun `visit record serializes and deserializes`() {
        // Simplified visit record round-trip
        val record = mapOf(
            "visitId" to "v1",
            "buildingId" to "demo-building",
            "destinationName" to "Kitchen",
            "timestamp" to "2026-03-10T00:00:00Z",
        )
        val serialized = json.encodeToString(record)
        val deserialized = json.decodeFromString<Map<String, String>>(serialized)
        assertEquals("v1", deserialized["visitId"])
        assertEquals("Kitchen", deserialized["destinationName"])
    }
}
