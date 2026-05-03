package com.Vectura AI.tools.admin

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import java.io.File
import kotlin.test.*

class RoomEditAndExportRoutesTest {

    private val testDirs = mutableListOf<File>()

    @AfterTest
    fun cleanup() {
        testDirs.forEach { it.deleteRecursively() }
        testDirs.clear()
    }

    private fun testApp(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        val dir = File("build/test-edit-export-${System.nanoTime()}")
        dir.mkdirs()
        testDirs.add(dir)
        application { configureApp(dir.absolutePath) }
        block()
    }

    /** Seeds a succeeded job with a config containing the given rooms. */
    private fun seedJob(
        jobsDir: File,
        jobId: String,
        rooms: List<Triple<String, String, String>> = listOf(Triple("zone-a", "Zone A", "n01")),
    ) {
        val jobDir = File(jobsDir, jobId)
        val outputDir = File(jobDir, "output")
        outputDir.mkdirs()

        val roomsJson = rooms.joinToString(",\n") { (id, name, node) ->
            """{"id":"$id","displayName":"$name","destinationNodeId":"$node","category":"unknown","description":"auto"}"""
        }
        File(outputDir, "authoring_config.generated.json").writeText("""
        {
            "buildingId":"draft-x","buildingName":"Test","floorId":"ground",
            "nodes":[{"id":"n01","x":0,"y":0,"z":0,"type":"junction","label":"A"}],
            "edges":[{"id":"e01","from":"n01","to":"n01","cost":1,"bidirectional":true}],
            "rooms":[$roomsJson],
            "entranceMarkers":[],"checkpointMarkers":[],
            "routeRendering":{"arrowSpacingMeters":1.5,"lookaheadDistanceMeters":8.0,"destinationThresholdMeters":1.5,"turnMarkerThresholdDegrees":30,"arrowHeightOffsetMeters":0.05}
        }
        """.trimIndent())

        File(jobDir, "job.json").writeText("""
        {"id":"$jobId","originalFilename":"test.glb","createdAt":"2026-03-30T10:00:00Z",
         "updatedAt":"2026-03-30T10:01:00Z","status":"succeeded","errorMessage":null,
         "artifacts":["authoring_config.generated.json"]}
        """.trimIndent())
    }

    // ── PATCH room tests ─────────────────────────────────

    @Test
    fun `PATCH room returns 404 for nonexistent job`() = testApp {
        val r = client.patch("/admin/draft-jobs/nope/rooms/zone-a") {
            contentType(ContentType.Application.Json)
            setBody("""{"displayName":"New"}""")
        }
        assertEquals(HttpStatusCode.NotFound, r.status)
    }

