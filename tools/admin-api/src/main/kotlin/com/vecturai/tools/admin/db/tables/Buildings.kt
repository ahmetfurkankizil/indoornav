package com.Vectura AI.tools.admin.db.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

object Buildings : UUIDTable("buildings") {
    val managerId    = reference("manager_id", Managers)
    val name         = varchar("name", 255)
    val description  = text("description").nullable()
    val address      = text("address").nullable()
    val qrToken      = varchar("qr_token", 64).uniqueIndex()
    val qrImagePath  = varchar("qr_image_path", 512).nullable()
    val widthMeters  = double("width_meters").default(25.0)
    val status       = varchar("status", 20).default("draft")  // draft | published
    val createdAt    = timestamp("created_at")
    val updatedAt    = timestamp("updated_at")
}
