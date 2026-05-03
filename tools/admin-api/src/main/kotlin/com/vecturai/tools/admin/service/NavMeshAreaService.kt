package com.Vectura AI.tools.admin.service

import com.Vectura AI.tools.admin.db.tables.Buildings
import com.Vectura AI.tools.admin.db.tables.Floors
import com.Vectura AI.tools.admin.db.tables.NavMeshAreas
import com.Vectura AI.tools.admin.model.NavMeshAreaResponse
import com.Vectura AI.tools.admin.model.Waypoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

class NavMeshAreaService {

    suspend fun list(floorId: String, managerId: String): List<NavMeshAreaResponse>? =
        withContext(Dispatchers.IO) {
            transaction {
                Floors.innerJoin(Buildings)
                    .selectAll()
                    .where {
                        (Floors.id eq UUID.fromString(floorId)) and
                            (Buildings.managerId eq UUID.fromString(managerId))
                    }
                    .singleOrNull() ?: return@transaction null

                NavMeshAreas.selectAll()
                    .where { NavMeshAreas.floorId eq UUID.fromString(floorId) }
                    .map { rowToResponse(it) }
            }
        }

    suspend fun create(
        floorId: String,
        managerId: String,
        label: String,
        vertices: List<Waypoint>,
    ): NavMeshAreaResponse? = withContext(Dispatchers.IO) {
        transaction {
            val floorRow = Floors.innerJoin(Buildings)
                .selectAll()
                .where {
                    (Floors.id eq UUID.fromString(floorId)) and
                        (Buildings.managerId eq UUID.fromString(managerId))
                }
                .singleOrNull() ?: return@transaction null

            val buildingId = floorRow[Floors.buildingId].value.toString()
            val now = Instant.now()

            val id = NavMeshAreas.insertAndGetId {
                it[NavMeshAreas.floorId]    = UUID.fromString(floorId)
                it[NavMeshAreas.buildingId] = UUID.fromString(buildingId)
                it[NavMeshAreas.label]      = label
                it[NavMeshAreas.vertices]   = Json.encodeToString(vertices)
                it[NavMeshAreas.createdAt]  = now
                it[NavMeshAreas.updatedAt]  = now
            }

            NavMeshAreas.selectAll().where { NavMeshAreas.id eq id }.single().let { rowToResponse(it) }
        }
    }

    suspend fun update(
        areaId: String,
        managerId: String,
        label: String?,
        vertices: List<Waypoint>?,
    ): NavMeshAreaResponse? = withContext(Dispatchers.IO) {
        transaction {
            NavMeshAreas
                .innerJoin(Floors, { NavMeshAreas.floorId }, { Floors.id })
                .innerJoin(Buildings, { Floors.buildingId }, { Buildings.id })
                .selectAll()
                .where {
                    (NavMeshAreas.id eq UUID.fromString(areaId)) and
                        (Buildings.managerId eq UUID.fromString(managerId))
                }
                .singleOrNull() ?: return@transaction null

            NavMeshAreas.update({ NavMeshAreas.id eq UUID.fromString(areaId) }) {
                label?.let    { v -> it[NavMeshAreas.label]    = v }
                vertices?.let { v -> it[NavMeshAreas.vertices] = Json.encodeToString(v) }
                it[NavMeshAreas.updatedAt] = Instant.now()
            }

            NavMeshAreas.selectAll().where { NavMeshAreas.id eq UUID.fromString(areaId) }
                .single().let { rowToResponse(it) }
        }
    }

    suspend fun delete(areaId: String, managerId: String): Boolean =
        withContext(Dispatchers.IO) {
            transaction {
                NavMeshAreas
                    .innerJoin(Floors, { NavMeshAreas.floorId }, { Floors.id })
                    .innerJoin(Buildings, { Floors.buildingId }, { Buildings.id })
                    .selectAll()
                    .where {
                        (NavMeshAreas.id eq UUID.fromString(areaId)) and
                            (Buildings.managerId eq UUID.fromString(managerId))
                    }
                    .singleOrNull() ?: return@transaction false

                NavMeshAreas.deleteWhere { NavMeshAreas.id eq UUID.fromString(areaId) } > 0
            }
        }

    private fun rowToResponse(row: ResultRow) = NavMeshAreaResponse(
        id         = row[NavMeshAreas.id].value.toString(),
        floorId    = row[NavMeshAreas.floorId].value.toString(),
        buildingId = row[NavMeshAreas.buildingId].value.toString(),
        label      = row[NavMeshAreas.label],
        vertices   = Json.decodeFromString(row[NavMeshAreas.vertices]),
        createdAt  = row[NavMeshAreas.createdAt].toString(),
        updatedAt  = row[NavMeshAreas.updatedAt].toString(),
    )
}
