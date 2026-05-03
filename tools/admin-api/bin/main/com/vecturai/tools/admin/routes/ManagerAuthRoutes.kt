package com.vecturai.tools.admin.routes

import com.vecturai.tools.admin.model.LoginRequest
import com.vecturai.tools.admin.model.SignupRequest
import com.vecturai.tools.admin.service.ManagerAuthService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.managerAuthRoutes(authService: ManagerAuthService) {
    route("/auth") {
        post("/signup") {
            val req = call.receive<SignupRequest>()
            authService.signup(req.email, req.password, req.fullName)
                .onSuccess { call.respond(HttpStatusCode.Created, it) }
                .onFailure { call.respond(HttpStatusCode.BadRequest, ErrorResponse(it.message ?: "Signup failed")) }
        }

        post("/login") {
            val req = call.receive<LoginRequest>()
            authService.login(req.email, req.password)
                .onSuccess { call.respond(it) }
                .onFailure { call.respond(HttpStatusCode.Unauthorized, ErrorResponse(it.message ?: "Login failed")) }
        }

        authenticate("manager-auth") {
            get("/me") {
                val principal = call.principal<JWTPrincipal>()
                val managerId = principal?.subject ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val manager = authService.getById(managerId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("Manager not found"))
                call.respond(manager)
            }
        }
    }
}
