package com.osmcamera.mapper.data.auth

import android.util.Base64
import com.osmcamera.mapper.data.model.OAuthTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OAuth 2.0 service for OpenStreetMap authentication (PKCE flow)
 */
@Singleton
class OAuthService @Inject constructor() {
    
    private var codeVerifier: String? = null
    private var state: String? = null
    
    private val httpClient = OkHttpClient()
    
    companion object {
        // OAuth 2.0 credentials for BalanceTaCam
        private const val CLIENT_ID = "Az0_KWIqrRu2kW4xKIqskGyUDMVyaoaTVIAACBBE-Qs"
        private const val CLIENT_SECRET = "ISw-8waN1PKLHLfZi3v4AMq28CRpS5MUW5LADhgng44"
        private const val REDIRECT_URI = "osmcamera://oauth"
        
        // OSM OAuth 2.0 endpoints
        private const val AUTHORIZE_URL = "https://www.openstreetmap.org/oauth2/authorize"
        private const val TOKEN_URL = "https://www.openstreetmap.org/oauth2/token"
        
        // Generate random string for PKCE
        private fun generateRandomString(length: Int = 43): String {
            val random = SecureRandom()
            val bytes = ByteArray(length)
            random.nextBytes(bytes)
            return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
                .take(length)
        }
    }
    
    /**
     * Initialize OAuth service
     */
    fun initialize() {
        // Generate PKCE code verifier
        codeVerifier = generateRandomString(43)
        state = generateRandomString(16)
    }
    
    /**
     * Get authorization URL for OAuth 2.0
     */
    suspend fun getAuthorizationUrl(): String = withContext(Dispatchers.IO) {
        val verifier = codeVerifier ?: throw IllegalStateException("OAuth not initialized")
        
        // Build authorization URL
        val url = StringBuilder(AUTHORIZE_URL)
        url.append("?client_id=").append(CLIENT_ID)
        url.append("&redirect_uri=").append(REDIRECT_URI)
        url.append("&response_type=code")
        url.append("&scope=read_prefs write_api")
        url.append("&state=").append(state)
        
        url.toString()
    }
    
    /**
     * Exchange authorization code for access token
     */
    suspend fun getAccessToken(code: String): OAuthTokens = withContext(Dispatchers.IO) {
        val requestBody = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("client_id", CLIENT_ID)
            .add("client_secret", CLIENT_SECRET)
            .add("redirect_uri", REDIRECT_URI)
            .build()
        
        val request = Request.Builder()
            .url(TOKEN_URL)
            .post(requestBody)
            .build()
        
        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response")
        
        if (!response.isSuccessful) {
            throw Exception("Token request failed: $responseBody")
        }
        
        val json = JSONObject(responseBody)
        val accessToken = json.getString("access_token")
        
        OAuthTokens(
            accessToken = accessToken,
            accessTokenSecret = "" // Not used in OAuth 2.0
        )
    }
    
    /**
     * Get authenticated request headers
     */
    fun getAuthHeaders(accessToken: String): Map<String, String> {
        return mapOf(
            "Authorization" to "Bearer $accessToken"
        )
    }
    
    /**
     * Clean up
     */
    fun cleanup() {
        codeVerifier = null
        state = null
    }
}


