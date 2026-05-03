package com.Vectura AI.tools.admin.service

import com.Vectura AI.tools.admin.db.tables.Buildings
import com.Vectura AI.tools.admin.db.tables.FloorConnections
import com.Vectura AI.tools.admin.model.FloorConnectionResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

class FloorConnectionService {

    suspend fun create(
        buildingId: String,
        managerId: String,
        fromNodeId: String,
        toNodeId: String,
        connectionType: String,
        isBidirectional: Boolean,
    ): FloorConnectionResponse? = withContext(Dispatchers.IO) {
        transaction {
            Buildings.selectAll()
                .where {
                    (Buildings.id eq UUID.fromString(buildingId)) and
                        (Buildings.managerId eq UUID.fromString(managerId))
                }
                .singleOrNull() ?: return@transaction null

            val now = Instant.now()
            val id = FloorConnections.insertAndGetId {
                it[FloorConnections.buildingId]      = UUID.fromString(buildingId)
                it[FloorConnections.fromNodeId]      = UUID.fromString(fromNodeId)
                it[FloorConnections.toNodeId]        = UUID.fromString(toNodeId)
                it[FloorConnections.connectionType]  = connectionType
                it[FloorConnections.isBidirectional] = isBidirectional
                it[FloorConnections.createdAt]       = now
            }

            FloorConnections.selectAll()
                .where { FloorConnections.id eq id }
                .single()
                .let { rowToResponse(it) }
        }
    }

    suspend fun list(buildingId: String, managerId: String): List<FloorConnectionResponse>? =
        withContext(Dispatchers.IO) {
            transaction {
                Buildings.selectAll()
                    .where {
                        (Buildings.id eq UUID.fromString(buildingId)) and
                            (Buildings.managerId eq UUID.fromString(managerId))
                    }
                    .singleOrNull() ?: return@transaction null

                FloorConnections.selectAll()
                    .where { FloorConnections.buildingId eq UUID.fromString(buildingId) }
                    .map { rowToResponse(it) }
            }
        }

    suspend fun delete(connectionId: String, managerId: String): Boolean =
        withContext(Dispatchers.IO) {
            transaction {
                val row = FloorConnections.innerJoin(Buildings)
                    .selectAll()
                    .where {
                        (FloorConnections.id eq UUID.fromString(connectionId)) and
                            (Buildings.managerId eq UUID.fromString(managerId))
                    }
                    .singleOrNull() ?: return@transaction false

                FloorConnections.deleteWhere {
                    FloorConnections.id eq UUID.fromString(connectionId)
                } > 0
            }
        }

    private fun rowToResponse(row: ResultRow): FloorConnectionResponse = FloorConnectionResponse(
        id             = row[FloorConnections.id].value.toString(),
        buildingId     = row[FloorConnections.buildingId].value.toString(),
        fromNodeId     = row[FloorConnections.fromNodeId].value.toString(),
        toNodeId       = row[FloorConnections.toNodeId].value.toString(),
        connectionType = row[FloorConnections.connectionType],
        isBidirectional = row[FloorConnections.isBidirectional],
        createdAt      = row[FloorConnections.createdAt].toString(),
    )
}
