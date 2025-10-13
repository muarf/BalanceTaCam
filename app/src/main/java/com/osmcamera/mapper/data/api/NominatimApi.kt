package com.osmcamera.mapper.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Nominatim API for geocoding (address search)
 * Free service provided by OpenStreetMap
 */
interface NominatimApi {
    
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 5,
        @Query("addressdetails") addressDetails: Int = 1
    ): Response<List<NominatimResult>>
    
    companion object {
        const val BASE_URL = "https://nominatim.openstreetmap.org/"
        // Free service, please use responsibly
        // Max 1 request/second
    }
}

/**
 * Nominatim search result
 */
data class NominatimResult(
    @SerializedName("place_id")
    val placeId: Long,
    
    @SerializedName("licence")
    val licence: String?,
    
    @SerializedName("osm_type")
    val osmType: String,
    
    @SerializedName("osm_id")
    val osmId: Long,
    
    @SerializedName("lat")
    val lat: String,
    
    @SerializedName("lon")
    val lon: String,
    
    @SerializedName("display_name")
    val displayName: String,
    
    @SerializedName("address")
    val address: NominatimAddress?
)

data class NominatimAddress(
    @SerializedName("road")
    val road: String?,
    
    @SerializedName("house_number")
    val houseNumber: String?,
    
    @SerializedName("postcode")
    val postcode: String?,
    
    @SerializedName("city")
    val city: String?,
    
    @SerializedName("town")
    val town: String?,
    
    @SerializedName("village")
    val village: String?,
    
    @SerializedName("country")
    val country: String?
)