    @Test
    fun `PATCH room returns 400 for unknown room id`() = testApp {
        val dir = testDirs.last()
        seedJob(dir, "job001")
        val r = client.patch("/admin/draft-jobs/job001/rooms/zone-zzz") {
            contentType(ContentType.Application.Json)
            setBody("""{"displayName":"New"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, r.status)
        assertTrue(r.bodyAsText().contains("zone-zzz"))
    }

    @Test
    fun `PATCH room returns 400 for empty displayName`() = testApp {
        val dir = testDirs.last()
        seedJob(dir, "job001")
        val r = client.patch("/admin/draft-jobs/job001/rooms/zone-a") {
            contentType(ContentType.Application.Json)
            setBody("""{"displayName":"  "}""")
        }
        assertEquals(HttpStatusCode.BadRequest, r.status)
    }

    @Test
    fun `PATCH room updates displayName successfully`() = testApp {
        val dir = testDirs.last()
        seedJob(dir, "job001")
        val r = client.patch("/admin/draft-jobs/job001/rooms/zone-a") {
            contentType(ContentType.Application.Json)
            setBody("""{"displayName":"Kitchen","category":"kitchen"}""")
        }
        assertEquals(HttpStatusCode.OK, r.status)
        val body = Json.parseToJsonElement(r.bodyAsText()).jsonObject
        assertEquals("\"Kitchen\"", body["displayName"].toString())
        assertEquals("\"kitchen\"", body["category"].toString())
    }

    @Test
    fun `PATCH room is reflected in summary`() = testApp {
        val dir = testDirs.last()
        seedJob(dir, "job001")

        client.patch("/admin/draft-jobs/job001/rooms/zone-a") {
            contentType(ContentType.Application.Json)
            setBody("""{"displayName":"Living Room"}""")
        }

        val summary = Json.parseToJsonElement(
            client.get("/admin/draft-jobs/job001/summary").bodyAsText()
        ).jsonObject

        val rooms = summary["rooms"]!!.jsonArray
        assertEquals("\"Living Room\"", rooms[0].jsonObject["displayName"].toString())
    }

    // ── Export tests ─────────────────────────────────────

    @Test
    fun `POST export returns 404 for nonexistent job`() = testApp {
        val r = client.post("/admin/draft-jobs/nope/export-reviewed-package")
        assertEquals(HttpStatusCode.NotFound, r.status)
    }

    @Test
    fun `POST export succeeds for succeeded job`() = testApp {
        val dir = testDirs.last()
        seedJob(dir, "job001")
        val r = client.post("/admin/draft-jobs/job001/export-reviewed-package")
        assertEquals(HttpStatusCode.OK, r.status)
        val body = Json.parseToJsonElement(r.bodyAsText()).jsonObject
        assertEquals("\"succeeded\"", body["status"].toString())
        val files = body["files"]!!.jsonArray.map { it.jsonPrimitive.content }.toSet()
        assertTrue(files.containsAll(setOf("manifest.json", "rooms.json", "nav_graph.json", "entrance_markers.json", "route_rendering.json")))
    }

    @Test
    fun `exported rooms json reflects PATCH edits`() = testApp {
        val dir = testDirs.last()
        seedJob(dir, "job001")

        client.patch("/admin/draft-jobs/job001/rooms/zone-a") {
            contentType(ContentType.Application.Json)
            setBody("""{"displayName":"Mutfak","category":"kitchen"}""")
        }
        client.post("/admin/draft-jobs/job001/export-reviewed-package")

        val r = client.get("/admin/draft-jobs/job001/reviewed-package/rooms.json/content")
        assertEquals(HttpStatusCode.OK, r.status)
        val body = r.bodyAsText()
        assertTrue(body.contains("Mutfak"))
        assertTrue(body.contains("kitchen"))
    }

    @Test
    fun `GET reviewed-package returns 404 before export`() = testApp {
        val dir = testDirs.last()
        seedJob(dir, "job001")
        val r = client.get("/admin/draft-jobs/job001/reviewed-package")
        assertEquals(HttpStatusCode.NotFound, r.status)
    }

    @Test
    fun `GET reviewed-package lists files after export`() = testApp {
        val dir = testDirs.last()
        seedJob(dir, "job001")
        client.post("/admin/draft-jobs/job001/export-reviewed-package")
        val r = client.get("/admin/draft-jobs/job001/reviewed-package")
        assertEquals(HttpStatusCode.OK, r.status)
        val files = Json.parseToJsonElement(r.bodyAsText()).jsonArray.map { it.jsonPrimitive.content }.toSet()
        assertTrue(files.containsAll(setOf("manifest.json", "rooms.json")))
    }

    @Test
    fun `GET reviewed-package content returns JSON`() = testApp {
        val dir = testDirs.last()
        seedJob(dir, "job001")
        client.post("/admin/draft-jobs/job001/export-reviewed-package")
        val r = client.get("/admin/draft-jobs/job001/reviewed-package/manifest.json/content")
        assertEquals(HttpStatusCode.OK, r.status)
        assertTrue(r.bodyAsText().contains("buildingId"))
    }

    @Test
    fun `GET reviewed-package content rejects path traversal`() = testApp {
        val dir = testDirs.last()
        seedJob(dir, "job001")
        client.post("/admin/draft-jobs/job001/export-reviewed-package")
        val r = client.get("/admin/draft-jobs/job001/reviewed-package/..%2Fjob.json/content")
        assertTrue(r.status == HttpStatusCode.NotFound || r.status == HttpStatusCode.BadRequest)
    }
}
