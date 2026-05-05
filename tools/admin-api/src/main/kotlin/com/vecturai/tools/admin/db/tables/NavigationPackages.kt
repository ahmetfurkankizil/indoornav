package com.vecturai.tools.admin.db.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

object NavigationPackages : UUIDTable("navigation_packages") {
    val buildingId   = reference("building_id", Buildings)
    val version      = integer("version").default(1)
    val checksum     = varchar("checksum", 64)   // SHA-256 hex of packageJson
    val isCurrent    = bool("is_current").default(true)
    val generatedAt  = timestamp("generated_at")
    // Full Dijkstra-ready JSON — NEVER returned by manager-facing API routes
    val packageJson  = text("package_json")
}
