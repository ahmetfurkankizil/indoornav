package com.vecturai.tools.admin.routes

import com.vecturai.tools.admin.model.FloorBoundsRequest
import com.vecturai.tools.admin.service.FloorService
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*

fun Route.floorRoutes(service: FloorService) {
    authenticate("manager-auth") {
        route("/api/manager/buildings/{buildingId}/floors") {

            get {
                val managerId  = managerId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val buildingId = call.parameters["buildingId"] ?: return@get badRequest("buildingId")
                val floors = service.list(buildingId, managerId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("Building not found"))
                call.respond(floors)
            }

            post {
                val managerId  = managerId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val buildingId = call.parameters["buildingId"] ?: return@post badRequest("buildingId")

                var floorNumber: Int? = null
                var floorName: String? = null
                var mapBytes: ByteArray? = null
                var mapFileName = "floor.glb"

                call.receiveMultipart().forEachPart { part ->
                    when {
                        part is PartData.FormItem && part.name == "floorNumber" ->
                            floorNumber = part.value.toIntOrNull()
                        part is PartData.FormItem && part.name == "floorName" ->
                            floorName = part.value
                        part is PartData.FileItem && (part.name == "mapFile" || part.name == "glbFile") -> {
                            mapFileName = part.originalFileName ?: "floor.glb"
                            mapBytes = part.provider().toByteArray()
                        }
                    }
                    part.dispose()
                }

                val fn = floorNumber ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("floorNumber required"))
                val name = floorName?.takeIf { it.isNotBlank() } ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("floorName required"))
                val bytes = mapBytes?.takeIf { it.isNotEmpty() } ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("mapFile required"))
                val ext = resolveExtension(mapFileName)

                val floor = service.create(buildingId, managerId, fn, name, bytes, ext)
                    ?: return@post call.respond(HttpStatusCode.NotFound, ErrorResponse("Building not found"))
                call.respond(HttpStatusCode.Created, floor)
            }

            get("{floorId}") {
                val managerId = managerId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val floorId   = call.parameters["floorId"] ?: return@get badRequest("floorId")
                val floor = service.getById(floorId, managerId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("Floor not found"))
                call.respond(floor)
            }

            put("{floorId}") {
                val managerId = managerId() ?: return@put call.respond(HttpStatusCode.Unauthorized)
                val floorId   = call.parameters["floorId"] ?: return@put badRequest("floorId")

                var floorName: String? = null
                var floorNumber: Int? = null
                var mapBytes: ByteArray? = null
                var mapFileName = "floor.glb"

                call.receiveMultipart().forEachPart { part ->
                    when {
                        part is PartData.FormItem && part.name == "floorName"   -> floorName   = part.value
                        part is PartData.FormItem && part.name == "floorNumber" -> floorNumber = part.value.toIntOrNull()
                        part is PartData.FileItem && (part.name == "mapFile" || part.name == "glbFile") -> {
                            mapFileName = part.originalFileName ?: "floor.glb"
                            mapBytes = part.provider().toByteArray()
                        }
                    }
                    part.dispose()
                }

                val bytes = mapBytes?.takeIf { it.isNotEmpty() }
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ErrorResponse("mapFile required"))
                val ext = resolveExtension(mapFileName)

                val floor = service.replaceGlb(floorId, managerId, bytes, ext, floorName, floorNumber)
                    ?: return@put call.respond(HttpStatusCode.NotFound, ErrorResponse("Floor not found"))
                call.respond(floor)
            }

            delete("{floorId}") {
                val managerId = managerId() ?: return@delete call.respond(HttpStatusCode.Unauthorized)
                val floorId   = call.parameters["floorId"] ?: return@delete badRequest("floorId")
                if (service.delete(floorId, managerId)) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Floor not found"))
                }
            }

            get("{floorId}/glb") {
                val managerId = managerId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val floorId   = call.parameters["floorId"] ?: return@get badRequest("floorId")
                service.getById(floorId, managerId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("Floor not found"))
                val (bytes, ext) = service.getMapFile(floorId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("Map file not found"))
                val contentType = when (ext) {
                    "svg" -> ContentType.parse("image/svg+xml")
                    "png" -> ContentType.Image.PNG
                    "dxf" -> ContentType.parse("application/dxf")
                    else  -> ContentType.Application.OctetStream
                }
                call.response.headers.append("Content-Disposition", "inline; filename=\"floor-$floorId.$ext\"")
                call.respondBytes(bytes, contentType)
            }

            put("{floorId}/bounds") {
                val managerId = managerId() ?: return@put call.respond(HttpStatusCode.Unauthorized)
                val floorId   = call.parameters["floorId"] ?: return@put badRequest("floorId")
                val req = call.receive<FloorBoundsRequest>()
                val floor = service.updateBounds(floorId, managerId, req)
                    ?: return@put call.respond(HttpStatusCode.NotFound, ErrorResponse("Floor not found"))
                call.respond(floor)
            }
        }
    }
}

private fun RoutingContext.managerId(): String? =
    call.principal<JWTPrincipal>()?.subject

private suspend fun RoutingContext.badRequest(param: String) =
    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing $param"))

private val ALLOWED_EXTENSIONS = setOf("glb", "svg", "png", "dxf")

private fun resolveExtension(fileName: String): String =
    fileName.substringAfterLast('.', "glb").lowercase().let {
        if (it in ALLOWED_EXTENSIONS) it else "glb"
    }
