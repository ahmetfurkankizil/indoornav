package com.VecturAI.tools.admin.service

import com.VecturAI.tools.admin.model.*
import com.VecturAI.tools.preprocessor.MainKt

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.File
import java.time.Instant
import java.util.UUID

class DraftJobService(
    private val jobsBaseDir: String = "build/admin-draft-jobs",
) {
    private val json = Json { prettyPrint = true }
    private val overrideService = RoomOverrideService(jobsBaseDir)
    private val exporter = ReviewedPackageExporter(jobsBaseDir)

    fun createJob(originalFilename: String, glbBytes: ByteArray): DraftJob {
        val jobId = UUID.randomUUID().toString().take(8)
        val now = Instant.now().toString()

        val jobDir = File(jobsBaseDir, jobId)
        jobDir.mkdirs()

        // Write the uploaded GLB
        File(jobDir, "input.glb").writeBytes(glbBytes)

        // Create output directory
        File(jobDir, "output").mkdirs()

        val job = DraftJob(
            id = jobId,
            originalFilename = originalFilename,
            createdAt = now,
            updatedAt = now,
            status = JobStatus.queued,
        )
        saveJob(job)
        return job
    }

    suspend fun runDraftGeneration(jobId: String): DraftJob {
        val job = getJob(jobId) ?: error("Job not found: $jobId")

        val updatedJob = job.copy(
            status = JobStatus.processing,
            updatedAt = Instant.now().toString(),
        )
        saveJob(updatedJob)

        val jobDir = File(jobsBaseDir, jobId)
        val glbPath = File(jobDir, "input.glb").absolutePath
        val outputDir = File(jobDir, "output").absolutePath

        return withContext(Dispatchers.IO) {
            try {
                val pipeline = com.VecturAI.tools.preprocessor.DraftPipeline()
                val exitCode = pipeline.execute(glbPath, outputDir)

                if (exitCode == 0) {
                    val artifacts = File(jobDir, "output")
                        .listFiles()
                        ?.map { it.name }
                        ?.sorted()
                        ?: emptyList()

                    val succeeded = updatedJob.copy(
                        status = JobStatus.succeeded,
                        updatedAt = Instant.now().toString(),
                        artifacts = artifacts,
                    )
                    saveJob(succeeded)
                    succeeded
                } else {
                    val failed = updatedJob.copy(
                        status = JobStatus.failed,
                        updatedAt = Instant.now().toString(),
                        errorMessage = "Draft pipeline exited with code $exitCode",
                    )
                    saveJob(failed)
                    failed
                }
            } catch (e: Exception) {
                val failed = updatedJob.copy(
                    status = JobStatus.failed,
                    updatedAt = Instant.now().toString(),
                    errorMessage = "Draft pipeline error: ${e.message}",
                )
                saveJob(failed)
                failed
            }
        }
    }

    fun getJob(jobId: String): DraftJob? {
        val jobFile = File(jobsBaseDir, "$jobId/job.json")
        if (!jobFile.exists()) return null
        return json.decodeFromString<DraftJob>(jobFile.readText())
    }

    fun listJobs(): List<DraftJob> {
        val baseDir = File(jobsBaseDir)
        if (!baseDir.exists()) return emptyList()

        return baseDir.listFiles()
            ?.filter { it.isDirectory && File(it, "job.json").exists() }
            ?.map { json.decodeFromString<DraftJob>(File(it, "job.json").readText()) }
            ?.sortedByDescending { it.createdAt }
            ?: emptyList()
    }

    fun getArtifacts(jobId: String): List<String>? {
        val job = getJob(jobId) ?: return null
        return job.artifacts
    }

    private val summaryExtractor = DraftSummaryExtractor()

    fun getSummary(jobId: String): DraftSummary? {
        val job = getJob(jobId) ?: return null
        val outputDir = File(jobsBaseDir, "$jobId/output")
        val overrides = overrideService.loadOverrides(jobId)
        return summaryExtractor.extract(jobId, job.status, outputDir, overrides)
    }

    /**
     * Returns the content of a specific artifact file, or null if the job or artifact doesn't exist.
     * Validates the artifact name against the job's known artifact list to prevent path traversal.
     */
    fun getArtifactContent(jobId: String, artifactName: String): File? {
        val job = getJob(jobId) ?: return null

        // Prevent path traversal: artifact name must be in the known list
        if (artifactName !in job.artifacts) return null

        // Extra safety: reject names with path separators
        if (artifactName.contains('/') || artifactName.contains('\\') || artifactName.contains("..")) return null

        val file = File(jobsBaseDir, "$jobId/output/$artifactName")
        return if (file.exists() && file.isFile) file else null
    }

    // ── Room overrides ─────────────────────────────────────

    fun patchRoom(jobId: String, roomId: String, patch: RoomPatchRequest): Result<RoomOverride>? {
        val job = getJob(jobId) ?: return null
        if (job.status != JobStatus.succeeded) {
            return Result.failure(IllegalStateException("Room editing requires a succeeded job"))
        }
        val validIds = getRoomIdsFromDraft(jobId)
        return overrideService.patchRoom(jobId, roomId, patch, validIds)
    }

    private fun getRoomIdsFromDraft(jobId: String): Set<String> {
        val configFile = File(jobsBaseDir, "$jobId/output/authoring_config.generated.json")
        if (!configFile.exists()) return emptySet()
        return try {
            val obj = kotlinx.serialization.json.Json.parseToJsonElement(configFile.readText()).jsonObject
            obj["rooms"]?.jsonArray?.mapNotNull {
                it.jsonObject["id"]?.jsonPrimitive?.contentOrNull
            }?.toSet() ?: emptySet()
        } catch (_: Exception) { emptySet() }
    }

    fun loadOverrides(jobId: String) = overrideService.loadOverrides(jobId)

    // ── Export ─────────────────────────────────────────────

    fun exportReviewedPackage(jobId: String): ExportResult? {
        val job = getJob(jobId) ?: return null
        if (job.status != JobStatus.succeeded) {
            return ExportResult(
                jobId = jobId,
                status = "failed",
                warnings = listOf("Export requires a succeeded job, current status: ${job.status}")
            )
        }
        val overrides = overrideService.loadOverrides(jobId)
        return exporter.export(jobId, overrides)
    }

    fun getExportedPackageFiles(jobId: String): List<String>? {
        getJob(jobId) ?: return null
        val exportDir = File(jobsBaseDir, "$jobId/reviewed-package")
        if (!exportDir.exists()) return null
        return exportDir.listFiles()?.map { it.name }?.sorted() ?: emptyList()
    }

    fun getExportedPackageFile(jobId: String, filename: String): File? {
        getJob(jobId) ?: return null
        // Safety: reject path separators
        if (filename.contains('/') || filename.contains('\\') || filename.contains("..")) return null
        val file = File(jobsBaseDir, "$jobId/reviewed-package/$filename")
        return if (file.exists() && file.isFile) file else null
    }

    private fun saveJob(job: DraftJob) {
        val jobFile = File(jobsBaseDir, "${job.id}/job.json")
        // Parent directory may not exist if the job store was cleaned up (e.g. in tests).
        if (!jobFile.parentFile.exists()) return
        jobFile.writeText(json.encodeToString(DraftJob.serializer(), job))
    }
}
