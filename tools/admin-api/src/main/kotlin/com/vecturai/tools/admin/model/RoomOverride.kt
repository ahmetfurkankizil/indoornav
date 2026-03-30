package com.vecturai.tools.admin.model

import kotlinx.serialization.Serializable

/**
 * A single room metadata override, keyed by room id.
 * Only non-null fields replace the generated draft value.
 */
@Serializable
data class RoomOverride(
    val displayName: String? = null,
    val category: String? = null,
    val description: String? = null,
    val updatedAt: String? = null,
)

/**
 * Container for all room overrides for a given job.
 */
@Serializable
data class RoomOverrides(
    val overrides: Map<String, RoomOverride> = emptyMap(),
)

/**
 * Request body for patching a room.
 */
@Serializable
data class RoomPatchRequest(
    val displayName: String? = null,
    val category: String? = null,
    val description: String? = null,
)

/**
 * Response after export.
 */
@Serializable
data class ExportResult(
    val jobId: String,
    val status: String,
    val files: List<String> = emptyList(),
    val exportPath: String? = null,
    val warnings: List<String> = emptyList(),
)
