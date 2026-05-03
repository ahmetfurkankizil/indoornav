package com.vecturai.tools.admin.routes

import com.vecturai.tools.admin.Env

import com.vecturai.tools.admin.db.tables.EntranceMarkers
import com.vecturai.tools.admin.db.tables.Nodes
import com.vecturai.tools.admin.model.CreateBuildingRequest
import com.vecturai.tools.admin.model.CreateEntranceMarkerRequest
import com.vecturai.tools.admin.model.EntranceMarkerResponse
import com.vecturai.tools.admin.model.UpdateBuildingRequest
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
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

            get("{id}/entrance-markers") {
                val managerId  = managerId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val buildingId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing id"))
                service.getById(buildingId, managerId) ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("Building not found"))
                val markers = transaction {
                    EntranceMarkers.selectAll()
                        .where { EntranceMarkers.buildingId eq UUID.fromString(buildingId) }
                        .map { r ->
                            EntranceMarkerResponse(
                                id = r[EntranceMarkers.id].value.toString(),
                                buildingId = buildingId,
                                displayName = r[EntranceMarkers.displayName],
                                startNodeId = r[EntranceMarkers.startNodeId].value.toString(),
                                physicalWidthMeters = r[EntranceMarkers.physicalWidthMeters],
                                physicalHeightMeters = r[EntranceMarkers.physicalHeightMeters],
                                worldX = r[EntranceMarkers.worldX],
                                worldY = r[EntranceMarkers.worldY],
                                worldZ = r[EntranceMarkers.worldZ],
                                forwardBasis = r[EntranceMarkers.forwardBasis],
                                rotationYDegrees = r[EntranceMarkers.rotationYDegrees],
                                referenceImageName = r[EntranceMarkers.referenceImageName],
                            )
                        }
                }
                call.respond(markers)
            }

            post("{id}/entrance-markers") {
                val managerId  = managerId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val buildingId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing id"))
                service.getById(buildingId, managerId) ?: return@post call.respond(HttpStatusCode.NotFound, ErrorResponse("Building not found"))
                val req = call.receive<CreateEntranceMarkerRequest>()
                val nodeExists = transaction {
                    Nodes.selectAll().where { Nodes.id eq UUID.fromString(req.startNodeId) }.any()
                }
                if (!nodeExists) return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("startNodeId not found"))
                val marker = transaction {
                    val newId = EntranceMarkers.insertAndGetId {
                        it[EntranceMarkers.buildingId]          = UUID.fromString(buildingId)
                        it[EntranceMarkers.displayName]         = req.displayName
                        it[EntranceMarkers.startNodeId]         = UUID.fromString(req.startNodeId)
                        it[EntranceMarkers.physicalWidthMeters]  = req.physicalWidthMeters
                        it[EntranceMarkers.physicalHeightMeters] = req.physicalHeightMeters
                        it[EntranceMarkers.worldX]              = req.worldX
                        it[EntranceMarkers.worldY]              = req.worldY
                        it[EntranceMarkers.worldZ]              = req.worldZ
                        it[EntranceMarkers.forwardBasis]        = req.forwardBasis
                        it[EntranceMarkers.rotationYDegrees]    = req.rotationYDegrees
                        it[EntranceMarkers.referenceImageName]  = req.referenceImageName
                    }
                    EntranceMarkerResponse(
                        id = newId.value.toString(), buildingId = buildingId,
                        displayName = req.displayName, startNodeId = req.startNodeId,
                        physicalWidthMeters = req.physicalWidthMeters, physicalHeightMeters = req.physicalHeightMeters,
                        worldX = req.worldX, worldY = req.worldY, worldZ = req.worldZ,
                        forwardBasis = req.forwardBasis, rotationYDegrees = req.rotationYDegrees,
                        referenceImageName = req.referenceImageName,
                    )
                }
                call.respond(HttpStatusCode.Created, marker)
            }

            delete("{id}/entrance-markers/{markerId}") {
                val managerId  = managerId() ?: return@delete call.respond(HttpStatusCode.Unauthorized)
                val buildingId = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing id"))
                val markerId   = call.parameters["markerId"] ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing markerId"))
                service.getById(buildingId, managerId) ?: return@delete call.respond(HttpStatusCode.NotFound, ErrorResponse("Building not found"))
                val deleted = transaction {
                    EntranceMarkers.deleteWhere {
                        (EntranceMarkers.id eq UUID.fromString(markerId)) and
                        (EntranceMarkers.buildingId eq UUID.fromString(buildingId))
                    } > 0
                }
                if (deleted) call.respond(HttpStatusCode.NoContent) else call.respond(HttpStatusCode.NotFound, ErrorResponse("Marker not found"))
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
