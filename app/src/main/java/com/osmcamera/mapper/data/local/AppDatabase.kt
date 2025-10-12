package com.osmcamera.mapper.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.osmcamera.mapper.data.model.Camera

/**
 * Room Database for the app
 */
@Database(
    entities = [Camera::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cameraDao(): CameraDao
    
    companion object {
        const val DATABASE_NAME = "osm_camera_mapper_db"
    }
}


