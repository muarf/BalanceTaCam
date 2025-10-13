package com.osmcamera.mapper.data.model

import org.osmdroid.util.GeoPoint

/**
 * Route model with camera avoidance info
 */
data class Route(
    val id: String,
    val points: List<GeoPoint>,
    val distance: Double, // meters
    val duration: Double, // seconds
    val cameraCount: Int = 0,
    val instructions: List<RouteInstruction> = emptyList()
) {
    val distanceKm: Double get() = distance / 1000.0
    val durationMinutes: Double get() = duration / 60.0
    
    fun getScoreLabel(): String {
        return when {
            cameraCount == 0 -> "✅ Aucune caméra"
            cameraCount <= 2 -> "✅ Très peu de caméras ($cameraCount)"
            cameraCount <= 5 -> "⚠️ Quelques caméras ($cameraCount)"
            cameraCount <= 10 -> "⚠️ Caméras présentes ($cameraCount)"
            else -> "❌ Nombreuses caméras ($cameraCount)"
        }
    }
}

data class RouteInstruction(
    val text: String,
    val distance: Double,
    val duration: Double
)

/**
 * Route comparison for display
 */
data class RouteComparison(
    val routes: List<Route>,
    val bestRoute: Route,
    val directRoute: Route?
) {
    val camerasSaved: Int = (directRoute?.cameraCount ?: 0) - bestRoute.cameraCount
}

