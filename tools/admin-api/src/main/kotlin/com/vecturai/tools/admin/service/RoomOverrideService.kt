package com.vecturai.tools.admin.service

import com.vecturai.tools.admin.model.RoomOverride
import com.vecturai.tools.admin.model.RoomOverrides
import com.vecturai.tools.admin.model.RoomPatchRequest
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant

/**
 * Manages room metadata overrides for draft jobs.
 * Overrides are stored in `room_overrides.json` under each job directory.
 * Never mutates the generated draft artifacts.
 */
class RoomOverrideService(
    private val jobsBaseDir: String,
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private fun overridesFile(jobId: String): File =
        File(jobsBaseDir, "$jobId/room_overrides.json")

    fun loadOverrides(jobId: String): RoomOverrides {
        val file = overridesFile(jobId)
        if (!file.exists()) return RoomOverrides()
        return try {
            json.decodeFromString<RoomOverrides>(file.readText())
        } catch (_: Exception) {
            RoomOverrides()
        }
    }

    fun saveOverrides(jobId: String, overrides: RoomOverrides) {
        val file = overridesFile(jobId)
        if (!file.parentFile.exists()) return
        file.writeText(json.encodeToString(RoomOverrides.serializer(), overrides))
    }

    /**
     * Apply a patch to a single room. Returns the updated override, or null if validation fails.
     * [validRoomIds] is the set of room ids from the generated draft.
     */
    fun patchRoom(
        jobId: String,
        roomId: String,
        patch: RoomPatchRequest,
        validRoomIds: Set<String>,
    ): Result<RoomOverride> {
        if (roomId !in validRoomIds) {
            return Result.failure(IllegalArgumentException("Room ID '$roomId' does not exist in the generated draft"))
        }

        if (patch.displayName != null && patch.displayName.trim().isEmpty()) {
            return Result.failure(IllegalArgumentException("displayName must be non-empty"))
        }

        val current = loadOverrides(jobId)
        val existing = current.overrides[roomId] ?: RoomOverride()

        val updated = existing.copy(
            displayName = patch.displayName ?: existing.displayName,
            category = if (patch.category != null) patch.category else existing.category,
            description = if (patch.description != null) patch.description else existing.description,
            updatedAt = Instant.now().toString(),
        )

        val newOverrides = current.copy(overrides = current.overrides + (roomId to updated))
        saveOverrides(jobId, newOverrides)

        return Result.success(updated)
    }
}
