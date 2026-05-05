package com.vecturai.tools.admin.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.vecturai.tools.admin.db.tables.Managers
import com.vecturai.tools.admin.model.AuthResponse
import com.vecturai.tools.admin.model.ManagerResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.time.Instant
import java.util.Date
import java.util.UUID

class ManagerAuthService(
    private val jwtSecret: String,
    private val jwtIssuer: String,
) {
    private val tokenTtlMs = 30L * 24 * 60 * 60 * 1000  // 30 days

    suspend fun signup(email: String, password: String, fullName: String): Result<AuthResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                val emailLower = email.lowercase().trim()
                require(emailLower.contains('@')) { "Invalid email address" }
                require(password.length >= 8) { "Password must be at least 8 characters" }
                require(fullName.isNotBlank()) { "Full name is required" }

                transaction {
                    val existing = Managers.selectAll()
                        .where { Managers.email eq emailLower }
                        .singleOrNull()
                    require(existing == null) { "An account with this email already exists" }

                    val hash = BCrypt.hashpw(password, BCrypt.gensalt())
                    val now = Instant.now()
                    val id = Managers.insertAndGetId {
                        it[Managers.email]        = emailLower
                        it[Managers.passwordHash] = hash
                        it[Managers.fullName]     = fullName.trim()
                        it[Managers.createdAt]    = now
                        it[Managers.updatedAt]    = now
                    }

                    val manager = ManagerResponse(
                        id        = id.value.toString(),
                        email     = emailLower,
                        fullName  = fullName.trim(),
                        createdAt = now.toString(),
                    )
                    AuthResponse(token = issueToken(id.value.toString(), "manager"), manager = manager)
                }
            }
        }

    suspend fun login(email: String, password: String): Result<AuthResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                val emailLower = email.lowercase().trim()
                transaction {
                    val row = Managers.selectAll()
                        .where { Managers.email eq emailLower }
                        .singleOrNull()
                        ?: error("Invalid email or password")

                    require(BCrypt.checkpw(password, row[Managers.passwordHash])) {
                        "Invalid email or password"
                    }

                    val manager = ManagerResponse(
                        id        = row[Managers.id].value.toString(),
                        email     = row[Managers.email],
                        fullName  = row[Managers.fullName],
                        createdAt = row[Managers.createdAt].toString(),
                    )
                    AuthResponse(token = issueToken(manager.id, "manager"), manager = manager)
                }
            }
        }

    suspend fun getById(managerId: String): ManagerResponse? =
        withContext(Dispatchers.IO) {
            transaction {
                Managers.selectAll()
                    .where { Managers.id eq UUID.fromString(managerId) }
                    .singleOrNull()
                    ?.let { row ->
                        ManagerResponse(
                            id        = row[Managers.id].value.toString(),
                            email     = row[Managers.email],
                            fullName  = row[Managers.fullName],
                            createdAt = row[Managers.createdAt].toString(),
                        )
                    }
            }
        }

    fun issueDbAdminToken(username: String): String =
        issueToken(username, "superadmin")

    private fun issueToken(subject: String, role: String): String =
        JWT.create()
            .withIssuer(jwtIssuer)
            .withSubject(subject)
            .withClaim("role", role)
            .withExpiresAt(Date(System.currentTimeMillis() + tokenTtlMs))
            .sign(Algorithm.HMAC256(jwtSecret))
}
