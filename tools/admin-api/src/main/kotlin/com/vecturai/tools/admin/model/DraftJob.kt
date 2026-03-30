package com.vecturai.tools.admin.model

import kotlinx.serialization.Serializable

@Serializable
data class DraftJob(
    val id: String,
    val originalFilename: String,
    val createdAt: String,
    val updatedAt: String,
    val status: JobStatus,
    val errorMessage: String? = null,
    val artifacts: List<String> = emptyList(),
)

@Serializable
enum class JobStatus {
    queued,
    processing,
    succeeded,
    failed,
}
