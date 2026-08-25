package com.osmcamera.mapper.data.repository

import com.google.gson.JsonParser
import com.osmcamera.mapper.data.api.OSMApiService
import com.osmcamera.mapper.data.auth.OAuthService
import com.osmcamera.mapper.data.local.PreferencesManager
import com.osmcamera.mapper.data.model.OAuthTokens
import com.osmcamera.mapper.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for authentication operations
 */
@Singleton
class AuthRepository @Inject constructor(
    private val oauthService: OAuthService,
    private val osmApiService: OSMApiService,
    private val preferencesManager: PreferencesManager
) {
    
    init {
        oauthService.initialize()
    }
    
    /**
     * Start OAuth authentication flow
     */
    suspend fun startAuthentication(): String {
        return oauthService.getAuthorizationUrl()
    }
    
    /**
     * Complete OAuth authentication with authorization code
     */
    suspend fun completeAuthentication(code: String): Result<OAuthTokens> {
        return withContext(Dispatchers.IO) {
            try {
                val tokens = oauthService.getAccessToken(code)
                
                // Save tokens
                preferencesManager.saveOAuthTokens(tokens)
                
                // Fetch user details to update tokens with user info
                val user = getUserDetails()
                if (user != null) {
                    val updatedTokens = tokens.copy(
                        userId = user.id,
                        userName = user.displayName
                    )
                    preferencesManager.saveOAuthTokens(updatedTokens)
                    Result.success(updatedTokens)
                } else {
                    Result.success(tokens)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get user details from OSM API
     */
    suspend fun getUserDetails(): User? {
        return withContext(Dispatchers.IO) {
            try {
                val tokens = preferencesManager.getOAuthTokens() ?: return@withContext null
                
                // Create authenticated request
                val okHttpClient = okhttp3.OkHttpClient()
                val headers = oauthService.getAuthHeaders(tokens.accessToken)
                
                val okHttpRequest = okhttp3.Request.Builder()
                    .url("${OSMApiService.BASE_URL}api/0.6/user/details.json")
                    .apply {
                        headers.forEach { (key, value) ->
                            addHeader(key, value)
                        }
                    }
                    .build()
                    
                val response = okHttpClient.newCall(okHttpRequest).execute()
                if (response.isSuccessful) {
                    val json = JsonParser.parseString(response.body?.string() ?: "{}").asJsonObject
                    val userObj = json.getAsJsonObject("user")
                    
                    User(
                        id = userObj.get("id")?.asLong ?: 0,
                        displayName = userObj.get("display_name")?.asString ?: "",
                        accountCreated = userObj.get("account_created")?.asString,
                        description = userObj.get("description")?.asString,
                        changesetCount = userObj.getAsJsonObject("changesets")
                            ?.get("count")?.asInt ?: 0
                    )
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
     * Check if user is authenticated
     */
    fun isAuthenticated(): Boolean {
        return preferencesManager.isAuthenticated()
    }
    
    /**
     * Ensure token is valid — refresh silently if expired
     */
    suspend fun ensureValidToken(): Boolean {
        val tokens = preferencesManager.getOAuthTokens() ?: return false
        if (!tokens.isExpired()) return true
        val rt = tokens.refreshToken ?: return false
        return try {
            val newTokens = oauthService.refreshAccessToken(rt)
            val updated = newTokens.copy(
                userId = tokens.userId,
                userName = tokens.userName
            )
            preferencesManager.saveOAuthTokens(updated)
            true
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Token refresh failed", e)
            false
        }
    }
    
    /**
     * Get stored OAuth tokens
     */
    fun getTokens(): OAuthTokens? {
        return preferencesManager.getOAuthTokens()
    }
    
    /**
     * Logout user
     */
    fun logout() {
        preferencesManager.clearOAuthTokens()
        oauthService.cleanup()
        oauthService.initialize()
    }
}


