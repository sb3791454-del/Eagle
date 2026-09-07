package com.aegis.cloak.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aegis.cloak.data.dao.AuditLogDao
import com.aegis.cloak.data.dao.WaypointDao
import com.aegis.cloak.data.entity.AuditLogEntity
import com.aegis.cloak.data.entity.WaypointEntity

@Database(
    entities = [AuditLogEntity::class, WaypointEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AegisDatabase : RoomDatabase() {
    abstract fun auditLogDao(): AuditLogDao
    abstract fun waypointDao(): WaypointDao

    companion object {
        @Volatile
        private var INSTANCE: AegisDatabase? = null

        fun getInstance(context: Context): AegisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AegisDatabase::class.java,
                    "aegis_cloak_secure.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
