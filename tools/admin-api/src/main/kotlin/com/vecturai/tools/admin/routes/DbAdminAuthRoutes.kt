package com.vecturai.tools.admin.routes

import com.vecturai.tools.admin.Env
import com.vecturai.tools.admin.model.DbAdminAuthResponse
import com.vecturai.tools.admin.model.DbAdminLoginRequest
import com.vecturai.tools.admin.service.ManagerAuthService
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
