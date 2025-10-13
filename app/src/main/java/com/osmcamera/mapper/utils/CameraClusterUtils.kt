package com.osmcamera.mapper.utils

import com.osmcamera.mapper.data.model.Camera
import org.osmdroid.util.GeoPoint
import kotlin.math.sqrt

/**
 * Utilities for clustering cameras to identify hotspots
 */
object CameraClusterUtils {
    
    /**
     * Find camera clusters using simple distance-based clustering
     * @return List of cluster centers with their camera count
     */
    fun findCameraClusters(
        cameras: List<Camera>,
        maxDistanceMeters: Double = 100.0,
        minCamerasPerCluster: Int = 3
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
                // Calculate cluster center (average position)
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
     * Generate intelligent waypoints that avoid camera clusters
     */
    fun generateAvoidanceWaypoints(
        start: GeoPoint,
        end: GeoPoint,
        clusters: List<CameraCluster>,
        distances: List<Double> = listOf(200.0, 400.0, 600.0)
    ): List<GeoPoint> {
        if (clusters.isEmpty()) return emptyList()
        
        val waypoints = mutableListOf<GeoPoint>()
        
        // Mid-point between start and end
        val midLat = (start.latitude + end.latitude) / 2
        val midLon = (start.longitude + end.longitude) / 2
        val midPoint = GeoPoint(midLat, midLon)
        
        // For each major cluster
        clusters.take(3).forEach { cluster ->
            // Calculate direction from mid-point to cluster
            val toClusterLat = cluster.center.latitude - midLat
            val toClusterLon = cluster.center.longitude - midLon
            
            // Calculate perpendicular directions (to go around the cluster)
            val perpAngle1 = Math.PI / 2 // 90 degrees
            val perpAngle2 = -Math.PI / 2 // -90 degrees
            
            distances.forEach { distance ->
                // Convert distance to degrees (~111km per degree)
                val distDegrees = distance / 111000.0
                
                // Create waypoints perpendicular to cluster direction
                listOf(perpAngle1, perpAngle2).forEach { angle ->
                    val wpLat = midLat + distDegrees * Math.sin(angle)
                    val wpLon = midLon + distDegrees * Math.cos(angle)
                    
                    val waypoint = GeoPoint(wpLat, wpLon)
                    
                    // Check if waypoint is not too close to any cluster
                    val tooClose = clusters.any { c ->
                        GeometryUtils.distance(
                            waypoint.latitude, waypoint.longitude,
                            c.center.latitude, c.center.longitude
                        ) < 100.0
                    }
                    
                    if (!tooClose) {
                        waypoints.add(waypoint)
                    }
                }
            }
        }
        
        // Also add some systematic detours (fallback)
        val systematicOffsets = listOf(
            Pair(0.003, 0.0),    // North
            Pair(-0.003, 0.0),   // South
            Pair(0.0, 0.003),    // East
            Pair(0.0, -0.003)    // West
        )
        
        systematicOffsets.forEach { (latOffset, lonOffset) ->
            waypoints.add(GeoPoint(midLat + latOffset, midLon + lonOffset))
        }
        
        return waypoints.distinctBy { 
            "${String.format("%.4f", it.latitude)},${String.format("%.4f", it.longitude)}"
        }
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

