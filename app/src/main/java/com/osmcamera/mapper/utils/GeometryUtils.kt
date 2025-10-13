package com.osmcamera.mapper.utils

import com.osmcamera.mapper.data.model.Camera
import org.osmdroid.util.GeoPoint
import kotlin.math.*

/**
 * Geometry utilities for camera avoidance
 */
object GeometryUtils {
    
    /**
     * Calculate distance between two points in meters (Haversine formula)
     */
    fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return R * c
    }
    
    /**
     * Calculate distance between two GeoPoints
     */
    fun distance(p1: GeoPoint, p2: GeoPoint): Double {
        return distance(p1.latitude, p1.longitude, p2.latitude, p2.longitude)
    }
    
    /**
     * Create a circular polygon around a point (for avoiding cameras)
     */
    fun createAvoidanceCircle(
        center: GeoPoint,
        radiusMeters: Double,
        points: Int = 16
    ): List<List<Double>> {
        val circle = mutableListOf<List<Double>>()
        val earthRadius = 6371000.0 // meters
        
        for (i in 0..points) {
            val angle = 2 * PI * i / points
            
            // Calculate offset
            val dx = radiusMeters * cos(angle)
            val dy = radiusMeters * sin(angle)
            
            // Convert to lat/lon
            val lat = center.latitude + (dy / earthRadius) * (180 / PI)
            val lon = center.longitude + (dx / earthRadius) * (180 / PI) / cos(center.latitude * PI / 180)
            
            circle.add(listOf(lon, lat)) // ORS uses [lon, lat] order
        }
        
        return circle
    }
    
    /**
     * Count cameras near a route
     */
    fun countCamerasNearRoute(
        routePoints: List<GeoPoint>,
        cameras: List<Camera>,
        radiusMeters: Double = 50.0
    ): Int {
        val camerasNear = mutableSetOf<String>()
        
        routePoints.forEach { point ->
            cameras.forEach { camera ->
                val dist = distance(
                    point.latitude, point.longitude,
                    camera.latitude, camera.longitude
                )
                if (dist <= radiusMeters) {
                    camerasNear.add(camera.id)
                }
            }
        }
        
        return camerasNear.size
    }
    
    /**
     * Get cameras along a route
     */
    fun getCamerasAlongRoute(
        routePoints: List<GeoPoint>,
        cameras: List<Camera>,
        radiusMeters: Double = 50.0
    ): List<Camera> {
        val camerasAlong = mutableMapOf<String, Camera>()
        
        routePoints.forEach { point ->
            cameras.forEach { camera ->
                val dist = distance(
                    point.latitude, point.longitude,
                    camera.latitude, camera.longitude
                )
                if (dist <= radiusMeters && !camerasAlong.containsKey(camera.id)) {
                    camerasAlong[camera.id] = camera
                }
            }
        }
        
        return camerasAlong.values.toList()
    }
    
    /**
     * Decode polyline (Google format)
     */
    fun decodePolyline(encoded: String): List<GeoPoint> {
        val poly = mutableListOf<GeoPoint>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            
            shift = 0
            result = 0
            
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            
            poly.add(GeoPoint(lat / 1e5, lng / 1e5))
        }
        
        return poly
    }
}

