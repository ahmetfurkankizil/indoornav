package com.vecturai.tools.admin

import com.vecturai.tools.admin.model.JobStatus
import com.vecturai.tools.admin.service.DraftJobService
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import java.io.File
import kotlin.test.*

class SummaryAndArtifactRoutesTest {

    private var testDir: File? = null

    @AfterTest
    fun cleanup() {
        testDir?.deleteRecursively()
    }

    private fun testApp(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        testDir = File("build/test-summary-routes-${System.currentTimeMillis()}")
        testDir!!.mkdirs()
        application { configureApp(testDir!!.absolutePath) }
        block()
    }

    /**
     * Creates a job directory directly (bypassing real pipeline) with preset artifacts.
     */
    private fun seedSuccessfulJob(
        jobsDir: File,
        jobId: String,
        withConfig: Boolean = true,
        withSvgs: Boolean = true,
    ) {
        val jobDir = File(jobsDir, jobId)
        val outputDir = File(jobDir, "output")
        outputDir.mkdirs()

        val artifacts = mutableListOf<String>()

        if (withConfig) {
            val configContent = """
            {
                "buildingId": "draft-test",
                "buildingName": "Test Building",
                "floorId": "ground",
                "nodes": [{"id":"n01"},{"id":"n02"}],
                "edges": [{"id":"e01"}],
                "rooms": [
                    {"id":"zone-a","displayName":"Zone A","destinationNodeId":"n01","category":"unknown","description":"test"}
                ],
                "entranceMarkers": [],
                "checkpointMarkers": []
            }
            """.trimIndent()
            File(outputDir, "authoring_config.generated.json").writeText(configContent)
            artifacts.add("authoring_config.generated.json")
        }

        if (withSvgs) {
            File(outputDir, "occupancy_debug.svg").writeText("<svg><rect width='100' height='100'/></svg>")
            File(outputDir, "draft_graph_debug.svg").writeText("<svg><circle cx='50' cy='50' r='10'/></svg>")
            artifacts.add("occupancy_debug.svg")
            artifacts.add("draft_graph_debug.svg")
        }

        // Write job.json
        val jobJson = """
        {
            "id": "$jobId",
            "originalFilename": "test.glb",
            "createdAt": "2026-03-29T10:00:00Z",
            "updatedAt": "2026-03-29T10:01:00Z",
            "status": "succeeded",
            "errorMessage": null,
            "artifacts": ${artifacts.joinToString(",", "[", "]") { "\"$it\"" }}
        }
        """.trimIndent()
        File(jobDir, "job.json").writeText(jobJson)
    }

    // ── Summary tests ─────────────────────────────────────

    @Test
    fun `GET summary returns 404 for nonexistent job`() = testApp {
        val response = client.get("/admin/draft-jobs/nonexistent/summary")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET summary returns parsed room list`() = testApp {
        seedSuccessfulJob(testDir!!, "abc12345")
        val response = client.get("/admin/draft-jobs/abc12345/summary")
        assertEquals(HttpStatusCode.OK, response.status)

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("\"abc12345\"", body["jobId"].toString())
        assertEquals("\"draft-test\"", body["buildingId"].toString())

        val rooms = body["rooms"]?.jsonArray
        assertNotNull(rooms)
        assertEquals(1, rooms.size)
        assertEquals("\"Zone A\"", rooms[0].jsonObject["displayName"].toString())
    }

    @Test
    fun `GET summary includes counts`() = testApp {
        seedSuccessfulJob(testDir!!, "abc12345")
        val response = client.get("/admin/draft-jobs/abc12345/summary")

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val counts = body["counts"]?.jsonObject
        assertNotNull(counts)
        assertEquals("2", counts["nodes"].toString())
        assertEquals("1", counts["edges"].toString())
        assertEquals("1", counts["rooms"].toString())
    }

    @Test
    fun `GET summary includes artifact availability`() = testApp {
        seedSuccessfulJob(testDir!!, "abc12345", withSvgs = true)
        val response = client.get("/admin/draft-jobs/abc12345/summary")

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val avail = body["artifactAvailability"]?.jsonObject
        assertNotNull(avail)
        assertEquals("true", avail["hasOccupancyPreview"].toString())
        assertEquals("true", avail["hasGraphPreview"].toString())
        assertEquals("true", avail["hasAuthoringConfig"].toString())
    }

    @Test
    fun `GET summary returns warnings when config missing`() = testApp {
        seedSuccessfulJob(testDir!!, "abc12345", withConfig = false, withSvgs = false)
        // Re-write job.json with empty artifacts
        File(testDir!!, "abc12345/job.json").writeText("""
        {
            "id": "abc12345",
            "originalFilename": "test.glb",
            "createdAt": "2026-03-29T10:00:00Z",
            "updatedAt": "2026-03-29T10:00:00Z",
            "status": "succeeded",
            "errorMessage": null,
            "artifacts": []
        }
        """.trimIndent())

        val response = client.get("/admin/draft-jobs/abc12345/summary")
        assertEquals(HttpStatusCode.OK, response.status)

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val warnings = body["warnings"]?.jsonArray
        assertNotNull(warnings)
        assertTrue(warnings.isNotEmpty())
    }

    // ── Artifact content tests ─────────────────────────────

    @Test
    fun `GET artifact content returns 404 for nonexistent job`() = testApp {
        val response = client.get("/admin/draft-jobs/nonexistent/artifacts/occupancy_debug.svg/content")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET artifact content returns 404 for unknown artifact`() = testApp {
        seedSuccessfulJob(testDir!!, "abc12345")
        val response = client.get("/admin/draft-jobs/abc12345/artifacts/unknown_file.svg/content")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET artifact content returns SVG with correct content`() = testApp {
        seedSuccessfulJob(testDir!!, "abc12345")
        val response = client.get("/admin/draft-jobs/abc12345/artifacts/occupancy_debug.svg/content")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("<svg"))
    }

    @Test
    fun `GET artifact content returns JSON file`() = testApp {
        seedSuccessfulJob(testDir!!, "abc12345")
        val response = client.get("/admin/draft-jobs/abc12345/artifacts/authoring_config.generated.json/content")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("buildingId"))
    }

    @Test
    fun `GET artifact content rejects path traversal`() = testApp {
        seedSuccessfulJob(testDir!!, "abc12345")
        // ../job.json is not in the artifacts list, so it should 404
        val response = client.get("/admin/draft-jobs/abc12345/artifacts/..%2Fjob.json/content")
        // Either 404 (not found) or 400 (bad request) — must not serve the file
        assertTrue(
            response.status == HttpStatusCode.NotFound || response.status == HttpStatusCode.BadRequest,
            "Expected 404 or 400, got ${response.status}"
        )
    }
}
