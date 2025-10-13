package com.osmcamera.mapper.data.repository

import android.util.Log
import com.osmcamera.mapper.data.api.OpenRouteServiceApi
import com.osmcamera.mapper.data.api.ORSAlternativeRoutes
import com.osmcamera.mapper.data.api.ORSOptions
import com.osmcamera.mapper.data.api.ORSPolygon
import com.osmcamera.mapper.data.api.ORSRouteRequest
import com.osmcamera.mapper.data.model.Camera
import com.osmcamera.mapper.data.model.Route
import com.osmcamera.mapper.data.model.RouteComparison
import com.osmcamera.mapper.data.model.RouteInstruction
import com.osmcamera.mapper.utils.GeometryUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for routing with camera avoidance
 */
@Singleton
class RoutingRepository @Inject constructor(
    private val orsApi: OpenRouteServiceApi,
    private val cameraRepository: CameraRepository
) {
    
    companion object {
        private const val TAG = "BalanceTaCam-Routing"
        private const val CAMERA_AVOIDANCE_RADIUS = 50.0 // meters
        private const val MAX_CAMERAS_TO_AVOID = 30 // Limit to avoid 413 error
    }
    
    /**
     * Calculate routes avoiding cameras
     * @return RouteComparison with multiple alternatives
     */
    suspend fun calculateAntiCameraRoutes(
        start: GeoPoint,
        end: GeoPoint,
        transportMode: String = "foot-walking" // foot-walking, driving-car, cycling-regular
    ): Result<RouteComparison> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "=== Calculating anti-camera routes ===")
                Log.d(TAG, "Start: ${start.latitude}, ${start.longitude}")
                Log.d(TAG, "End: ${end.latitude}, ${end.longitude}")
                
                // 1. Get cameras in the area (bounding box with padding)
                val cameras = getCamerasInArea(start, end)
                Log.d(TAG, "Found ${cameras.size} cameras in area")
                
                // 2. Calculate direct route (no avoidance)
                val directRoute = calculateDirectRoute(start, end, cameras, transportMode)
                Log.d(TAG, "Direct route: ${directRoute?.cameraCount} cameras")
                
                // 3. Calculate alternative routes (without avoid_polygons since ORS 500 error)
                val alternativeRoutes = calculateAlternativeRoutes(start, end, cameras, transportMode)
                Log.d(TAG, "Calculated ${alternativeRoutes.size} alternative routes")
                
                // 4. Combine and sort by camera count (best = fewer cameras)
                val allRoutes = (listOf(directRoute) + alternativeRoutes).filterNotNull()
                val sortedRoutes = allRoutes.sortedBy { it.cameraCount }
                
                val bestRoute = sortedRoutes.firstOrNull() 
                    ?: return@withContext Result.failure(Exception("No routes found"))
                
                val comparison = RouteComparison(
                    routes = sortedRoutes,
                    bestRoute = bestRoute,
                    directRoute = directRoute
                )
                
                Log.d(TAG, "Best route has ${bestRoute.cameraCount} cameras")
                Log.d(TAG, "=== Routing calculation complete ===")
                
                Result.success(comparison)
            } catch (e: Exception) {
                Log.e(TAG, "Routing failed", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get cameras in the area between start and end with padding
     */
    private suspend fun getCamerasInArea(start: GeoPoint, end: GeoPoint): List<Camera> {
        val padding = 0.05 // ~5 km padding
        
        val minLat = minOf(start.latitude, end.latitude) - padding
        val maxLat = maxOf(start.latitude, end.latitude) + padding
        val minLon = minOf(start.longitude, end.longitude) - padding
        val maxLon = maxOf(start.longitude, end.longitude) + padding
        
        val result = cameraRepository.fetchCamerasFromOverpass(
            south = minLat,
            west = minLon,
            north = maxLat,
            east = maxLon
        )
        
        return result.getOrNull() ?: emptyList()
    }
    
    /**
     * Calculate direct route without camera avoidance
     */
    private suspend fun calculateDirectRoute(
        start: GeoPoint,
        end: GeoPoint,
        cameras: List<Camera>,
        transportMode: String = "foot-walking"
    ): Route? {
        val request = ORSRouteRequest(
            coordinates = listOf(
                listOf(start.longitude, start.latitude),
                listOf(end.longitude, end.latitude)
            ),
            alternativeRoutes = null // Direct route only
        )
        
        return executeRouteRequest(request, cameras, "direct", transportMode)
    }
    
    /**
     * Calculate alternative routes (using ORS alternative_routes feature)
     * Then count cameras on each to find the best one
     */
    private suspend fun calculateAlternativeRoutes(
        start: GeoPoint,
        end: GeoPoint,
        cameras: List<Camera>,
        transportMode: String = "foot-walking"
    ): List<Route> {
        if (cameras.isEmpty()) {
            return emptyList()
        }
        
        // Request MORE alternative routes from ORS
        // shareFactor = how different routes must be (lower = more different)
        // weightFactor = accept longer routes for alternatives (higher = longer OK)
        val request = ORSRouteRequest(
            coordinates = listOf(
                listOf(start.longitude, start.latitude),
                listOf(end.longitude, end.latitude)
            ),
            alternativeRoutes = ORSAlternativeRoutes(
                targetCount = 10, // Request 10 alternatives (ORS will return what it can)
                shareFactor = 0.3, // Routes must share <30% of their path (very different)
                weightFactor = 2.5 // Accept routes up to 2.5x longer if they avoid cameras
            ),
            options = null // No avoidance - ORS gives alternatives naturally
        )
        
        Log.d(TAG, "Requesting up to 10 alternative routes (shareFactor=0.3)")
        
        return try {
            val response = orsApi.getRoute(
                profile = transportMode,
                apiKey = OpenRouteServiceApi.API_KEY,
                request = request
            )
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Alternative routes error: ${response.code()}")
                return emptyList()
            }
            
            val orsRoutes = response.body()?.routes ?: return emptyList()
            Log.d(TAG, "Got ${orsRoutes.size} total routes from ORS (including direct)")
            
            // Convert each ORS alternative route (skip first = direct) and count cameras
            val alternatives = orsRoutes.drop(1).mapIndexedNotNull { index, orsRoute ->
                val points = GeometryUtils.decodePolyline(orsRoute.geometry)
                val cameraCount = GeometryUtils.countCamerasNearRoute(
                    routePoints = points,
                    cameras = cameras,
                    radiusMeters = CAMERA_AVOIDANCE_RADIUS
                )
                
                Log.d(TAG, "Alternative route ${index + 1}: ${points.size} points, $cameraCount cameras, ${String.format("%.1f", orsRoute.summary.distance/1000)}km")
                
                Route(
                    id = "alt_$index",
                    points = points,
                    distance = orsRoute.summary.distance,
                    duration = orsRoute.summary.duration,
                    cameraCount = cameraCount
                )
            }
            
            Log.d(TAG, "Returning ${alternatives.size} alternative routes")
            alternatives
        } catch (e: Exception) {
            Log.e(TAG, "Alternative routes failed", e)
            emptyList()
        }
    }
    
    /**
     * Execute ORS request and parse response
     */
    private suspend fun executeRouteRequest(
        request: ORSRouteRequest,
        cameras: List<Camera>,
        routeType: String,
        transportMode: String = "foot-walking"
    ): Route? {
        try {
            val response = orsApi.getRoute(
                profile = transportMode,
                apiKey = OpenRouteServiceApi.API_KEY,
                request = request
            )
            
            if (!response.isSuccessful) {
                Log.e(TAG, "ORS API error: ${response.code()} - ${response.errorBody()?.string()}")
                return null
            }
            
            val orsRoutes = response.body()?.routes ?: return null
            if (orsRoutes.isEmpty()) return null
            
            val orsRoute = orsRoutes.first()
            
            // Decode geometry to points
            val points = GeometryUtils.decodePolyline(orsRoute.geometry)
            
            // Count cameras along route
            val cameraCount = GeometryUtils.countCamerasNearRoute(
                routePoints = points,
                cameras = cameras,
                radiusMeters = CAMERA_AVOIDANCE_RADIUS
            )
            
            Log.d(TAG, "Route $routeType: ${points.size} points, $cameraCount cameras")
            
            return Route(
                id = routeType,
                points = points,
                distance = orsRoute.summary.distance,
                duration = orsRoute.summary.duration,
                cameraCount = cameraCount
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error executing route request", e)
            return null
        }
    }
}

