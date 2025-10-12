package com.osmcamera.mapper.data.repository

import com.google.gson.JsonParser
import com.osmcamera.mapper.BuildConfig
import com.osmcamera.mapper.data.api.ChangesetXmlBuilder
import com.osmcamera.mapper.data.api.NodeXmlBuilder
import com.osmcamera.mapper.data.api.OSMApiService
import com.osmcamera.mapper.data.auth.OAuthService
import com.osmcamera.mapper.data.local.PreferencesManager
import com.osmcamera.mapper.data.model.CameraFormData
import com.osmcamera.mapper.data.model.Changeset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for OSM API operations
 */
@Singleton
class OSMRepository @Inject constructor(
    private val osmApiService: OSMApiService,
    private val oauthService: OAuthService,
    private val preferencesManager: PreferencesManager
) {
    
    /**
     * Create a camera on OSM
     */
    suspend fun createCamera(cameraData: CameraFormData): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Validate
                val validation = cameraData.validate()
                if (!validation.isValid) {
                    return@withContext Result.failure(Exception(validation.error))
                }
                
                // Get OAuth tokens
                val tokens = preferencesManager.getOAuthTokens()
                    ?: return@withContext Result.failure(Exception("Not authenticated"))
                
                // 1. Create changeset
                val changesetId = createChangeset()
                    ?: return@withContext Result.failure(Exception("Failed to create changeset"))
                
                try {
                    // 2. Create node
                    val nodeId = createNode(changesetId, cameraData)
                        ?: return@withContext Result.failure(Exception("Failed to create node"))
                    
                    // 3. Close changeset
                    closeChangeset(changesetId)
                    
                    Result.success(nodeId)
                } catch (e: Exception) {
                    // Try to close changeset even if node creation failed
                    try {
                        closeChangeset(changesetId)
                    } catch (closeException: Exception) {
                        // Ignore close exception
                    }
                    throw e
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Create a changeset
     */
    private suspend fun createChangeset(): Long? {
        return withContext(Dispatchers.IO) {
            try {
                val tokens = preferencesManager.getOAuthTokens() ?: return@withContext null
                
                val changesetXml = ChangesetXmlBuilder.build(
                    comment = "Added surveillance camera via OSM Camera Mapper",
                    source = "survey",
                    createdBy = "OSM Camera Mapper v${BuildConfig.VERSION_NAME}"
                )
                
                // Sign the request
                val request = oauthService.signRequest(
                    url = "${OSMApiService.BASE_URL}api/0.6/changeset/create",
                    method = com.github.scribejava.core.model.Verb.PUT,
                    tokens = tokens
                )
                
                request.setPayload(changesetXml)
                request.addHeader("Content-Type", "text/xml")
                
                // Execute with OkHttp
                val okHttpClient = okhttp3.OkHttpClient()
                val okHttpRequest = okhttp3.Request.Builder()
                    .url(request.completeUrl)
                    .put(okhttp3.RequestBody.create(
                        "text/xml".toMediaTypeOrNull(),
                        changesetXml
                    ))
                    .headers(okhttp3.Headers.headersOf(*request.headers.flatMap { listOf(it.key, it.value) }.toTypedArray()))
                    .build()
                    
                val response = okHttpClient.newCall(okHttpRequest).execute()
                
                if (response.isSuccessful) {
                    response.body?.string()?.toLongOrNull()
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    
    /**
     * Create a node (camera)
     */
    private suspend fun createNode(changesetId: Long, cameraData: CameraFormData): String? {
        return withContext(Dispatchers.IO) {
            try {
                val tokens = preferencesManager.getOAuthTokens() ?: return@withContext null
                
                val nodeXml = NodeXmlBuilder.build(
                    changesetId = changesetId,
                    latitude = cameraData.latitude,
                    longitude = cameraData.longitude,
                    tags = cameraData.toOsmTags()
                )
                
                // Sign the request
                val request = oauthService.signRequest(
                    url = "${OSMApiService.BASE_URL}api/0.6/node/create",
                    method = com.github.scribejava.core.model.Verb.PUT,
                    tokens = tokens
                )
                
                request.setPayload(nodeXml)
                request.addHeader("Content-Type", "text/xml")
                
                // Execute with OkHttp
                val okHttpClient = okhttp3.OkHttpClient()
                val okHttpRequest = okhttp3.Request.Builder()
                    .url(request.completeUrl)
                    .put(okhttp3.RequestBody.create(
                        "text/xml".toMediaTypeOrNull(),
                        nodeXml
                    ))
                    .headers(okhttp3.Headers.headersOf(*request.headers.flatMap { listOf(it.key, it.value) }.toTypedArray()))
                    .build()
                    
                val response = okHttpClient.newCall(okHttpRequest).execute()
                
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    
    /**
     * Close a changeset
     */
    private suspend fun closeChangeset(changesetId: Long) {
        withContext(Dispatchers.IO) {
            try {
                val tokens = preferencesManager.getOAuthTokens() ?: return@withContext
                
                val request = oauthService.signRequest(
                    url = "${OSMApiService.BASE_URL}api/0.6/changeset/$changesetId/close",
                    method = com.github.scribejava.core.model.Verb.PUT,
                    tokens = tokens
                )
                
                // Execute with OkHttp
                val okHttpClient = okhttp3.OkHttpClient()
                val okHttpRequest = okhttp3.Request.Builder()
                    .url(request.completeUrl)
                    .put(okhttp3.RequestBody.create(null, ByteArray(0)))
                    .headers(okhttp3.Headers.headersOf(*request.headers.flatMap { listOf(it.key, it.value) }.toTypedArray()))
                    .build()
                    
                okHttpClient.newCall(okHttpRequest).execute()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}


