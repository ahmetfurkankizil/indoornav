package com.vecturai.tools.admin.routes

import com.vecturai.tools.admin.model.CreateNodeRequest
import com.vecturai.tools.admin.model.UpdateNodeRequest
import com.vecturai.tools.admin.service.NodeService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.nodeRoutes(service: NodeService) {
    authenticate("manager-auth") {
        route("/api/manager/floors/{floorId}/nodes") {

            get {
                val managerId = managerId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val floorId   = call.parameters["floorId"] ?: return@get badRequest("floorId")
                val nodes = service.list(floorId, managerId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("Floor not found"))
                call.respond(nodes)
            }

            post {
                val managerId = managerId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val floorId   = call.parameters["floorId"] ?: return@post badRequest("floorId")
                val req = call.receive<CreateNodeRequest>()
                if (req.label.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("label is required"))
                    return@post
                }
                val node = service.create(floorId, managerId, req.label, req.nodeType, req.canvasX, req.canvasY, req.metadata)
                    ?: return@post call.respond(HttpStatusCode.NotFound, ErrorResponse("Floor not found"))
                call.respond(HttpStatusCode.Created, node)
            }

            put("{nodeId}") {
                val managerId = managerId() ?: return@put call.respond(HttpStatusCode.Unauthorized)
                val nodeId    = call.parameters["nodeId"] ?: return@put badRequest("nodeId")
                val req = call.receive<UpdateNodeRequest>()
                val node = service.update(nodeId, managerId, req.label, req.nodeType, req.canvasX, req.canvasY, req.metadata)
                    ?: return@put call.respond(HttpStatusCode.NotFound, ErrorResponse("Node not found"))
                call.respond(node)
            }

            delete("{nodeId}") {
                val managerId = managerId() ?: return@delete call.respond(HttpStatusCode.Unauthorized)
                val nodeId    = call.parameters["nodeId"] ?: return@delete badRequest("nodeId")
                if (service.delete(nodeId, managerId)) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Node not found"))
                }
            }
        }
    }
}

private fun RoutingContext.managerId(): String? = call.principal<JWTPrincipal>()?.subject
private suspend fun RoutingContext.badRequest(p: String) =
    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing $p"))
