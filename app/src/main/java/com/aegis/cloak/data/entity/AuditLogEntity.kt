package com.aegis.cloak.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val category: String, // "CLOAK_ENGINE", "VPN_SANITIZER", "RADIO_SENTRY", "KINEMATIC"
    val severity: String, // "INFO", "WARN", "CRITICAL"
    val message: String,
    val details: String = ""
)
