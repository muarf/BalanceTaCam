package com.osmcamera.mapper.utils

import com.osmcamera.mapper.data.model.Camera
import org.osmdroid.util.GeoPoint
import kotlin.math.*

/**
 * Utilities for clustering cameras and calculating smart avoidance waypoints
 */
object CameraClusterUtils {
    
    /**
     * Find camera clusters using distance-based clustering
     */
    fun findCameraClusters(
        cameras: List<Camera>,
        maxDistanceMeters: Double = 120.0,
        minCamerasPerCluster: Int = 2
    ): List<CameraCluster> {
        if (cameras.isEmpty()) return emptyList()
        
        val clusters = mutableListOf<CameraCluster>()
        val assigned = mutableSetOf<String>()
        
        cameras.forEach { camera ->
            if (camera.id in assigned) return@forEach
            
            // Find all cameras within distance
            val nearby = cameras.filter { other ->
                other.id !in assigned && 
                GeometryUtils.distance(
                    camera.latitude, camera.longitude,
                    other.latitude, other.longitude
                ) <= maxDistanceMeters
            }
            
            if (nearby.size >= minCamerasPerCluster) {
                val centerLat = nearby.map { it.latitude }.average()
                val centerLon = nearby.map { it.longitude }.average()
                
                clusters.add(CameraCluster(
                    center = GeoPoint(centerLat, centerLon),
                    cameraCount = nearby.size,
                    cameras = nearby
                ))
                
                nearby.forEach { assigned.add(it.id) }
            }
        }
        
        return clusters.sortedByDescending { it.cameraCount }
    }
    
    /**
     * Calculate heading/bearing in radians from p1 to p2
     */
    fun calculateHeading(p1: GeoPoint, p2: GeoPoint): Double {
        val lat1 = Math.toRadians(p1.latitude)
        val lat2 = Math.toRadians(p2.latitude)
        val dLon = Math.toRadians(p2.longitude - p1.longitude)
        
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return atan2(y, x)
    }
    
    /**
     * Offset a GeoPoint by distanceMeters along a given bearing in radians
     */
    fun offsetPoint(point: GeoPoint, distanceMeters: Double, bearingRad: Double): GeoPoint {
        val earthRadius = 6371000.0
        val lat1 = Math.toRadians(point.latitude)
        val lon1 = Math.toRadians(point.longitude)
        val dR = distanceMeters / earthRadius
        
        val lat2 = asin(sin(lat1) * cos(dR) + cos(lat1) * sin(dR) * cos(bearingRad))
        val lon2 = lon1 + atan2(
            sin(bearingRad) * sin(dR) * cos(lat1),
            cos(dR) - sin(lat1) * sin(lat2)
        )
        
        return GeoPoint(Math.toDegrees(lat2), Math.toDegrees(lon2))
    }
    
