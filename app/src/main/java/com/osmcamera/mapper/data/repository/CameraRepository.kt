package com.osmcamera.mapper.data.repository

import com.osmcamera.mapper.data.api.OverpassApiService
import com.osmcamera.mapper.data.api.OverpassResponseParser
import com.osmcamera.mapper.data.local.CameraDao
import com.osmcamera.mapper.data.model.Camera
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for camera data operations
 */
@Singleton
class CameraRepository @Inject constructor(
    private val cameraDao: CameraDao,
    private val overpassApiService: OverpassApiService
) {
    
    /**
     * Get all cameras from local database
     */
    fun getAllCameras(): Flow<List<Camera>> {
        return cameraDao.getAllCameras()
    }
    
    /**
     * Get cameras in bounding box from local database
     */
    fun getCamerasInBounds(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double
    ): Flow<List<Camera>> {
        return cameraDao.getCamerasInBounds(minLat, maxLat, minLon, maxLon)
    }
    
    /**
     * Fetch cameras from Overpass API and cache them
     */
    suspend fun fetchCamerasFromOverpass(
        south: Double,
        west: Double,
        north: Double,
        east: Double
    ): Result<List<Camera>> {
        return withContext(Dispatchers.IO) {
            try {
                val query = OverpassApiService.buildCameraQuery(south, west, north, east)
                val response = overpassApiService.query(query)
                
                if (response.isSuccessful) {
                    val body = response.body()?.string() ?: ""
                    val cameras = OverpassResponseParser.parseCameras(body)
                    
                    // Cache cameras in local database
                    cameraDao.insertCameras(cameras)
                    
                    Result.success(cameras)
                } else {
                    Result.failure(Exception("Overpass API error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get camera by ID
     */
    suspend fun getCameraById(id: String): Camera? {
        return withContext(Dispatchers.IO) {
            cameraDao.getCameraById(id)
        }
    }
    
    /**
     * Insert camera into local database
     */
    suspend fun insertCamera(camera: Camera) {
        withContext(Dispatchers.IO) {
            cameraDao.insertCamera(camera)
        }
    }
    
    /**
     * Update camera
     */
    suspend fun updateCamera(camera: Camera) {
        withContext(Dispatchers.IO) {
            cameraDao.updateCamera(camera)
        }
    }
    
    /**
     * Delete camera
     */
    suspend fun deleteCamera(camera: Camera) {
        withContext(Dispatchers.IO) {
            cameraDao.deleteCamera(camera)
        }
    }
    
    /**
     * Get camera count
     */
    suspend fun getCameraCount(): Int {
        return withContext(Dispatchers.IO) {
            cameraDao.getCameraCount()
        }
    }
}


