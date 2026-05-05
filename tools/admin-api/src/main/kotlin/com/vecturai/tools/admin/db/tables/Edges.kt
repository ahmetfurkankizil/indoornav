package com.vecturai.tools.admin.db.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

object Edges : UUIDTable("edges") {
    val floorId         = reference("floor_id", Floors)
    val buildingId      = reference("building_id", Buildings)
    val fromNodeId      = reference("from_node_id", Nodes)
    val toNodeId        = reference("to_node_id", Nodes)
    val isBidirectional = bool("is_bidirectional").default(true)
    val distance        = double("distance").nullable()
    val edgeType        = varchar("edge_type", 20).default("walk")  // walk | stairs | elevator
    val waypoints       = text("waypoints").default("[]")  // JSON array of {x, y}
    val createdBy       = varchar("created_by", 10).default("human")  // human | ai
    val createdAt       = timestamp("created_at")
    val updatedAt       = timestamp("updated_at")
}
