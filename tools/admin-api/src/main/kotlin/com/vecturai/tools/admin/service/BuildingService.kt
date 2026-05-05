package com.vecturai.tools.admin.service

import com.vecturai.tools.admin.db.tables.Buildings
import com.vecturai.tools.admin.db.tables.Floors
import com.vecturai.tools.admin.model.BuildingResponse
import com.vecturai.tools.admin.model.FloorResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

class BuildingService(private val qrService: QRCodeService) {

    suspend fun create(
        managerId: String,
        name: String,
        description: String?,
        address: String?,
        widthMeters: Double = 25.0,
    ): BuildingResponse = withContext(Dispatchers.IO) {
        transaction {
            val now = Instant.now()
            val token = generateToken()
            val id = Buildings.insertAndGetId {
                it[Buildings.managerId]   = UUID.fromString(managerId)
                it[Buildings.name]        = name
                it[Buildings.description] = description
                it[Buildings.address]     = address
                it[Buildings.widthMeters]  = widthMeters
                it[Buildings.qrToken]     = token
                it[Buildings.status]      = "draft"
                it[Buildings.createdAt]   = now
                it[Buildings.updatedAt]   = now
            }
            qrService.saveQrImage(id.value.toString(), token)
            Buildings.update({ Buildings.id eq id }) {
                it[Buildings.qrImagePath] = "uploads/qr/${id.value}.png"
            }
            rowToResponse(
                Buildings.selectAll().where { Buildings.id eq id }.single(),
                emptyList(),
            )
        }
    }

    suspend fun list(managerId: String): List<BuildingResponse> = withContext(Dispatchers.IO) {
        transaction {
            Buildings.selectAll()
                .where { Buildings.managerId eq UUID.fromString(managerId) }
                .orderBy(Buildings.createdAt to SortOrder.DESC)
                .map { row ->
                    val floors = Floors.selectAll()
                        .where { Floors.buildingId eq row[Buildings.id] }
                        .orderBy(Floors.floorNumber to SortOrder.ASC)
                        .map { floorRowToResponse(it) }
                    rowToResponse(row, floors)
                }
        }
    }

    suspend fun getById(buildingId: String, managerId: String): BuildingResponse? =
        withContext(Dispatchers.IO) {
            transaction {
                val row = Buildings.selectAll()
                    .where {
                        (Buildings.id eq UUID.fromString(buildingId)) and
                            (Buildings.managerId eq UUID.fromString(managerId))
                    }
                    .singleOrNull() ?: return@transaction null

                val floors = Floors.selectAll()
                    .where { Floors.buildingId eq row[Buildings.id] }
                    .orderBy(Floors.floorNumber to SortOrder.ASC)
                    .map { floorRowToResponse(it) }

                rowToResponse(row, floors)
            }
        }

    suspend fun update(
        buildingId: String,
        managerId: String,
        name: String?,
        description: String?,
        address: String?,
        widthMeters: Double?,
    ): BuildingResponse? = withContext(Dispatchers.IO) {
        val exists = transaction {
            Buildings.selectAll()
                .where {
                    (Buildings.id eq UUID.fromString(buildingId)) and
                        (Buildings.managerId eq UUID.fromString(managerId))
                }
                .singleOrNull()
        } ?: return@withContext null

        transaction {
            Buildings.update({
                (Buildings.id eq UUID.fromString(buildingId)) and
                    (Buildings.managerId eq UUID.fromString(managerId))
            }) {
                name?.let { v -> it[Buildings.name] = v }
                it[Buildings.description] = description ?: exists[Buildings.description]
                it[Buildings.address] = address ?: exists[Buildings.address]
                widthMeters?.let { v -> it[Buildings.widthMeters] = v }
                it[Buildings.updatedAt] = Instant.now()
            }
        }

        getById(buildingId, managerId)
    }

    suspend fun delete(buildingId: String, managerId: String): Boolean =
        withContext(Dispatchers.IO) {
            transaction {
                Buildings.deleteWhere {
                    (id eq UUID.fromString(buildingId)) and
                        (Buildings.managerId eq UUID.fromString(managerId))
                } > 0
            }
        }

    suspend fun publish(buildingId: String, managerId: String): Boolean =
        withContext(Dispatchers.IO) {
            transaction {
                Buildings.update({
                    (Buildings.id eq UUID.fromString(buildingId)) and
                        (Buildings.managerId eq UUID.fromString(managerId))
                }) {
                    it[Buildings.status]    = "published"
                    it[Buildings.updatedAt] = Instant.now()
                } > 0
            }
        }

    suspend fun getByQrToken(token: String): BuildingResponse? = withContext(Dispatchers.IO) {
        transaction {
            Buildings.selectAll()
                .where { Buildings.qrToken eq token }
                .singleOrNull()
                ?.let { rowToResponse(it, null) }
        }
    }

    private fun generateToken(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return (1..16).map { chars.random() }.joinToString("")
    }

    private fun rowToResponse(row: ResultRow, floors: List<FloorResponse>?): BuildingResponse =
        BuildingResponse(
            id          = row[Buildings.id].value.toString(),
            managerId   = row[Buildings.managerId].value.toString(),
            name        = row[Buildings.name],
            description = row[Buildings.description],
            address     = row[Buildings.address],
            qrToken     = row[Buildings.qrToken],
            widthMeters = row[Buildings.widthMeters],
            status      = row[Buildings.status],
            createdAt   = row[Buildings.createdAt].toString(),
            updatedAt   = row[Buildings.updatedAt].toString(),
            floors      = floors,
        )

    private fun floorRowToResponse(row: ResultRow): FloorResponse =
        FloorResponse(
            id           = row[Floors.id].value.toString(),
            buildingId   = row[Floors.buildingId].value.toString(),
            floorNumber  = row[Floors.floorNumber],
            floorName    = row[Floors.floorName],
            boundsMinX   = row[Floors.boundsMinX],
            boundsMaxX   = row[Floors.boundsMaxX],
            boundsMinZ   = row[Floors.boundsMinZ],
            boundsMaxZ   = row[Floors.boundsMaxZ],
            floorY       = row[Floors.floorY],
            uploadStatus = row[Floors.uploadStatus],
            createdAt    = row[Floors.createdAt].toString(),
            updatedAt    = row[Floors.updatedAt].toString(),
        )
}
