package com.Vectura AI.tools.admin.routes

import com.Vectura AI.tools.admin.Env
import com.Vectura AI.tools.admin.model.DbAdminAuthResponse
import com.Vectura AI.tools.admin.model.DbAdminLoginRequest
import com.Vectura AI.tools.admin.service.ManagerAuthService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.dbAdminAuthRoutes(authService: ManagerAuthService) {
    val adminUser = Env.get("DB_ADMIN_USER") ?: "sysadmin"
    val adminPass = Env.get("DB_ADMIN_PASS") ?: ""

    route("/db-admin/auth") {
        post("/login") {
            val req = call.receive<DbAdminLoginRequest>()
            if (req.username != adminUser || req.password != adminPass || adminPass.isBlank()) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid credentials"))
                return@post
            }
            val token = authService.issueDbAdminToken(req.username)
            call.respond(DbAdminAuthResponse(token = token))
        }
    }
}