    /**
     * Find camera bottlenecks directly along a route
     */
    fun findRouteBottlenecks(
        routePoints: List<GeoPoint>,
        cameras: List<Camera>,
        corridorRadiusMeters: Double = 60.0
    ): List<RouteBottleneck> {
        if (routePoints.isEmpty() || cameras.isEmpty()) return emptyList()
        
        val camerasOnRoute = mutableListOf<Pair<Camera, GeoPoint>>() // camera to closest route point
        
        cameras.forEach { camera ->
            var minDist = Double.MAX_VALUE
            var closestRoutePoint: GeoPoint? = null
            
            routePoints.forEach { pt ->
                val d = GeometryUtils.distance(pt.latitude, pt.longitude, camera.latitude, camera.longitude)
                if (d < minDist) {
                    minDist = d
                    closestRoutePoint = pt
                }
            }
            
            if (minDist <= corridorRadiusMeters && closestRoutePoint != null) {
                camerasOnRoute.add(Pair(camera, closestRoutePoint!!))
            }
        }
        
        if (camerasOnRoute.isEmpty()) return emptyList()
        
        // Group nearby cameras into bottlenecks
        val bottlenecks = mutableListOf<RouteBottleneck>()
        val assigned = mutableSetOf<String>()
        
        camerasOnRoute.forEach { (camera, _) ->
            if (camera.id in assigned) return@forEach
            
            val group = camerasOnRoute.filter { (other, _) ->
                other.id !in assigned &&
                GeometryUtils.distance(camera.latitude, camera.longitude, other.latitude, other.longitude) <= 180.0
            }
            
            val centerLat = group.map { it.first.latitude }.average()
            val centerLon = group.map { it.first.longitude }.average()
            val center = GeoPoint(centerLat, centerLon)
            
            // Find local route direction around this bottleneck
            val closestIdx = routePoints.indices.minByOrNull { i ->
                GeometryUtils.distance(routePoints[i], center)
            } ?: 0
            
            val prevIdx = max(0, closestIdx - 3)
            val nextIdx = min(routePoints.size - 1, closestIdx + 3)
            val heading = if (prevIdx < nextIdx) {
                calculateHeading(routePoints[prevIdx], routePoints[nextIdx])
            } else {
                0.0
            }
            
            bottlenecks.add(RouteBottleneck(
                center = center,
                cameraCount = group.size,
                cameras = group.map { it.first },
                routeHeadingRad = heading
            ))
            
            group.forEach { assigned.add(it.first.id) }
        }
        
        return bottlenecks.sortedByDescending { it.cameraCount }
    }
    
    /**
     * Generate intelligent waypoints that avoid camera hotspots along the path
     */
    fun generateSmartAvoidanceWaypoints(
        start: GeoPoint,
        end: GeoPoint,
        directRoutePoints: List<GeoPoint>,
        cameras: List<Camera>,
        detourDistances: List<Double> = listOf(160.0, 300.0, 480.0, 700.0)
    ): List<GeoPoint> {
        val waypoints = mutableListOf<GeoPoint>()
        
        // 1. Identify bottlenecks on the direct route
        val bottlenecks = findRouteBottlenecks(directRoutePoints, cameras, corridorRadiusMeters = 65.0)
        
        bottlenecks.take(5).forEach { bottleneck ->
            val heading = bottleneck.routeHeadingRad
            val leftAngle = heading - (PI / 2.0)
            val rightAngle = heading + (PI / 2.0)
            
            detourDistances.forEach { distance ->
                // Try perpendicular left and right detours
                listOf(leftAngle, rightAngle).forEach { angle ->
                    val wp = offsetPoint(bottleneck.center, distance, angle)
                    
                    // Verify that the candidate waypoint is not right next to another camera
                    val isSafe = cameras.none { cam ->
                        GeometryUtils.distance(wp.latitude, wp.longitude, cam.latitude, cam.longitude) < 35.0
                    }
                    
                    if (isSafe) {
                        waypoints.add(wp)
                    }
                }
            }
        }
        
        // 2. Also consider general camera clusters between start and end
        val allClusters = findCameraClusters(cameras, maxDistanceMeters = 130.0, minCamerasPerCluster = 3)
        val generalHeading = calculateHeading(start, end)
        
        allClusters.take(4).forEach { cluster ->
            detourDistances.take(3).forEach { distance ->
                listOf(generalHeading - PI / 2, generalHeading + PI / 2).forEach { angle ->
                    val wp = offsetPoint(cluster.center, distance, angle)
                    val isSafe = cameras.none { cam ->
                        GeometryUtils.distance(wp.latitude, wp.longitude, cam.latitude, cam.longitude) < 35.0
                    }
                    if (isSafe) {
                        waypoints.add(wp)
                    }
                }
            }
        }
        
        // Deduplicate waypoints that are too close to each other (closer than 70m)
        val filtered = mutableListOf<GeoPoint>()
        waypoints.forEach { wp ->
            val isDuplicate = filtered.any { existing ->
                GeometryUtils.distance(existing, wp) < 70.0
            }
            if (!isDuplicate) {
                filtered.add(wp)
            }
        }
        
        return filtered.take(14)
    }

