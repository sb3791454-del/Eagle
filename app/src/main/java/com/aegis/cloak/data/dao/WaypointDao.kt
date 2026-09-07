package com.aegis.cloak.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aegis.cloak.data.entity.WaypointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WaypointDao {
    @Query("SELECT * FROM tactical_waypoints ORDER BY createdTimestamp DESC")
    fun getAllWaypoints(): Flow<List<WaypointEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaypoint(waypoint: WaypointEntity): Long

    @Delete
    suspend fun deleteWaypoint(waypoint: WaypointEntity)

    @Query("SELECT COUNT(*) FROM tactical_waypoints")
    suspend fun getCount(): Int
}
