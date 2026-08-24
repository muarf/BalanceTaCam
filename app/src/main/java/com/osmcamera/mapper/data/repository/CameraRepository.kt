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
     * Get cameras in bounding box as direct list from Room cache
     */
    suspend fun getCamerasInBoundsList(
        south: Double,
        west: Double,
        north: Double,
        east: Double
    ): List<Camera> {
        return withContext(Dispatchers.IO) {
            val minLat = minOf(south, north)
            val maxLat = maxOf(south, north)
            val minLon = minOf(west, east)
            val maxLon = maxOf(west, east)
            cameraDao.getCamerasInBoundsList(minLat, maxLat, minLon, maxLon)
        }
    }

    /**
     * Get all cameras as direct list from Room cache
     */
    suspend fun getAllCamerasList(): List<Camera> {
        return withContext(Dispatchers.IO) {
            cameraDao.getAllCamerasList()
        }
    }

    /**
     * Get cameras for routing with zero-latency local cache priority.
     */
    suspend fun getCamerasForRouting(
        south: Double,
        west: Double,
        north: Double,
        east: Double
    ): List<Camera> {
        return withContext(Dispatchers.IO) {
            val minLat = minOf(south, north)
            val maxLat = maxOf(south, north)
            val minLon = minOf(west, east)
            val maxLon = maxOf(west, east)
            
            // Check local Room cache first (instant response)
            val localCameras = cameraDao.getCamerasInBoundsList(minLat, maxLat, minLon, maxLon)
            if (localCameras.isNotEmpty()) {
                android.util.Log.d("BalanceTaCam", "Routing: using ${localCameras.size} cached cameras from Room database")
                return@withContext localCameras
            }
            
            // Fallback: if bounds query returned 0, try all local cameras
            val allLocal = cameraDao.getAllCamerasList()
            if (allLocal.isNotEmpty()) {
                val filtered = allLocal.filter {
                    it.latitude in minLat..maxLat && it.longitude in minLon..maxLon
                }
                if (filtered.isNotEmpty()) {
                    return@withContext filtered
                }
            }
            
            // Fallback to Overpass network fetch if local DB is completely empty
            fetchCamerasFromOverpass(minLat, minLon, maxLat, maxLon).getOrDefault(emptyList())
        }
    }

    /**
     * Fetch cameras from Overpass API and cache them.
     * Tries POST first, then GET, across all active mirror endpoints.
     */
    suspend fun fetchCamerasFromOverpass(
        south: Double,
        west: Double,
        north: Double,
        east: Double
    ): Result<List<Camera>> {
        return withContext(Dispatchers.IO) {
            val query = OverpassApiService.buildCameraQuery(south, west, north, east)
            var lastError: Exception? = null
            
            for (endpoint in OverpassApiService.ENDPOINTS) {
                try {
                    android.util.Log.d("BalanceTaCam", "Fetching cameras from $endpoint")
                    
                    // Try POST first
                    var response = try {
                        overpassApiService.queryPost(endpoint, query)
                    } catch (e: Exception) {
                        null
                    }
                    
                    // Fallback to GET if POST failed
                    if (response == null || !response.isSuccessful) {
                        response = overpassApiService.query(endpoint, query)
                    }
                    
                    if (response.isSuccessful) {
                        val body = response.body()?.string() ?: ""
                        val cameras = OverpassResponseParser.parseCameras(body)
                        android.util.Log.d("BalanceTaCam", "Got ${cameras.size} cameras from $endpoint")
                        
                        // Cache cameras in local database
                        cameraDao.insertCameras(cameras)
                        
                        return@withContext Result.success(cameras)
                    } else {
                        android.util.Log.e("BalanceTaCam", "Mirror failed $endpoint: ${response.code()}")
                        lastError = Exception("Overpass API error: ${response.code()} on $endpoint")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("BalanceTaCam", "Mirror unreachable $endpoint: ${e.message}")
                    lastError = e
                }
            }
            
            // Fallback: check local database cache if network requests failed
            val localCameras = cameraDao.getCamerasInBoundsList(
                minLat = minOf(south, north),
                maxLat = maxOf(south, north),
                minLon = minOf(west, east),
                maxLon = maxOf(west, east)
            )
            if (localCameras.isNotEmpty()) {
                android.util.Log.d("BalanceTaCam", "Overpass mirrors failed, retrieved ${localCameras.size} cameras from local cache")
                return@withContext Result.success(localCameras)
            }
            
            Result.failure(lastError ?: Exception("No Overpass endpoint available"))
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