    /**
     * Get cameras located within a corridor bounding box between start and end,
     * prioritizing cameras along the direct path and public surveillance cameras.
     */
    fun getCamerasInCorridor(
        cameras: List<Camera>,
        start: GeoPoint,
        end: GeoPoint,
        directRoutePoints: List<GeoPoint> = emptyList(),
        paddingMeters: Double = 600.0
    ): List<Camera> {
        val minLat = minOf(start.latitude, end.latitude)
        val maxLat = maxOf(start.latitude, end.latitude)
        val minLon = minOf(start.longitude, end.longitude)
        val maxLon = maxOf(start.longitude, end.longitude)
        
        val latPad = paddingMeters / 111000.0
        val lonPad = paddingMeters / 75000.0
        
        val corridor = cameras.filter { cam ->
            cam.latitude in (minLat - latPad)..(maxLat + latPad) &&
            cam.longitude in (minLon - lonPad)..(maxLon + lonPad)
        }
        
        // Priority 1: Cameras directly along the direct route (< 50m)
        val directCams = if (directRoutePoints.isNotEmpty()) {
            GeometryUtils.getCamerasAlongRoute(directRoutePoints, corridor, radiusMeters = 50.0)
        } else emptyList()
        val directIds = directCams.map { it.id }.toSet()
        
        // Priority 2: Other corridor cameras, prioritizing public cameras, then proximity to direct segment
        val otherCams = corridor.filter { it.id !in directIds }.sortedWith(
            compareBy<Camera> {
                if (it.surveillance == "public" || it.surveillance == null) 0 else 1
            }.thenBy { cam ->
                GeometryUtils.distanceToSegment(
                    point = GeoPoint(cam.latitude, cam.longitude),
                    segA = start,
                    segB = end
                )
            }
        )
        
        return directCams + otherCams
    }

    /**
     * Create an ORSMultiPolygon containing avoidance circles around a list of cameras.
     * Cameras too close to start or destination are excluded to allow departure/arrival.
     */
    fun createAvoidanceMultiPolygon(
        cameras: List<Camera>,
        start: GeoPoint,
        end: GeoPoint,
        radiusMeters: Double = 30.0,
        minDistanceFromEndpoints: Double = 40.0,
        maxPolygons: Int = 90
    ): com.osmcamera.mapper.data.api.ORSMultiPolygon? {
        if (cameras.isEmpty()) return null
        
        val validCameras = cameras.filter { cam ->
            val dStart = GeometryUtils.distance(cam.latitude, cam.longitude, start.latitude, start.longitude)
            val dEnd = GeometryUtils.distance(cam.latitude, cam.longitude, end.latitude, end.longitude)
            dStart > minDistanceFromEndpoints && dEnd > minDistanceFromEndpoints
        }.take(maxPolygons)
        
        if (validCameras.isEmpty()) return null
        
        val polygonList = validCameras.map { cam ->
            val ring = GeometryUtils.createAvoidanceCircle(
                center = GeoPoint(cam.latitude, cam.longitude),
                radiusMeters = radiusMeters,
                points = 8
            )
            listOf(ring)
        }
        
        return com.osmcamera.mapper.data.api.ORSMultiPolygon(
            type = "MultiPolygon",
            coordinates = polygonList
        )
    }
}

/**
 * Represents a cluster of cameras
 */
data class CameraCluster(
    val center: GeoPoint,
    val cameraCount: Int,
    val cameras: List<Camera>
)

/**
 * Represents a camera bottleneck on a specific route
 */
data class RouteBottleneck(
    val center: GeoPoint,
    val cameraCount: Int,
    val cameras: List<Camera>,
    val routeHeadingRad: Double
)
