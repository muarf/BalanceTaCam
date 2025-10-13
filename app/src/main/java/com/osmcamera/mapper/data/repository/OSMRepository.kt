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
                android.util.Log.d("BalanceTaCam", "=== Starting camera creation ===")
                
                // Validate
                val validation = cameraData.validate()
                if (!validation.isValid) {
                    android.util.Log.e("BalanceTaCam", "Validation failed: ${validation.error}")
                    return@withContext Result.failure(Exception(validation.error))
                }
                android.util.Log.d("BalanceTaCam", "✓ Validation OK")
                
                // Get OAuth tokens
                val tokens = preferencesManager.getOAuthTokens()
                if (tokens == null) {
                    android.util.Log.e("BalanceTaCam", "No OAuth tokens found")
                    return@withContext Result.failure(Exception("Not authenticated"))
                }
                android.util.Log.d("BalanceTaCam", "✓ OAuth tokens found")
                
                // 1. Create changeset
                android.util.Log.d("BalanceTaCam", "Creating changeset...")
                val changesetId = createChangeset()
                if (changesetId == null) {
                    android.util.Log.e("BalanceTaCam", "Changeset creation failed")
                    return@withContext Result.failure(Exception("Failed to create changeset - Check OAuth permissions"))
                }
                android.util.Log.d("BalanceTaCam", "✓ Changeset created: $changesetId")
                
                try {
                    // 2. Create node
                    android.util.Log.d("BalanceTaCam", "Creating node...")
                    android.util.Log.d("BalanceTaCam", "Position: ${cameraData.latitude}, ${cameraData.longitude}")
                    android.util.Log.d("BalanceTaCam", "Tags: ${cameraData.toOsmTags()}")
                    
                    val nodeId = createNode(changesetId, cameraData)
                    if (nodeId == null) {
                        android.util.Log.e("BalanceTaCam", "Node creation failed")
                        return@withContext Result.failure(Exception("Failed to create node - Check coordinates and tags"))
                    }
                    android.util.Log.d("BalanceTaCam", "✓ Node created: $nodeId")
                    
                    // 3. Close changeset
                    android.util.Log.d("BalanceTaCam", "Closing changeset...")
                    closeChangeset(changesetId)
                    android.util.Log.d("BalanceTaCam", "✓ Changeset closed")
                    android.util.Log.d("BalanceTaCam", "=== Camera creation SUCCESS ===")
                    
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
                    comment = "Added surveillance camera via BalanceTaCam",
                    source = "survey",
                    createdBy = "BalanceTaCam v${BuildConfig.VERSION_NAME}"
                )
                
                // Create authenticated request with OAuth 2.0
                val okHttpClient = okhttp3.OkHttpClient()
                val headers = oauthService.getAuthHeaders(tokens.accessToken)
                
                val okHttpRequest = okhttp3.Request.Builder()
                    .url("${OSMApiService.BASE_URL}api/0.6/changeset/create")
                    .put(okhttp3.RequestBody.create(
                        "text/xml".toMediaTypeOrNull(),
                        changesetXml
                    ))
                    .apply {
                        headers.forEach { (key, value) ->
                            addHeader(key, value)
                        }
                    }
                    .build()
                    
                val response = okHttpClient.newCall(okHttpRequest).execute()
                
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    android.util.Log.d("BalanceTaCam", "Changeset response: $body")
                    body?.toLongOrNull()
                } else {
                    val error = response.body?.string()
                    android.util.Log.e("BalanceTaCam", "Changeset creation failed: ${response.code} - $error")
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
                
                // Create authenticated request with OAuth 2.0
                val okHttpClient = okhttp3.OkHttpClient()
                val headers = oauthService.getAuthHeaders(tokens.accessToken)
                
                val okHttpRequest = okhttp3.Request.Builder()
                    .url("${OSMApiService.BASE_URL}api/0.6/node/create")
                    .put(okhttp3.RequestBody.create(
                        "text/xml".toMediaTypeOrNull(),
                        nodeXml
                    ))
                    .apply {
                        headers.forEach { (key, value) ->
                            addHeader(key, value)
                        }
                    }
                    .build()
                    
                val response = okHttpClient.newCall(okHttpRequest).execute()
                
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    android.util.Log.d("BalanceTaCam", "Node response: $body")
                    body
                } else {
                    val error = response.body?.string()
                    android.util.Log.e("BalanceTaCam", "Node creation failed: ${response.code} - $error")
                    null
                }
            } catch (e: Exception) {
                android.util.Log.e("BalanceTaCam", "Node creation exception", e)
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
                
                // Create authenticated request with OAuth 2.0
                val okHttpClient = okhttp3.OkHttpClient()
                val headers = oauthService.getAuthHeaders(tokens.accessToken)
                
                val okHttpRequest = okhttp3.Request.Builder()
                    .url("${OSMApiService.BASE_URL}api/0.6/changeset/$changesetId/close")
                    .put(okhttp3.RequestBody.create(null, ByteArray(0)))
                    .apply {
                        headers.forEach { (key, value) ->
                            addHeader(key, value)
                        }
                    }
                    .build()
                    
                okHttpClient.newCall(okHttpRequest).execute()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}


