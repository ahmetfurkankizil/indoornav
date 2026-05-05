package com.vecturai.tools.admin

import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import kotlin.test.*

class RoutesTest {

    private val testDirs = mutableListOf<File>()

    @AfterTest
    fun cleanup() {
        testDirs.forEach { it.deleteRecursively() }
        testDirs.clear()
    }

    private fun newTestDir(): File {
        val dir = File("build/test-routes-${System.nanoTime()}")
        dir.mkdirs()
        testDirs.add(dir)
        return dir
    }

    private fun testApp(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        val dir = newTestDir()
        application { configureApp(dir.absolutePath) }
        block()
    }

    @Test
    fun `GET draft-jobs returns empty list initially`() = testApp {
        val response = client.get("/admin/draft-jobs")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText().trim())
    }

    @Test
    fun `POST draft-jobs rejects non-GLB file`() = testApp {
        val response = client.submitFormWithBinaryData(
            url = "/admin/draft-jobs",
            formData = formData {
                append("file", byteArrayOf(1, 2, 3), Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=\"scan.obj\"")
                })
            }
        )
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains(".glb"))
    }

    @Test
    fun `POST draft-jobs rejects empty file`() = testApp {
        val response = client.submitFormWithBinaryData(
            url = "/admin/draft-jobs",
            formData = formData {
                append("file", byteArrayOf(), Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=\"scan.glb\"")
                })
            }
        )
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("empty"))
    }

    @Test
    fun `POST draft-jobs accepts GLB and creates job`() = testApp {
        val response = client.submitFormWithBinaryData(
            url = "/admin/draft-jobs",
            formData = formData {
                append("file", byteArrayOf(0x67, 0x6C, 0x54, 0x46), Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=\"test.glb\"")
                })
            }
        )
        assertEquals(HttpStatusCode.Created, response.status)

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("\"test.glb\"", body["originalFilename"].toString())
        assertNotNull(body["id"])
        assertEquals("\"queued\"", body["status"].toString())
    }

    @Test
    fun `GET draft-jobs by id returns 404 for nonexistent`() = testApp {
        val response = client.get("/admin/draft-jobs/nonexistent")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET draft-jobs artifacts returns 404 for nonexistent`() = testApp {
        val response = client.get("/admin/draft-jobs/nonexistent/artifacts")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
