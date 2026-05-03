package com.Vectura AI.tools.admin.routes

import com.Vectura AI.tools.admin.model.RoomPatchRequest
import com.Vectura AI.tools.admin.service.DraftJobService
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(val error: String)

fun Route.draftJobRoutes(service: DraftJobService) {

    route("/admin/draft-jobs") {

        // POST /admin/draft-jobs — upload GLB and create a draft job
        post {
            val multipart = call.receiveMultipart()
            var filename: String? = null
            var glbBytes: ByteArray? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        if (part.name == "file") {
                            filename = part.originalFileName
                            val channel = part.provider()
                            glbBytes = channel.toByteArray()
                        }
                    }
                    else -> {}
                }
                part.dispose()
            }

            if (filename == null || glbBytes == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing 'file' part in multipart upload"))
                return@post
            }

            if (!filename!!.lowercase().endsWith(".glb")) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Only .glb files are accepted"))
                return@post
            }

            if (glbBytes!!.isEmpty()) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Uploaded file is empty"))
                return@post
            }

            val job = service.createJob(filename!!, glbBytes!!)

            // Run draft generation asynchronously in-process
            CoroutineScope(Dispatchers.IO).launch {
                service.runDraftGeneration(job.id)
            }

            call.respond(HttpStatusCode.Created, job)
        }

        // GET /admin/draft-jobs — list all jobs
        get {
            val jobs = service.listJobs()
            call.respond(jobs)
        }

        // GET /admin/draft-jobs/{jobId}
        get("{jobId}") {
            val jobId = call.parameters["jobId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing jobId"))

            val job = service.getJob(jobId)
                ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("Job not found: $jobId"))

            call.respond(job)
        }

        // GET /admin/draft-jobs/{jobId}/artifacts
        get("{jobId}/artifacts") {
            val jobId = call.parameters["jobId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing jobId"))

            val artifacts = service.getArtifacts(jobId)
                ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("Job not found: $jobId"))

            call.respond(artifacts)
        }

        // GET /admin/draft-jobs/{jobId}/summary
        get("{jobId}/summary") {
            val jobId = call.parameters["jobId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing jobId"))

            val summary = service.getSummary(jobId)
                ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("Job not found: $jobId"))

            call.respond(summary)
        }

        // GET /admin/draft-jobs/{jobId}/artifacts/{artifactName}/content
        get("{jobId}/artifacts/{artifactName}/content") {
            val jobId = call.parameters["jobId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing jobId"))
            val artifactName = call.parameters["artifactName"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing artifactName"))

            val file = service.getArtifactContent(jobId, artifactName)
                ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("Artifact not found: $artifactName"))

            val contentType = when {
                artifactName.endsWith(".svg") -> ContentType.Image.SVG
                artifactName.endsWith(".json") -> ContentType.Application.Json
                else -> ContentType.Application.OctetStream
            }

            call.respond(object : io.ktor.http.content.OutgoingContent.ByteArrayContent() {
                override val contentType = contentType
                override fun bytes() = file.readBytes()
            })
        }

        // PATCH /admin/draft-jobs/{jobId}/rooms/{roomId}
        patch("{jobId}/rooms/{roomId}") {
            val jobId = call.parameters["jobId"]
                ?: return@patch call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing jobId"))
            val roomId = call.parameters["roomId"]
                ?: return@patch call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing roomId"))

            val patch = try {
                call.receive<RoomPatchRequest>()
            } catch (e: Exception) {
                return@patch call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid request body: ${e.message}"))
            }

            val result = service.patchRoom(jobId, roomId, patch)
                ?: return@patch call.respond(HttpStatusCode.NotFound, ErrorResponse("Job not found: $jobId"))

            result.fold(
                onSuccess = { override -> call.respond(override) },
                onFailure = { e -> call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Patch failed")) }
            )
        }

        // POST /admin/draft-jobs/{jobId}/export-reviewed-package
        post("{jobId}/export-reviewed-package") {
            val jobId = call.parameters["jobId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing jobId"))

            val result = service.exportReviewedPackage(jobId)
                ?: return@post call.respond(HttpStatusCode.NotFound, ErrorResponse("Job not found: $jobId"))

            val statusCode = if (result.status == "succeeded") HttpStatusCode.OK else HttpStatusCode.UnprocessableEntity
            call.respond(statusCode, result)
        }

        // GET /admin/draft-jobs/{jobId}/reviewed-package
        get("{jobId}/reviewed-package") {
            val jobId = call.parameters["jobId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing jobId"))

            val files = service.getExportedPackageFiles(jobId)
                ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("Job not found: $jobId"))

            if (files.isEmpty()) {
                return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("No reviewed package exported yet for job: $jobId"))
            }

            call.respond(files)
        }

        // GET /admin/draft-jobs/{jobId}/reviewed-package/{filename}/content
        get("{jobId}/reviewed-package/{filename}/content") {
            val jobId = call.parameters["jobId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing jobId"))
            val filename = call.parameters["filename"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing filename"))

            val file = service.getExportedPackageFile(jobId, filename)
                ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("File not found: $filename"))

            val contentType = if (filename.endsWith(".json")) ContentType.Application.Json else ContentType.Application.OctetStream
            call.respond(object : io.ktor.http.content.OutgoingContent.ByteArrayContent() {
                override val contentType = contentType
                override fun bytes() = file.readBytes()
            })
        }
    }
}
