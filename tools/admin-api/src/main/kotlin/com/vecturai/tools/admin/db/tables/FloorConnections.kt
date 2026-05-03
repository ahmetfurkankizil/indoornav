package com.Vectura AI.tools.admin.db.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

object FloorConnections : UUIDTable("floor_connections") {
    val buildingId      = reference("building_id", Buildings)
    val fromNodeId      = reference("from_node_id", Nodes)
    val toNodeId        = reference("to_node_id", Nodes)
    val connectionType  = varchar("connection_type", 20)  // stairs | elevator | escalator | ramp
    val isBidirectional = bool("is_bidirectional").default(true)
    val createdAt       = timestamp("created_at")
}
