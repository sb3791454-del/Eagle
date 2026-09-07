package com.aegis.cloak.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tactical_waypoints")
data class WaypointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val notes: String = "",
    val createdTimestamp: Long = System.currentTimeMillis()
)
