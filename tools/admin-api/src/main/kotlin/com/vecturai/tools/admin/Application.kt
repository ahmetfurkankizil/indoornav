package com.Vectura AI.tools.admin

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.Vectura AI.tools.admin.db.DatabaseFactory
import com.Vectura AI.tools.admin.routes.*
import com.Vectura AI.tools.admin.service.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.github.cdimascio.dotenv.dotenv
import kotlinx.serialization.json.*

fun main() {
    val port       = Env.get("ADMIN_API_PORT")?.toIntOrNull() ?: 8080
    val adminUser = Env.get("DB_ADMIN_USER") ?: "sysadmin"
    val adminPass = Env.get("DB_ADMIN_PASS") ?: ""
    val jobsDir    = Env.get("ADMIN_JOBS_DIR") ?: "build/admin-draft-jobs"
    val uploadsDir = Env.get("UPLOADS_DIR") ?: "uploads"

    println("╔══════════════════════════════════════════════╗")
    println("║   Vectura AI Admin API                         ║")
    println("╚══════════════════════════════════════════════╝")
    println("  Port:        $port")
    println("  Uploads dir: $uploadsDir")
    println("  Jobs dir:    $jobsDir  (legacy)")
    println()

    DatabaseFactory.init()

    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        configureApp(jobsDir, uploadsDir)
    }.start(wait = true)
}

fun Application.configureApp(
    jobsDir: String    = "build/admin-draft-jobs",
    uploadsDir: String = "uploads",
) {
    val jwtSecret = Env.get("JWT_SECRET") ?: "dev-secret-change-in-production"
    val jwtIssuer = Env.get("JWT_ISSUER") ?: "Vectura AI"

    install(ContentNegotiation) {
        json(Json {
            prettyPrint       = true
            isLenient         = true
            encodeDefaults    = true
            ignoreUnknownKeys = true
        })
    }

    install(CORS) {
        val origin = Env.get("MANAGER_WEB_ORIGIN") ?: "http://localhost:5173"
        val host   = origin.removePrefix("https://").removePrefix("http://")
        allowHost(host, schemes = listOf("http", "https"))
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Options)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowCredentials = true
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            val code = when (cause) {
                is IllegalArgumentException -> HttpStatusCode.BadRequest
                else                        -> HttpStatusCode.InternalServerError
            }
            call.respond(code, buildJsonObject { put("error", cause.message ?: "Server error") })
        }
    }

    install(Authentication) {
        jwt("manager-auth") {
            realm = "Vectura AI Manager"
            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret))
                    .withIssuer(jwtIssuer)
                    .withClaim("role", "manager")
                    .build()
            )
            validate { credential ->
                if (credential.payload.subject != null) JWTPrincipal(credential.payload) else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, buildJsonObject { put("error", "Authentication required") })
            }
        }

        jwt("superadmin-auth") {
            realm = "Vectura AI DB Admin"
            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret))
                    .withIssuer(jwtIssuer)
                    .withClaim("role", "superadmin")
                    .build()
            )
            validate { credential ->
                if (credential.payload.subject != null) JWTPrincipal(credential.payload) else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, buildJsonObject { put("error", "DB Admin authentication required") })
            }
        }
    }

    // Services
    val authService      = ManagerAuthService(jwtSecret, jwtIssuer)
    val qrService        = QRCodeService(uploadsDir)
    val buildingService  = BuildingService(qrService)
    val floorService     = FloorService(uploadsDir)
    val nodeService      = NodeService()
    val edgeService      = EdgeService()
    val connService      = FloorConnectionService()
    val navMeshService   = NavMeshAreaService()
    val aiSuggester      = AiEdgeSuggester()
    val navGen           = NavPackageGenerator()
    val draftService     = DraftJobService(jobsDir)  // legacy

    routing {
        managerAuthRoutes(authService)
        dbAdminAuthRoutes(authService)
        buildingRoutes(buildingService, nodeService, navGen)
        floorRoutes(floorService)
        nodeRoutes(nodeService)
        edgeRoutes(edgeService, aiSuggester)
        floorConnectionRoutes(connService)
        navMeshAreaRoutes(navMeshService)
        mobileApiRoutes()
        dbAdminRoutes(navGen)

        // Diagnostic route
        get("/api/admin/test-env") {
            val url = Env.get("OLLAMA_BASE_URL")
            val model = Env.get("OLLAMA_MODEL")
            call.respond(buildJsonObject {
                put("ollamaUrl", url ?: "not set")
                put("ollamaModel", model ?: "not set")
                put("workingDir", System.getProperty("user.dir"))
            })
        }

        // Legacy draft-job pipeline — preserved for backward compatibility
        draftJobRoutes(draftService)
    }
}
