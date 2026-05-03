package com.vecturai.tools.admin

import com.vecturai.tools.admin.model.JobStatus
import com.vecturai.tools.admin.service.DraftJobService
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import java.io.File
import kotlin.test.*

/**
 * Verifies the async upload contract:
 *   - POST /admin/draft-jobs returns 201 + queued without waiting for the pipeline
 *   - Job status transitions: queued → processing → succeeded / failed
 *   - Failures are persisted with a readable error message
 *
 * These tests document and enforce Phase 10's timeout-resilience guarantee:
 * the HTTP response must return before any preprocessing work completes.
 */
class AsyncJobLifecycleTest {

    private val testDirs = mutableListOf<File>()

    @AfterTest
    fun cleanup() {
        testDirs.forEach { it.deleteRecursively() }
        testDirs.clear()
    }

    private fun newTestDir(): File {
        val dir = File("build/test-async-${System.nanoTime()}")
        dir.mkdirs()
        testDirs.add(dir)
        return dir
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Route layer: POST /admin/draft-jobs must return before pipeline completes
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `POST draft-jobs returns 201 with queued status without blocking on pipeline`() = testApplication {
        val dir = newTestDir()
        application { configureApp(dir.absolutePath) }

        val startMs = System.currentTimeMillis()
        val response = client.submitFormWithBinaryData(
            url = "/admin/draft-jobs",
            formData = formData {
                append("file", byteArrayOf(0x67, 0x6C, 0x54, 0x46), Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=\"scan.glb\"")
                })
            }
        )
        val elapsedMs = System.currentTimeMillis() - startMs

        assertEquals(HttpStatusCode.Created, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        // Status must be queued — pipeline must NOT have run inline before this response
        assertEquals("queued", body["status"]?.jsonPrimitive?.content,
            "POST /admin/draft-jobs must return 'queued', not a terminal status")

        // Response must arrive well within a safe threshold; a synchronous pipeline
        // would take far longer than this on any non-trivial GLB input
        assertTrue(elapsedMs < 5_000,
            "POST /admin/draft-jobs took ${elapsedMs}ms — expected immediate response (< 5s)")
    }

    @Test
    fun `POST draft-jobs response status is never succeeded on immediate return`() = testApplication {
        val dir = newTestDir()
        application { configureApp(dir.absolutePath) }

        val response = client.submitFormWithBinaryData(
            url = "/admin/draft-jobs",
            formData = formData {
                append("file", byteArrayOf(0x67, 0x6C, 0x54, 0x46), Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=\"scan.glb\"")
                })
            }
        )

        assertEquals(HttpStatusCode.Created, response.status)
        val status = Json.parseToJsonElement(response.bodyAsText())
            .jsonObject["status"]?.jsonPrimitive?.content

        assertNotEquals("succeeded", status,
            "Job must not be succeeded immediately — pipeline runs asynchronously in background")
        assertNotEquals("failed", status,
            "Job must not be failed immediately — pipeline runs asynchronously in background")
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Service layer: status transitions and failure persistence
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `createJob persists queued status before pipeline runs`() {
        val dir = newTestDir()
        val service = DraftJobService(dir.absolutePath)

        val job = service.createJob("scan.glb", byteArrayOf(0x67, 0x6C, 0x54, 0x46))

        assertEquals(JobStatus.queued, job.status,
            "Newly created job must be queued before any pipeline execution")

        val persisted = service.getJob(job.id)
        assertNotNull(persisted)
        assertEquals(JobStatus.queued, persisted.status,
            "Queued status must be persisted to disk immediately after createJob")
        assertNull(persisted.errorMessage,
            "errorMessage must be null for a freshly queued job")
        assertTrue(persisted.artifacts.isEmpty(),
            "artifacts must be empty for a freshly queued job")
    }

    @Test
    fun `runDraftGeneration transitions job to failed for invalid GLB bytes`() = runBlocking {
        val dir = newTestDir()
        val service = DraftJobService(dir.absolutePath)

        // Four random bytes — not a valid GLB; pipeline will fail fast
        val job = service.createJob("bad.glb", byteArrayOf(0x00, 0x01, 0x02, 0x03))
        assertEquals(JobStatus.queued, job.status)

        val result = service.runDraftGeneration(job.id)

        assertEquals(JobStatus.failed, result.status,
            "Invalid GLB input must cause pipeline to fail")
        assertNotNull(result.errorMessage,
            "Failed job must carry an error message")
        assertTrue(result.errorMessage!!.isNotBlank(),
            "Error message must not be blank")
    }

    @Test
    fun `runDraftGeneration persists failure to disk`() = runBlocking {
        val dir = newTestDir()
        val service = DraftJobService(dir.absolutePath)

        val job = service.createJob("bad.glb", byteArrayOf(0xFF.toByte(), 0x00))
        service.runDraftGeneration(job.id)

        val persisted = service.getJob(job.id)
        assertNotNull(persisted)
        assertEquals(JobStatus.failed, persisted.status,
            "Failed status must be persisted to job.json")
        assertNotNull(persisted.errorMessage,
            "Error message must be persisted to job.json")
        assertTrue(persisted.errorMessage!!.lowercase().contains("pipeline"),
            "Error message should identify the pipeline as the source: '${persisted.errorMessage}'")
    }

    @Test
    fun `runDraftGeneration on nonexistent job throws rather than silently succeeding`() = runBlocking {
        val dir = newTestDir()
        val service = DraftJobService(dir.absolutePath)

        val threw = try {
            service.runDraftGeneration("nonexistent-job-id")
            false
        } catch (_: Exception) {
            true
        }
        assertTrue(threw, "runDraftGeneration on a nonexistent job must throw, not silently succeed")
    }
}
