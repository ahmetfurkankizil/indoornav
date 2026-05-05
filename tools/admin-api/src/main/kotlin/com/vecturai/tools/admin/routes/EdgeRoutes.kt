package com.vecturai.tools.admin.routes

import com.vecturai.tools.admin.model.AiSuggestRequest
import com.vecturai.tools.admin.model.CreateEdgeRequest
import com.vecturai.tools.admin.model.UpdateEdgeRequest
import com.vecturai.tools.admin.service.AiEdgeSuggester
import com.vecturai.tools.admin.service.EdgeService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.edgeRoutes(service: EdgeService, aiSuggester: AiEdgeSuggester) {
    authenticate("manager-auth") {
        route("/api/manager/floors/{floorId}/edges") {

            get {
                val managerId = managerId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val floorId   = call.parameters["floorId"] ?: return@get badRequest("floorId")
                val edges = service.list(floorId, managerId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("Floor not found"))
                call.respond(edges)
            }

            post {
                val managerId = managerId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val floorId   = call.parameters["floorId"] ?: return@post badRequest("floorId")
                val req = call.receive<CreateEdgeRequest>()
                val edge = service.create(
                    floorId, managerId,
                    req.fromNodeId, req.toNodeId,
                    req.edgeType, req.isBidirectional,
                    req.waypoints, req.createdBy,
                ) ?: return@post call.respond(HttpStatusCode.NotFound, ErrorResponse("Floor not found"))
                call.respond(HttpStatusCode.Created, edge)
            }

            put("{edgeId}") {
                val managerId = managerId() ?: return@put call.respond(HttpStatusCode.Unauthorized)
                val edgeId    = call.parameters["edgeId"] ?: return@put badRequest("edgeId")
                val req = call.receive<UpdateEdgeRequest>()
                val edge = service.update(edgeId, managerId, req.edgeType, req.isBidirectional, req.waypoints)
                    ?: return@put call.respond(HttpStatusCode.NotFound, ErrorResponse("Edge not found"))
                call.respond(edge)
            }

            delete("{edgeId}") {
                val managerId = managerId() ?: return@delete call.respond(HttpStatusCode.Unauthorized)
                val edgeId    = call.parameters["edgeId"] ?: return@delete badRequest("edgeId")
                if (service.delete(edgeId, managerId)) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Edge not found"))
                }
            }

            post("ai-suggest") {
                println("API: Received ai-suggest request")
                val managerId = managerId() ?: return@post println("API: Unauthorized manager").run { call.respond(HttpStatusCode.Unauthorized) }
                val floorId   = call.parameters["floorId"] ?: return@post badRequest("floorId")
                try {
                    val req = call.receive<AiSuggestRequest>()
                    println("API: Received ${req.nodes.size} nodes, img length ${req.floorPlanImageBase64.length}")
                    val result = aiSuggester.suggest(req.floorPlanImageBase64, req.nodes)
                    call.respond(result)
                } catch (e: Exception) {
                    println("API Error: ${e.message}")
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "AI suggest failed"))
                }
            }
        }
    }
}

private fun RoutingContext.managerId(): String? = call.principal<JWTPrincipal>()?.subject
private suspend fun RoutingContext.badRequest(p: String) =
    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing $p"))
