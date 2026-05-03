package com.Vectura AI.tools.admin

import com.Vectura AI.tools.admin.model.RoomPatchRequest
import com.Vectura AI.tools.admin.service.RoomOverrideService
import java.io.File
import kotlin.test.*

class RoomOverrideServiceTest {

    private lateinit var jobDir: File
    private lateinit var service: RoomOverrideService
    private val jobId = "testjob1"
    private val validIds = setOf("zone-a", "zone-b", "zone-c")

    @BeforeTest
    fun setup() {
        val base = File("build/test-overrides-${System.nanoTime()}")
        jobDir = File(base, jobId)
        jobDir.mkdirs()
        service = RoomOverrideService(base.absolutePath)
    }

    @AfterTest
    fun cleanup() {
        jobDir.parentFile.deleteRecursively()
    }

    @Test
    fun `loads empty overrides when file absent`() {
        val overrides = service.loadOverrides(jobId)
        assertTrue(overrides.overrides.isEmpty())
    }

    @Test
    fun `patches displayName and persists`() {
        val result = service.patchRoom(jobId, "zone-a", RoomPatchRequest(displayName = "Kitchen"), validIds)
        assertTrue(result.isSuccess)
        assertEquals("Kitchen", result.getOrThrow().displayName)

        val reloaded = service.loadOverrides(jobId)
        assertEquals("Kitchen", reloaded.overrides["zone-a"]?.displayName)
    }

    @Test
    fun `patches category and description`() {
        service.patchRoom(jobId, "zone-b", RoomPatchRequest(category = "bedroom", description = "Main bedroom"), validIds)
        val overrides = service.loadOverrides(jobId)
        assertEquals("bedroom", overrides.overrides["zone-b"]?.category)
        assertEquals("Main bedroom", overrides.overrides["zone-b"]?.description)
    }

    @Test
    fun `partial patch preserves existing fields`() {
        service.patchRoom(jobId, "zone-a", RoomPatchRequest(displayName = "Kitchen", category = "kitchen"), validIds)
        service.patchRoom(jobId, "zone-a", RoomPatchRequest(displayName = "Updated Kitchen"), validIds)

        val overrides = service.loadOverrides(jobId)
        val o = overrides.overrides["zone-a"]!!
        assertEquals("Updated Kitchen", o.displayName)
        assertEquals("kitchen", o.category)
    }

    @Test
    fun `rejects unknown room id`() {
        val result = service.patchRoom(jobId, "zone-zzz", RoomPatchRequest(displayName = "X"), validIds)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("zone-zzz"))
    }

    @Test
    fun `rejects empty displayName`() {
        val result = service.patchRoom(jobId, "zone-a", RoomPatchRequest(displayName = "   "), validIds)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("non-empty"))
    }

    @Test
    fun `allows null category to clear category`() {
        service.patchRoom(jobId, "zone-a", RoomPatchRequest(category = "kitchen"), validIds)
        // Sending null category means "don't change it" — category stays
        val result = service.patchRoom(jobId, "zone-a", RoomPatchRequest(displayName = "Kitchen"), validIds)
        assertTrue(result.isSuccess)
        val overrides = service.loadOverrides(jobId)
        assertEquals("kitchen", overrides.overrides["zone-a"]?.category)
    }

    @Test
    fun `multiple rooms patched independently`() {
        service.patchRoom(jobId, "zone-a", RoomPatchRequest(displayName = "Room A"), validIds)
        service.patchRoom(jobId, "zone-b", RoomPatchRequest(displayName = "Room B"), validIds)
        val overrides = service.loadOverrides(jobId)
        assertEquals("Room A", overrides.overrides["zone-a"]?.displayName)
        assertEquals("Room B", overrides.overrides["zone-b"]?.displayName)
        assertNull(overrides.overrides["zone-c"])
    }
}
