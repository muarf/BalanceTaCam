package com.osmcamera.mapper.data.api

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * OpenStreetMap API v0.6 Service
 */
interface OSMApiService {
    
    /**
     * Get user details
     */
    @GET("api/0.6/user/details.json")
    suspend fun getUserDetails(): Response<ResponseBody>
    
    /**
     * Create a new changeset
     * @param changesetXml XML body for changeset creation
     */
    @PUT("api/0.6/changeset/create")
    @Headers("Content-Type: text/xml")
    suspend fun createChangeset(@Body changesetXml: RequestBody): Response<ResponseBody>
    
    /**
     * Close a changeset
     */
    @PUT("api/0.6/changeset/{id}/close")
    suspend fun closeChangeset(@Path("id") changesetId: Long): Response<ResponseBody>
    
    /**
     * Create a new node
     * @param nodeXml XML body for node creation
     */
    @PUT("api/0.6/node/create")
    @Headers("Content-Type: text/xml")
    suspend fun createNode(@Body nodeXml: RequestBody): Response<ResponseBody>
    
    /**
     * Get changeset details
     */
    @GET("api/0.6/changeset/{id}")
    suspend fun getChangeset(@Path("id") changesetId: Long): Response<ResponseBody>
    
    companion object {
        const val BASE_URL = "https://api.openstreetmap.org/"
        const val BASE_URL_DEV = "https://master.apis.dev.openstreetmap.org/" // For testing
    }
}

/**
 * Helper to build changeset XML
 */
object ChangesetXmlBuilder {
    fun build(comment: String, source: String, createdBy: String): String {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <osm>
              <changeset>
                <tag k="comment" v="${comment.escapeXml()}" />
                <tag k="source" v="${source.escapeXml()}" />
                <tag k="created_by" v="${createdBy.escapeXml()}" />
              </changeset>
            </osm>
        """.trimIndent()
    }
    
    private fun String.escapeXml(): String {
        return this
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}

/**
 * Helper to build node XML
 */
object NodeXmlBuilder {
    fun build(
        changesetId: Long,
        latitude: Double,
        longitude: Double,
        tags: Map<String, String>
    ): String {
        val tagElements = tags.entries.joinToString("\n") { (k, v) ->
            """    <tag k="${k.escapeXml()}" v="${v.escapeXml()}" />"""
        }
        
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <osm>
              <node changeset="$changesetId" lat="$latitude" lon="$longitude">
            $tagElements
              </node>
            </osm>
        """.trimIndent()
    }
    
    private fun String.escapeXml(): String {
        return this
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}


