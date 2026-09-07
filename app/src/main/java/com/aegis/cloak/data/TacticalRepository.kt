package com.aegis.cloak.data

import com.aegis.cloak.data.entity.AuditLogEntity
import com.aegis.cloak.data.entity.WaypointEntity
import kotlinx.coroutines.flow.Flow

class TacticalRepository(private val database: AegisDatabase) {
    val auditLogs: Flow<List<AuditLogEntity>> = database.auditLogDao().getAllLogs()
    val waypoints: Flow<List<WaypointEntity>> = database.waypointDao().getAllWaypoints()

    suspend fun logEvent(category: String, severity: String, message: String, details: String = "") {
        database.auditLogDao().insertLog(
            AuditLogEntity(
                category = category,
                severity = severity,
                message = message,
                details = details
            )
        )
        database.auditLogDao().trimLogs()
    }

    suspend fun saveWaypoint(name: String, latitude: Double, longitude: Double, altitude: Double, notes: String = "") {
        database.waypointDao().insertWaypoint(
            WaypointEntity(
                name = name,
                latitude = latitude,
                longitude = longitude,
                altitude = altitude,
                notes = notes
            )
        )
    }

    suspend fun deleteWaypoint(waypoint: WaypointEntity) {
        database.waypointDao().deleteWaypoint(waypoint)
    }

    suspend fun clearLogs() {
        database.auditLogDao().clearAll()
    }
}
