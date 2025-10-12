package com.osmcamera.mapper.data.api

import com.osmcamera.mapper.data.model.Camera
import com.google.gson.JsonParser
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Overpass API Service for querying existing cameras
 */
interface OverpassApiService {
    
    /**
     * Execute Overpass query
     */
    @GET("api/interpreter")
    suspend fun query(@Query("data") query: String): Response<ResponseBody>
    
    companion object {
        const val BASE_URL = "https://overpass-api.de/"
        
        /**
         * Build query to get surveillance cameras in bounding box
         */
        fun buildCameraQuery(
            south: Double,
            west: Double,
            north: Double,
            east: Double
        ): String {
            return """
                [out:json][timeout:25];
                (
                  node["man_made"="surveillance"]["surveillance:type"="camera"]($south,$west,$north,$east);
                );
                out body;
                >;
                out skel qt;
            """.trimIndent()
        }
    }
}

/**
 * Parser for Overpass API JSON responses
 */
object OverpassResponseParser {
    fun parseCameras(json: String): List<Camera> {
        val cameras = mutableListOf<Camera>()
        
        try {
            val root = JsonParser.parseString(json).asJsonObject
            val elements = root.getAsJsonArray("elements")
            
            for (element in elements) {
                val obj = element.asJsonObject
                val type = obj.get("type")?.asString
                
                if (type == "node") {
                    val id = obj.get("id")?.asString ?: continue
                    val lat = obj.get("lat")?.asDouble ?: continue
                    val lon = obj.get("lon")?.asDouble ?: continue
                    val tags = obj.getAsJsonObject("tags")
                    
                    if (tags != null) {
                        val camera = Camera(
                            id = id,
                            latitude = lat,
                            longitude = lon,
                            manMade = tags.get("man_made")?.asString ?: "surveillance",
                            surveillanceType = tags.get("surveillance:type")?.asString ?: "camera",
                            cameraType = tags.get("camera:type")?.asString,
                            cameraMount = tags.get("camera:mount")?.asString,
                            cameraDirection = tags.get("camera:direction")?.asString?.toIntOrNull(),
                            surveillance = tags.get("surveillance")?.asString,
                            operator = tags.get("operator")?.asString,
                            operatorType = tags.get("operator:type")?.asString,
                            surveillanceZone = tags.get("surveillance:zone")?.asString,
                            description = tags.get("description")?.asString,
                            level = tags.get("level")?.asString,
                            height = tags.get("height")?.asString
                        )
                        cameras.add(camera)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return cameras
    }
}


