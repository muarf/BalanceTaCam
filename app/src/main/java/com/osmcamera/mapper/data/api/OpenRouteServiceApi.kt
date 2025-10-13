package com.osmcamera.mapper.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

/**
 * OpenRouteService API for routing
 */
interface OpenRouteServiceApi {
    
    @POST("v2/directions/{profile}")
    suspend fun getRoute(
        @Path("profile") profile: String = "driving-car",
        @Header("Authorization") apiKey: String,
        @Body request: ORSRouteRequest
    ): Response<ORSRouteResponse>
    
    companion object {
        const val BASE_URL = "https://api.openrouteservice.org/"
        // Free tier: 2000 requests/day
        const val API_KEY = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6ImRiZWE4ZmVlMWEwYzQ3YTVhYTE5ZmQ4ZGZjNDA4NzM2IiwiaCI6Im11cm11cjY0In0="
    }
}

/**
 * ORS Route Request
 */
data class ORSRouteRequest(
    @SerializedName("coordinates")
    val coordinates: List<List<Double>>, // [[lon, lat], [lon, lat]]
    
    @SerializedName("alternative_routes")
    val alternativeRoutes: ORSAlternativeRoutes? = ORSAlternativeRoutes(
        targetCount = 3,
        shareFactor = 0.6,
        weightFactor = 1.4
    ),
    
    @SerializedName("options")
    val options: ORSOptions? = null,
    
    @SerializedName("geometry")
    val geometry: Boolean = true,
    
    @SerializedName("instructions")
    val instructions: Boolean = true
)

data class ORSAlternativeRoutes(
    @SerializedName("target_count")
    val targetCount: Int,
    
    @SerializedName("share_factor")
    val shareFactor: Double,
    
    @SerializedName("weight_factor")
    val weightFactor: Double
)

data class ORSOptions(
    @SerializedName("avoid_polygons")
    val avoidPolygons: ORSPolygon? = null
)

data class ORSPolygon(
    @SerializedName("type")
    val type: String = "Polygon",
    
    @SerializedName("coordinates")
    val coordinates: List<List<List<Double>>> // [[[lon, lat], [lon, lat], ...]]
)

/**
 * ORS Route Response
 */
data class ORSRouteResponse(
    @SerializedName("routes")
    val routes: List<ORSRoute>,
    
    @SerializedName("metadata")
    val metadata: ORSMetadata?
)

data class ORSRoute(
    @SerializedName("summary")
    val summary: ORSSummary,
    
    @SerializedName("geometry")
    val geometry: String, // Encoded polyline
    
    @SerializedName("segments")
    val segments: List<ORSSegment>?,
    
    @SerializedName("way_points")
    val wayPoints: List<Int>?
)

data class ORSSummary(
    @SerializedName("distance")
    val distance: Double, // meters
    
    @SerializedName("duration")
    val duration: Double // seconds
)

data class ORSSegment(
    @SerializedName("distance")
    val distance: Double,
    
    @SerializedName("duration")
    val duration: Double,
    
    @SerializedName("steps")
    val steps: List<ORSStep>?
)

data class ORSStep(
    @SerializedName("distance")
    val distance: Double,
    
    @SerializedName("duration")
    val duration: Double,
    
    @SerializedName("type")
    val type: Int,
    
    @SerializedName("instruction")
    val instruction: String,
    
    @SerializedName("name")
    val name: String?,
    
    @SerializedName("way_points")
    val wayPoints: List<Int>
)

data class ORSMetadata(
    @SerializedName("service")
    val service: String?,
    
    @SerializedName("timestamp")
    val timestamp: Long?
)

