package com.Vectura AI.tools.admin

import com.Vectura AI.tools.admin.model.JobStatus
import com.Vectura AI.tools.admin.service.DraftJobService
import java.io.File
import kotlin.test.*

class DraftJobServiceTest {

    private lateinit var service: DraftJobService
    private lateinit var testDir: File

    @BeforeTest
    fun setup() {
        testDir = File("build/test-admin-jobs-${System.currentTimeMillis()}")
        testDir.mkdirs()
        service = DraftJobService(testDir.absolutePath)
    }

    @AfterTest
    fun cleanup() {
        testDir.deleteRecursively()
    }

    @Test
    fun `createJob stores GLB and creates job json`() {
        val glbBytes = byteArrayOf(0x67, 0x6C, 0x54, 0x46) // "glTF" magic
        val job = service.createJob("test-scan.glb", glbBytes)

        assertEquals(JobStatus.queued, job.status)
        assertEquals("test-scan.glb", job.originalFilename)
        assertNotNull(job.id)
        assertTrue(job.id.length == 8)

        // Verify files on disk
        val jobDir = File(testDir, job.id)
        assertTrue(jobDir.exists())
        assertTrue(File(jobDir, "input.glb").exists())
        assertTrue(File(jobDir, "job.json").exists())
        assertTrue(File(jobDir, "output").exists())

        // Verify GLB content
        assertContentEquals(glbBytes, File(jobDir, "input.glb").readBytes())
    }

    @Test
    fun `getJob returns null for nonexistent job`() {
        assertNull(service.getJob("nonexistent"))
    }

    @Test
    fun `getJob returns persisted job`() {
        val job = service.createJob("scan.glb", byteArrayOf(1, 2, 3))
        val retrieved = service.getJob(job.id)

        assertNotNull(retrieved)
        assertEquals(job.id, retrieved.id)
        assertEquals(job.originalFilename, retrieved.originalFilename)
        assertEquals(job.status, retrieved.status)
    }

    @Test
    fun `listJobs returns all jobs sorted by createdAt descending`() {
        service.createJob("first.glb", byteArrayOf(1))
        Thread.sleep(10) // ensure different timestamps
        service.createJob("second.glb", byteArrayOf(2))

        val jobs = service.listJobs()
        assertEquals(2, jobs.size)
        assertEquals("second.glb", jobs[0].originalFilename)
        assertEquals("first.glb", jobs[1].originalFilename)
    }

    @Test
    fun `getArtifacts returns null for nonexistent job`() {
        assertNull(service.getArtifacts("nonexistent"))
    }

    @Test
    fun `getArtifacts returns empty list for new job`() {
        val job = service.createJob("scan.glb", byteArrayOf(1))
        val artifacts = service.getArtifacts(job.id)
        assertNotNull(artifacts)
        assertTrue(artifacts.isEmpty())
    }
}
