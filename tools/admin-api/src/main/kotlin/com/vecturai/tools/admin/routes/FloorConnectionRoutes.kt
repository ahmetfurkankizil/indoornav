package com.Vectura AI.tools.admin.routes

import com.Vectura AI.tools.admin.model.CreateFloorConnectionRequest
import com.Vectura AI.tools.admin.service.FloorConnectionService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.floorConnectionRoutes(service: FloorConnectionService) {
    authenticate("manager-auth") {
        route("/api/manager/buildings/{buildingId}/connections") {

            get {
                val managerId  = managerId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val buildingId = call.parameters["buildingId"] ?: return@get badRequest("buildingId")
                val conns = service.list(buildingId, managerId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("Building not found"))
                call.respond(conns)
            }

            post {
                val managerId  = managerId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val buildingId = call.parameters["buildingId"] ?: return@post badRequest("buildingId")
                val req = call.receive<CreateFloorConnectionRequest>()
                val conn = service.create(
                    buildingId, managerId,
                    req.fromNodeId, req.toNodeId,
                    req.connectionType, req.isBidirectional,
                ) ?: return@post call.respond(HttpStatusCode.NotFound, ErrorResponse("Building not found"))
                call.respond(HttpStatusCode.Created, conn)
            }

            delete("{connectionId}") {
                val managerId    = managerId() ?: return@delete call.respond(HttpStatusCode.Unauthorized)
                val connectionId = call.parameters["connectionId"] ?: return@delete badRequest("connectionId")
                if (service.delete(connectionId, managerId)) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Connection not found"))
                }
            }
        }
    }
}

private fun RoutingContext.managerId(): String? = call.principal<JWTPrincipal>()?.subject
private suspend fun RoutingContext.badRequest(p: String) =
    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing $p"))
