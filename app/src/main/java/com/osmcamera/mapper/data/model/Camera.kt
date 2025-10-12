package com.osmcamera.mapper.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Camera data model representing a surveillance camera
 */
@Entity(tableName = "cameras")
data class Camera(
    @PrimaryKey
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val manMade: String = "surveillance",
    val surveillanceType: String = "camera",
    
    // Optional fields
    val cameraType: String? = null,
    val cameraMount: String? = null,
    val cameraDirection: Int? = null,
    val surveillance: String? = null,
    val operator: String? = null,
    val operatorType: String? = null,
    val surveillanceZone: String? = null,
    val description: String? = null,
    val level: String? = null,
    val height: String? = null,
    
    // Metadata
    val lastModified: Long = System.currentTimeMillis(),
    val version: Int = 1
)

/**
 * Camera form data for creating/editing cameras
 */
data class CameraFormData(
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var cameraType: String? = null,
    var cameraMount: String? = null,
    var cameraDirection: String? = null,
    var surveillance: String? = null,
    var operator: String? = null,
    var operatorType: String? = null,
    var surveillanceZone: String? = null,
    var description: String? = null,
    var level: String? = null,
    var height: String? = null
) {
    /**
     * Convert to OSM tags map
     */
    fun toOsmTags(): Map<String, String> {
        val tags = mutableMapOf(
            "man_made" to "surveillance",
            "surveillance:type" to "camera"
        )
        
        cameraType?.let { tags["camera:type"] = it }
        cameraMount?.let { tags["camera:mount"] = it }
        cameraDirection?.let { 
            if (it.isNotBlank()) {
                tags["camera:direction"] = it
            }
        }
        surveillance?.let { tags["surveillance"] = it }
        operator?.let { 
            if (it.isNotBlank()) {
                tags["operator"] = it
            }
        }
        operatorType?.let { tags["operator:type"] = it }
        surveillanceZone?.let { tags["surveillance:zone"] = it }
        description?.let { 
            if (it.isNotBlank()) {
                tags["description"] = it
            }
        }
        level?.let { 
            if (it.isNotBlank()) {
                tags["level"] = it
            }
        }
        height?.let { 
            if (it.isNotBlank()) {
                tags["height"] = it
            }
        }
        
        return tags
    }
    
    /**
     * Validate form data
     */
    fun validate(): ValidationResult {
        if (latitude == 0.0 || longitude == 0.0) {
            return ValidationResult(false, "Invalid position")
        }
        
        if (latitude < -90 || latitude > 90) {
            return ValidationResult(false, "Latitude must be between -90 and 90")
        }
        
        if (longitude < -180 || longitude > 180) {
            return ValidationResult(false, "Longitude must be between -180 and 180")
        }
        
        cameraDirection?.let {
            if (it.isNotBlank()) {
                val dir = it.toIntOrNull()
                if (dir == null || dir < 0 || dir > 360) {
                    return ValidationResult(false, "Direction must be between 0 and 360")
                }
            }
        }
        
        return ValidationResult(true)
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val error: String? = null
)

/**
 * Enums for camera properties
 */
object CameraTypes {
    const val FIXED = "fixed"
    const val DOME = "dome"
    const val PTZ = "ptz"
    const val PANORAMIC = "panoramic"
    
    val all = listOf(FIXED, DOME, PTZ, PANORAMIC)
}

object CameraMounts {
    const val POLE = "pole"
    const val WALL = "wall"
    const val CEILING = "ceiling"
    const val STREET_LAMP = "street_lamp"
    
    val all = listOf(POLE, WALL, CEILING, STREET_LAMP)
}

object SurveillanceTypes {
    const val PUBLIC = "public"
    const val OUTDOOR = "outdoor"
    const val INDOOR = "indoor"
    
    val all = listOf(PUBLIC, OUTDOOR, INDOOR)
}

object OperatorTypes {
    const val PUBLIC = "public"
    const val PRIVATE = "private"
    const val COMMERCIAL = "commercial"
    
    val all = listOf(PUBLIC, PRIVATE, COMMERCIAL)
}

object SurveillanceZones {
    const val TOWN = "town"
    const val PARKING = "parking"
    const val TRAFFIC = "traffic"
    const val BUILDING = "building"
    
    val all = listOf(TOWN, PARKING, TRAFFIC, BUILDING)
}


