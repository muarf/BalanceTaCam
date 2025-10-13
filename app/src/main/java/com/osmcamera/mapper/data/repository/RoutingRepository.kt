package com.osmcamera.mapper.data.repository

import android.util.Log
import com.osmcamera.mapper.data.api.OpenRouteServiceApi
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
    }
    
    /**
     * Calculate routes avoiding cameras
     * @return RouteComparison with multiple alternatives
     */
    suspend fun calculateAntiCameraRoutes(
        start: GeoPoint,
        end: GeoPoint
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
                val directRoute = calculateDirectRoute(start, end, cameras)
                Log.d(TAG, "Direct route: ${directRoute?.cameraCount} cameras")
                
                // 3. Calculate routes avoiding cameras
                val avoidingRoutes = calculateRoutesWithAvoidance(start, end, cameras)
                Log.d(TAG, "Calculated ${avoidingRoutes.size} avoiding routes")
                
                // 4. Combine and sort by camera count
                val allRoutes = (listOf(directRoute) + avoidingRoutes).filterNotNull()
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
        cameras: List<Camera>
    ): Route? {
        val request = ORSRouteRequest(
            coordinates = listOf(
                listOf(start.longitude, start.latitude),
                listOf(end.longitude, end.latitude)
            ),
            alternativeRoutes = null // Direct route only
        )
        
        return executeRouteRequest(request, cameras, "direct")
    }
    
    /**
     * Calculate routes with camera avoidance
     */
    private suspend fun calculateRoutesWithAvoidance(
        start: GeoPoint,
        end: GeoPoint,
        cameras: List<Camera>
    ): List<Route> {
        if (cameras.isEmpty()) {
            return emptyList()
        }
        
        // Create avoidance polygons around cameras
        val avoidPolygons = cameras.map { camera ->
            GeometryUtils.createAvoidanceCircle(
                center = GeoPoint(camera.latitude, camera.longitude),
                radiusMeters = CAMERA_AVOIDANCE_RADIUS,
                points = 8 // Octagon for performance
            )
        }
        
        // Combine all polygons into one MultiPolygon
        val multiPolygon = ORSPolygon(
            type = "Polygon",
            coordinates = listOf(avoidPolygons.flatten().chunked(2).map { listOf(it[0], it[1]) })
        )
        
        val request = ORSRouteRequest(
            coordinates = listOf(
                listOf(start.longitude, start.latitude),
                listOf(end.longitude, end.latitude)
            ),
            alternativeRoutes = null,
            options = ORSOptions(avoidPolygons = multiPolygon)
        )
        
        val route = executeRouteRequest(request, cameras, "avoiding")
        return listOfNotNull(route)
    }
    
    /**
     * Execute ORS request and parse response
     */
    private suspend fun executeRouteRequest(
        request: ORSRouteRequest,
        cameras: List<Camera>,
        routeType: String
    ): Route? {
        try {
            val response = orsApi.getRoute(
                profile = "driving-car",
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

