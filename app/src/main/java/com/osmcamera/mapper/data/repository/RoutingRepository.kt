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
import com.osmcamera.mapper.utils.CameraClusterUtils
import com.osmcamera.mapper.utils.GeometryUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Repository for routing with camera avoidance
 */
@Singleton
class RoutingRepository @Inject constructor(
    private val orsApi: OpenRouteServiceApi,
    private val cameraRepository: CameraRepository,
    private val preferencesManager: com.osmcamera.mapper.data.local.PreferencesManager,
    private val offlineEngine: com.osmcamera.mapper.offline.OfflineRoutingEngine,
    private val regionManager: com.osmcamera.mapper.offline.OfflineRegionManager
) {
    
    companion object {
        private const val TAG = "BalanceTaCam-Routing"
        private const val CAMERA_AVOIDANCE_RADIUS = 40.0 // meters (expanded avoidance margin)
    }
    
    /**
     * Calculate routes avoiding cameras
     * @return RouteComparison with multiple alternatives ranked by fewest cameras
     */
    suspend fun calculateAntiCameraRoutes(
        start: GeoPoint,
        end: GeoPoint,
        transportMode: String = "foot-walking",
        avoidanceRadius: Double = 40.0
    ): Result<RouteComparison> {
        // Offline mode: embedded GraphHopper, zero network
        if (preferencesManager.offlineMode.first()) {
            return calculateOfflineRoutes(start, end, avoidanceRadius)
        }
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "=== Calculating anti-camera routes (radius: ${avoidanceRadius}m) ===")
                Log.d(TAG, "Start: ${start.latitude}, ${start.longitude}")
                Log.d(TAG, "End: ${end.latitude}, ${end.longitude}")
                
                // 1. Get cameras in the area, filtering strictly for PUBLIC surveillance only
                val allCameras = getCamerasInArea(start, end)
                val cameras = allCameras.filter { it.surveillance == "public" || it.surveillance == null }
                Log.i(TAG, "Loaded ${allCameras.size} total cameras, filtering to ${cameras.size} PUBLIC surveillance cameras")
                
                // 1. Calculate direct route
                val directRoute = calculateDirectRoute(start, end, cameras, transportMode, avoidanceRadius)
                
                // 2. Dynamic polygon avoidance (focused high-priority avoidance configs)
                val polygonRoutes = calculatePolygonAvoidanceRoutes(
                    start = start,
                    end = end,
                    directRoutePoints = directRoute?.points ?: emptyList(),
                    cameras = cameras,
                    transportMode = transportMode,
                    avoidanceRadius = avoidanceRadius
                )
                
                Log.i(TAG, "Calculated ${polygonRoutes.size} anti-camera polygon routes (direct route has ${directRoute?.cameraCount ?: 0} cameras)")
                
                // 3. Combine all candidate routes
                val candidateRoutes = mutableListOf<Route>()
                directRoute?.let { candidateRoutes.add(it) }
                candidateRoutes.addAll(polygonRoutes)
                
                if (candidateRoutes.isEmpty()) {
                    return@withContext Result.failure(Exception("Aucun itinéraire trouvé"))
                }
                
                // 4. Iterative Refinement: If best route still has cameras, run a refinement pass targeting residual cameras
                val currentBest = candidateRoutes.minByOrNull { it.cameraCount }
                if (currentBest != null && currentBest.cameraCount > 0) {
                    val residualCameras = GeometryUtils.getCamerasAlongRoute(currentBest.points, cameras, radiusMeters = avoidanceRadius)
                    if (residualCameras.isNotEmpty()) {
                        val directRouteCameras = if (directRoute?.points?.isNotEmpty() == true) {
                            GeometryUtils.getCamerasAlongRoute(directRoute.points, cameras, radiusMeters = avoidanceRadius)
                        } else emptyList()
                        
                        val refinedCameras = (directRouteCameras + residualCameras).distinctBy { it.id }
                        val refinedPolygon = CameraClusterUtils.createAvoidanceMultiPolygon(refinedCameras, start, end, radiusMeters = avoidanceRadius)
                        if (refinedPolygon != null) {
                            val refinedRoutes = calculateSingleAvoidance(start, end, refinedPolygon, cameras, transportMode, "refined", avoidanceRadius)
                            candidateRoutes.addAll(refinedRoutes)
                        }
                    }
                }
                
                // 5. Deduplicate similar routes (similar distance and same camera count)
                val uniqueRoutes = mutableListOf<Route>()
                candidateRoutes.forEach { route ->
                    val isDuplicate = uniqueRoutes.any { existing ->
                        abs(existing.distance - route.distance) < 35 &&
                        existing.cameraCount == route.cameraCount
                    }
                    if (!isDuplicate) {
                        uniqueRoutes.add(route)
                    }
                }
                
                // 6. Sort by lowest camera count first, then shortest distance
                val sortedRoutes = uniqueRoutes.sortedWith(
                    compareBy<Route> { it.cameraCount }
                        .thenBy { it.distance }
                )
                
                val bestRoute = sortedRoutes.first()
                
                val comparison = RouteComparison(
                    routes = sortedRoutes.take(5),
                    bestRoute = bestRoute,
                    directRoute = directRoute
                )
                
                Log.d(TAG, "Best route has ${bestRoute.cameraCount} cameras (Direct had ${directRoute?.cameraCount})")
                Log.d(TAG, "=== Routing calculation complete ===")
                
                Result.success(comparison)
            } catch (e: Exception) {
                Log.e(TAG, "Routing failed", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Fully offline routing: embedded GraphHopper + local Room cameras
     */
    private suspend fun calculateOfflineRoutes(
        start: GeoPoint,
        end: GeoPoint,
        avoidanceRadius: Double
    ): Result<RouteComparison> = withContext(Dispatchers.IO) {
        try {
            val regionId = regionManager.findInstalledRegionFor(start.latitude, start.longitude)
                ?: regionManager.findInstalledRegionFor(end.latitude, end.longitude)
            if (regionId == null) {
                return@withContext Result.failure(Exception(
                    "Aucune région hors-ligne installée ici. Téléchargez une région dans Réglages."
                ))
            }
            val graphDir = regionManager.graphCacheDir(regionId)
                ?: return@withContext Result.failure(Exception("Graphe $regionId introuvable"))

            if (!offlineEngine.ensureLoaded(graphDir, regionId)) {
                return@withContext Result.failure(Exception("Échec du chargement du graphe hors-ligne"))
            }

            // Cameras from Room only (no Overpass)
            val padding = 0.04
            val cameras = cameraRepository.getCamerasInBoundsList(
                minOf(start.latitude, end.latitude) - padding,
                minOf(start.longitude, end.longitude) - padding,
                maxOf(start.latitude, end.latitude) + padding,
                maxOf(start.longitude, end.longitude) + padding
            ).filter { it.surveillance == "public" || it.surveillance == null }

            Log.i(TAG, "[OFFLINE] ${cameras.size} caméras locales, région $regionId")

            offlineEngine.calculateAntiCameraRoutes(start, end, cameras)
        } catch (e: Exception) {
            Log.e(TAG, "[OFFLINE] Échec", e)
            Result.failure(e)
        }
    }

    /**
     * Get cameras in the area between start and end with padding
     */
    private suspend fun getCamerasInArea(start: GeoPoint, end: GeoPoint): List<Camera> {
        val padding = 0.04 // ~4 km padding
        
        val minLat = minOf(start.latitude, end.latitude) - padding
        val maxLat = maxOf(start.latitude, end.latitude) + padding
        val minLon = minOf(start.longitude, end.longitude) - padding
        val maxLon = maxOf(start.longitude, end.longitude) + padding
        
        return cameraRepository.getCamerasForRouting(
            south = minLat,
            west = minLon,
            north = maxLat,
            east = maxLon
        )
    }
    
    /**
     * Calculate direct route without camera avoidance
     */
    private suspend fun calculateDirectRoute(
        start: GeoPoint,
        end: GeoPoint,
        cameras: List<Camera>,
        transportMode: String = "foot-walking",
        avoidanceRadius: Double = 40.0
    ): Route? {
        val request = ORSRouteRequest(
            coordinates = listOf(
                listOf(start.longitude, start.latitude),
                listOf(end.longitude, end.latitude)
            ),
            alternativeRoutes = null
        )
        
        return executeRouteRequest(request, cameras, "direct", transportMode, avoidanceRadius)
    }
    
    /**
     * Calculate standard ORS alternative routes
     */
    private suspend fun calculateORSAlternatives(
        start: GeoPoint,
        end: GeoPoint,
        cameras: List<Camera>,
        transportMode: String = "foot-walking"
    ): List<Route> {
        if (cameras.isEmpty()) return emptyList()
        
        val alternatives = mutableListOf<Route>()
        try {
            val request = ORSRouteRequest(
                coordinates = listOf(
                    listOf(start.longitude, start.latitude),
                    listOf(end.longitude, end.latitude)
                ),
                alternativeRoutes = ORSAlternativeRoutes(
                    targetCount = 3,
                    shareFactor = 0.6,
                    weightFactor = 1.6
                ),
                options = null
            )
            
            val response = orsApi.getRoute(
                profile = transportMode,
                apiKey = OpenRouteServiceApi.API_KEY,
                request = request
            )
            
            if (response.isSuccessful) {
                val orsRoutes = response.body()?.routes ?: emptyList()
                // Drop the first route (which is direct) and convert alternatives
                orsRoutes.drop(1).forEachIndexed { idx, orsRoute ->
                    val points = GeometryUtils.decodePolyline(orsRoute.geometry)
                    val cameraCount = GeometryUtils.countCamerasNearRoute(
                        routePoints = points,
                        cameras = cameras,
                        radiusMeters = CAMERA_AVOIDANCE_RADIUS
                    )
                    
                    alternatives.add(Route(
                        id = "ors_alt_$idx",
                        points = points,
                        distance = orsRoute.summary.distance,
                        duration = orsRoute.summary.duration,
                        cameraCount = cameraCount
                    ))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "ORS alternatives query failed", e)
        }
        
        return alternatives
    }
    
    /**
     * Generate routes dynamically avoiding camera zones via ORS avoid_polygons
     */
    private suspend fun calculatePolygonAvoidanceRoutes(
        start: GeoPoint,
        end: GeoPoint,
        directRoutePoints: List<GeoPoint>,
        cameras: List<Camera>,
        transportMode: String,
        avoidanceRadius: Double = 40.0
    ): List<Route> = coroutineScope {
        if (cameras.isEmpty()) return@coroutineScope emptyList()
        
        // 1. Identify cameras directly along the direct path
        val directRouteCameras = if (directRoutePoints.isNotEmpty()) {
            GeometryUtils.getCamerasAlongRoute(directRoutePoints, cameras, radiusMeters = avoidanceRadius)
        } else {
            emptyList()
        }
        
        // 2. Identify dense clusters in the corridor
        val clusters = CameraClusterUtils.findCameraClusters(cameras, maxDistanceMeters = 100.0, minCamerasPerCluster = 2)
        val clusterCameras = clusters.flatMap { it.cameras }.distinctBy { it.id }
        
        // 3. Identify all cameras in the navigation corridor between start and end (prioritizing direct path and public cameras)
        val corridorCameras = CameraClusterUtils.getCamerasInCorridor(
            cameras = cameras,
            start = start,
            end = end,
            directRoutePoints = directRoutePoints,
            paddingMeters = 600.0
        )
        
        // 4. Define multi-polygon configurations to test in parallel around the user-specified radius
        val radiusBase = avoidanceRadius
        val radiusTight = maxOf(18.0, avoidanceRadius - 6.0)
        val radiusWide = avoidanceRadius + 5.0
        
        val configs = listOfNotNull(
            // Config A: Avoid ALL corridor cameras (user-selected radius)
            if (corridorCameras.isNotEmpty()) {
                CameraClusterUtils.createAvoidanceMultiPolygon(corridorCameras, start, end, radiusMeters = radiusBase, maxPolygons = 85)
            } else null,
            // Config B: Avoid ALL corridor cameras (tighter radius for dense areas)
            if (corridorCameras.isNotEmpty()) {
                CameraClusterUtils.createAvoidanceMultiPolygon(corridorCameras, start, end, radiusMeters = radiusTight, maxPolygons = 85)
            } else null,
            // Config C: Avoid ALL corridor cameras (wider radius for extra margin)
            if (corridorCameras.isNotEmpty()) {
                CameraClusterUtils.createAvoidanceMultiPolygon(corridorCameras, start, end, radiusMeters = radiusWide, maxPolygons = 85)
            } else null,
            // Config D: Avoid union of direct cameras + clusters
            if (directRouteCameras.isNotEmpty() || clusterCameras.isNotEmpty()) {
                val union = (directRouteCameras + clusterCameras).distinctBy { it.id }
                CameraClusterUtils.createAvoidanceMultiPolygon(union, start, end, radiusMeters = radiusBase, maxPolygons = 85)
            } else null
        ).distinct()
        
        Log.d(TAG, "Testing ${configs.size} dynamic avoid_polygon configurations in parallel (including corridor cameras: ${corridorCameras.size})")
        
        val deferredRoutes = configs.mapIndexed { index, multiPolygon ->
            async {
                calculateSingleAvoidance(start, end, multiPolygon, cameras, transportMode, "polygon_avoid_$index", avoidanceRadius)
            }
        }
        
        deferredRoutes.awaitAll().flatten()
    }
    
    /**
     * Execute an ORS request with a specific avoid_polygons GeoJSON and return scored routes
     */
    private suspend fun calculateSingleAvoidance(
        start: GeoPoint,
        end: GeoPoint,
        multiPolygon: com.osmcamera.mapper.data.api.ORSMultiPolygon,
        cameras: List<Camera>,
        transportMode: String,
        label: String,
        avoidanceRadius: Double = 40.0
    ): List<Route> {
        return try {
            val request = ORSRouteRequest(
                coordinates = listOf(
                    listOf(start.longitude, start.latitude),
                    listOf(end.longitude, end.latitude)
                ),
                alternativeRoutes = null,
                options = ORSOptions(avoidPolygons = multiPolygon)
            )
            
            val response = orsApi.getRoute(
                profile = transportMode,
                apiKey = OpenRouteServiceApi.API_KEY,
                request = request
            )
            
            if (response.isSuccessful) {
                val orsRoutes = response.body()?.routes ?: emptyList()
                orsRoutes.mapIndexed { rIdx, orsRoute ->
                    val points = GeometryUtils.decodePolyline(orsRoute.geometry)
                    val cameraCount = GeometryUtils.countCamerasNearRoute(
                        routePoints = points,
                        cameras = cameras,
                        radiusMeters = avoidanceRadius
                    )
                    
                    Log.i(TAG, "Dynamic avoid_polygon $label-$rIdx: $cameraCount cameras, ${String.format("%.1f", orsRoute.summary.distance/1000)}km")
                    
                    Route(
                        id = "${label}_$rIdx",
                        points = points,
                        distance = orsRoute.summary.distance,
                        duration = orsRoute.summary.duration,
                        cameraCount = cameraCount
                    )
                }
            } else {
                Log.w(TAG, "Polygon avoidance $label failed: ${response.code()} ${response.errorBody()?.string()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Polygon avoidance $label error", e)
            emptyList()
        }
    }
    
    /**
     * Generate intelligent routes bypassing camera bottlenecks on the direct path
     */
    private suspend fun calculateSmartDetours(
        start: GeoPoint,
        end: GeoPoint,
        directRoutePoints: List<GeoPoint>,
        cameras: List<Camera>,
        transportMode: String
    ): List<Route> = coroutineScope {
        if (cameras.isEmpty()) return@coroutineScope emptyList()
        
        val waypoints = CameraClusterUtils.generateSmartAvoidanceWaypoints(
            start = start,
            end = end,
            directRoutePoints = directRoutePoints,
            cameras = cameras,
            detourDistances = listOf(160.0, 300.0, 480.0, 700.0)
        )
        
        Log.d(TAG, "Testing ${waypoints.size} smart avoidance waypoints in parallel")
        
        val deferredRoutes = waypoints.mapIndexed { index, waypoint ->
            async {
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
                            
                            Log.d(TAG, "Smart detour $index (via ${String.format("%.4f", waypoint.latitude)}, ${String.format("%.4f", waypoint.longitude)}): $cameraCount cameras, ${String.format("%.1f", orsRoute.summary.distance/1000)}km")
                            
                            Route(
                                id = "smart_detour_$index",
                                points = points,
                                distance = orsRoute.summary.distance,
                                duration = orsRoute.summary.duration,
                                cameraCount = cameraCount
                            )
                        } else null
                    } else {
                        Log.w(TAG, "Detour $index failed: ${response.code()}")
                        null
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Detour $index error", e)
                    null
                }
            }
        }
        
        deferredRoutes.awaitAll().filterNotNull()
    }
    
    /**
     * Execute direct ORS request and parse response
     */
    private suspend fun executeRouteRequest(
        request: ORSRouteRequest,
        cameras: List<Camera>,
        routeType: String,
        transportMode: String = "foot-walking",
        avoidanceRadius: Double = 40.0
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
            val points = GeometryUtils.decodePolyline(orsRoute.geometry)
            val cameraCount = GeometryUtils.countCamerasNearRoute(
                routePoints = points,
                cameras = cameras,
                radiusMeters = avoidanceRadius
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
