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
import com.osmcamera.mapper.utils.CameraClusterUtils
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
     * Calculate alternative routes by trying different parameters
     * to get the most variety and find routes with fewer cameras
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
        
        val allAlternatives = mutableListOf<Route>()
        
        // Try 3 different configurations to get more variety
        val configurations = listOf(
            ORSAlternativeRoutes(targetCount = 5, shareFactor = 0.3, weightFactor = 2.0),
            ORSAlternativeRoutes(targetCount = 5, shareFactor = 0.4, weightFactor = 2.5),
            ORSAlternativeRoutes(targetCount = 5, shareFactor = 0.5, weightFactor = 3.0)
        )
        
        configurations.forEachIndexed { configIndex, altConfig ->
            try {
                val request = ORSRouteRequest(
                    coordinates = listOf(
                        listOf(start.longitude, start.latitude),
                        listOf(end.longitude, end.latitude)
                    ),
                    alternativeRoutes = altConfig,
                    options = null
                )
                
                val response = orsApi.getRoute(
                    profile = transportMode,
                    apiKey = OpenRouteServiceApi.API_KEY,
                    request = request
                )
                
                if (response.isSuccessful) {
                    val orsRoutes = response.body()?.routes ?: emptyList()
                    Log.d(TAG, "Config $configIndex: Got ${orsRoutes.size} routes")
                    
                    // Skip first (direct) and convert others
                    orsRoutes.drop(1).forEach { orsRoute ->
                        val points = GeometryUtils.decodePolyline(orsRoute.geometry)
                        val cameraCount = GeometryUtils.countCamerasNearRoute(
                            routePoints = points,
                            cameras = cameras,
                            radiusMeters = CAMERA_AVOIDANCE_RADIUS
                        )
                        
                        val route = Route(
                            id = "alt_${configIndex}_${allAlternatives.size}",
                            points = points,
                            distance = orsRoute.summary.distance,
                            duration = orsRoute.summary.duration,
                            cameraCount = cameraCount
                        )
                        
                        // Only add if significantly different from existing routes
                        val isDuplicate = allAlternatives.any { existing ->
                            kotlin.math.abs(existing.distance - route.distance) < 50 &&
                            existing.cameraCount == route.cameraCount
                        }
                        
                        if (!isDuplicate) {
                            allAlternatives.add(route)
                            Log.d(TAG, "Added alternative: $cameraCount cameras, ${String.format("%.1f", orsRoute.summary.distance/1000)}km")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Config $configIndex failed", e)
            }
        }
        
        Log.d(TAG, "Total unique alternatives found: ${allAlternatives.size}")
        
        // FALLBACK: If ORS didn't give us alternatives, create detour routes manually
        if (allAlternatives.isEmpty()) {
            Log.w(TAG, "No alternatives from ORS, generating detour routes manually")
            return generateDetourRoutes(start, end, cameras, transportMode)
        }
        
        return allAlternatives
    }
    
    /**
     * Generate INTELLIGENT routes by adding waypoints to avoid camera zones
     * This analyzes camera positions and creates strategic detours
     */
    private suspend fun generateDetourRoutes(
        start: GeoPoint,
        end: GeoPoint,
        cameras: List<Camera>,
        transportMode: String
    ): List<Route> {
        Log.d(TAG, "=== Generating intelligent detour routes ===")
        
        // 1. Find camera clusters (hotspots)
        val clusters = CameraClusterUtils.findCameraClusters(
            cameras = cameras,
            maxDistanceMeters = 150.0,
            minCamerasPerCluster = 3
        )
        
        Log.d(TAG, "Found ${clusters.size} camera clusters")
        clusters.take(5).forEach { cluster ->
            Log.d(TAG, "  Cluster: ${cluster.cameraCount} cameras at (${String.format("%.4f", cluster.center.latitude)}, ${String.format("%.4f", cluster.center.longitude)})")
        }
        
        // 2. Generate intelligent waypoints that avoid clusters
        val waypoints = CameraClusterUtils.generateAvoidanceWaypoints(
            start = start,
            end = end,
            clusters = clusters,
            distances = listOf(200.0, 400.0, 600.0)
        )
        
        Log.d(TAG, "Generated ${waypoints.size} intelligent waypoints")
        
        // 3. Calculate routes via each waypoint
        val routes = mutableListOf<Route>()
        
        waypoints.take(10).forEachIndexed { index, waypoint ->
            try {
                val request = ORSRouteRequest(
                    coordinates = listOf(
                        listOf(start.longitude, start.latitude),
                        listOf(waypoint.longitude, waypoint.latitude),
                        listOf(end.longitude, end.latitude)
                    ),
                    alternativeRoutes = null
                )
                
                val response = orsApi.getRoute(
                    profile = transportMode,
                    apiKey = OpenRouteServiceApi.API_KEY,
                    request = request
                )
                
                if (response.isSuccessful) {
                    val orsRoute = response.body()?.routes?.firstOrNull()
                    if (orsRoute != null) {
                        val points = GeometryUtils.decodePolyline(orsRoute.geometry)
                        val cameraCount = GeometryUtils.countCamerasNearRoute(
                            routePoints = points,
                            cameras = cameras,
                            radiusMeters = CAMERA_AVOIDANCE_RADIUS
                        )
                        
                        routes.add(Route(
                            id = "intelligent_$index",
                            points = points,
                            distance = orsRoute.summary.distance,
                            duration = orsRoute.summary.duration,
                            cameraCount = cameraCount
                        ))
                        
                        Log.d(TAG, "Smart detour $index: $cameraCount cameras, ${String.format("%.1f", orsRoute.summary.distance/1000)}km")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Smart detour $index failed", e)
            }
        }
        
        Log.d(TAG, "Generated ${routes.size} intelligent detour routes")
        return routes.sortedBy { it.cameraCount }.take(5) // Keep best 5
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

