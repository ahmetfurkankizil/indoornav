package com.vecturai.tools.admin.routes

import com.vecturai.tools.admin.model.CreateNavMeshAreaRequest
import com.vecturai.tools.admin.model.UpdateNavMeshAreaRequest
import com.vecturai.tools.admin.service.NavMeshAreaService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.navMeshAreaRoutes(service: NavMeshAreaService) {
    authenticate("manager-auth") {
        route("/api/manager/floors/{floorId}/navmesh-areas") {

            get {
                val managerId = call.principal<JWTPrincipal>()?.subject
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val floorId = call.parameters["floorId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing floorId"))
                val areas = service.list(floorId, managerId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("Floor not found"))
                call.respond(areas)
            }

            post {
                val managerId = call.principal<JWTPrincipal>()?.subject
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val floorId = call.parameters["floorId"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing floorId"))
                val req = call.receive<CreateNavMeshAreaRequest>()
                val area = service.create(floorId, managerId, req.label, req.vertices)
                    ?: return@post call.respond(HttpStatusCode.NotFound, ErrorResponse("Floor not found"))
                call.respond(HttpStatusCode.Created, area)
            }

            put("{areaId}") {
                val managerId = call.principal<JWTPrincipal>()?.subject
                    ?: return@put call.respond(HttpStatusCode.Unauthorized)
                val areaId = call.parameters["areaId"]
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing areaId"))
                val req = call.receive<UpdateNavMeshAreaRequest>()
                val area = service.update(areaId, managerId, req.label, req.vertices)
                    ?: return@put call.respond(HttpStatusCode.NotFound, ErrorResponse("Area not found"))
                call.respond(area)
            }

            delete("{areaId}") {
                val managerId = call.principal<JWTPrincipal>()?.subject
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized)
                val areaId = call.parameters["areaId"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing areaId"))
                if (service.delete(areaId, managerId)) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Area not found"))
                }
            }
        }
    }
}
