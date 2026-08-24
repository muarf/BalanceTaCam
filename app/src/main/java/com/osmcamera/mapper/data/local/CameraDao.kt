package com.osmcamera.mapper.data.local

import androidx.room.*
import com.osmcamera.mapper.data.model.Camera
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Camera database operations
 */
@Dao
interface CameraDao {
    
    @Query("SELECT * FROM cameras")
    fun getAllCameras(): Flow<List<Camera>>
    
    @Query("SELECT * FROM cameras WHERE id = :id")
    suspend fun getCameraById(id: String): Camera?
    
    @Query("""
        SELECT * FROM cameras 
        WHERE latitude BETWEEN :minLat AND :maxLat 
        AND longitude BETWEEN :minLon AND :maxLon
    """)
    fun getCamerasInBounds(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double
    ): Flow<List<Camera>>
    
    @Query("""
        SELECT * FROM cameras 
        WHERE latitude BETWEEN :minLat AND :maxLat 
        AND longitude BETWEEN :minLon AND :maxLon
    """)
    suspend fun getCamerasInBoundsList(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double
    ): List<Camera>
    
    @Query("SELECT * FROM cameras")
    suspend fun getAllCamerasList(): List<Camera>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCamera(camera: Camera)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCameras(cameras: List<Camera>)
    
    @Update
    suspend fun updateCamera(camera: Camera)
    
    @Delete
    suspend fun deleteCamera(camera: Camera)
    
    @Query("DELETE FROM cameras")
    suspend fun deleteAllCameras()
    
    @Query("SELECT COUNT(*) FROM cameras")
    suspend fun getCameraCount(): Int
}


