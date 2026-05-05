package com.vecturai.tools.admin.db.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

object NavMeshAreas : UUIDTable("navmesh_areas") {
    val floorId    = reference("floor_id", Floors)
    val buildingId = reference("building_id", Buildings)
    val label      = varchar("label", 100).default("Walkable Area")
    val vertices   = text("vertices").default("[]")  // JSON: [{x, y}, ...] canvas 0-1 coords
    val createdAt  = timestamp("created_at")
    val updatedAt  = timestamp("updated_at")
}
