package com.vecturai.tools.admin.routes

import com.vecturai.tools.admin.Env

import com.vecturai.tools.admin.model.CreateBuildingRequest
import com.vecturai.tools.admin.model.UpdateBuildingRequest
import com.vecturai.tools.admin.service.BuildingService
import com.vecturai.tools.admin.service.NodeService
import com.vecturai.tools.admin.service.NavPackageGenerator
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.buildingRoutes(service: BuildingService, nodeService: NodeService, navGen: NavPackageGenerator) {
    authenticate("manager-auth") {
        route("/api/manager/buildings") {

            get {
                val managerId = managerId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                call.respond(service.list(managerId))
            }

            post {
                val managerId = managerId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val req = call.receive<CreateBuildingRequest>()
                if (req.name.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Building name is required"))
                    return@post
                }
                val building = service.create(managerId, req.name, req.description, req.address, req.widthMeters)
                call.respond(HttpStatusCode.Created, building)
            }

            get("{id}") {
                val managerId  = managerId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val buildingId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing id"))
                val building   = service.getById(buildingId, managerId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("Building not found"))
                call.respond(building)
            }

            put("{id}") {
                val managerId  = managerId() ?: return@put call.respond(HttpStatusCode.Unauthorized)
                val buildingId = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing id"))
                val req        = call.receive<UpdateBuildingRequest>()
                val building   = service.update(buildingId, managerId, req.name, req.description, req.address, req.widthMeters)
                    ?: return@put call.respond(HttpStatusCode.NotFound, ErrorResponse("Building not found"))
                call.respond(building)
            }

            delete("{id}") {
                val managerId  = managerId() ?: return@delete call.respond(HttpStatusCode.Unauthorized)
                val buildingId = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing id"))
                if (service.delete(buildingId, managerId)) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Building not found"))
                }
            }

            get("{id}/qr") {
                val buildingId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing id"))
                val managerId  = managerId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                // Verify ownership
                service.getById(buildingId, managerId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("Building not found"))

                val qrService = com.vecturai.tools.admin.service.QRCodeService(
                    Env.get("UPLOADS_DIR") ?: "uploads"
                )
                val bytes = qrService.getQrImageBytes(buildingId)
                    ?: qrService.generate(
                        service.getById(buildingId, managerId)!!.qrToken
                    )

                call.response.headers.append("Content-Disposition", "attachment; filename=\"building-$buildingId-qr.png\"")
                call.respondBytes(bytes, ContentType.Image.PNG)
            }

            post("{id}/publish") {
                val managerId  = managerId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val buildingId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing id"))

                service.getById(buildingId, managerId)
                    ?: return@post call.respond(HttpStatusCode.NotFound, ErrorResponse("Building not found"))

                navGen.generate(buildingId)
                service.publish(buildingId, managerId)
                call.respond(mapOf("status" to "published"))
            }

            get("{id}/nodes") {
                val managerId  = managerId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val buildingId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing id"))

                val nodes = nodeService.listByBuilding(buildingId, managerId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("Building not found"))
                call.respond(nodes)
            }
        }
    }
}

private fun RoutingContext.managerId(): String? =
    call.principal<JWTPrincipal>()?.subject
