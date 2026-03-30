package com.vecturai.tools.admin

import com.vecturai.tools.admin.routes.draftJobRoutes
import com.vecturai.tools.admin.service.DraftJobService
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun main() {
    val port = System.getenv("ADMIN_API_PORT")?.toIntOrNull() ?: 8080
    val jobsDir = System.getenv("ADMIN_JOBS_DIR") ?: "build/admin-draft-jobs"

    println("╔══════════════════════════════════════════════╗")
    println("║   VecturAI Admin API (dev-only)              ║")
    println("╚══════════════════════════════════════════════╝")
    println("  Port:     $port")
    println("  Jobs dir: $jobsDir")
    println()

    embeddedServer(Netty, port = port) {
        configureApp(jobsDir)
    }.start(wait = true)
}

fun Application.configureApp(jobsDir: String = "build/admin-draft-jobs") {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            encodeDefaults = true
        })
    }

    install(CORS) {
        anyHost()
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.ContentType)
    }

    val service = DraftJobService(jobsDir)

    routing {
        draftJobRoutes(service)
    }
}
