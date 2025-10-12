package com.osmcamera.mapper.data.auth

import com.github.scribejava.core.builder.ServiceBuilder
import com.github.scribejava.core.model.OAuth1AccessToken
import com.github.scribejava.core.model.OAuth1RequestToken
import com.github.scribejava.core.model.OAuthRequest
import com.github.scribejava.core.model.Verb
import com.github.scribejava.core.oauth.OAuth10aService
import com.osmcamera.mapper.data.model.OAuthTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OAuth 1.0a service for OpenStreetMap authentication
 */
@Singleton
class OAuthService @Inject constructor() {
    
    private var oauthService: OAuth10aService? = null
    private var requestToken: OAuth1RequestToken? = null
    
    companion object {
        private const val CONSUMER_KEY = "your_consumer_key" // Will be configured
        private const val CONSUMER_SECRET = "your_consumer_secret" // Will be configured
        private const val CALLBACK_URL = "osmcamera://oauth"
        
        // OSM OAuth endpoints
        private const val REQUEST_TOKEN_URL = "https://www.openstreetmap.org/oauth/request_token"
        private const val AUTHORIZE_URL = "https://www.openstreetmap.org/oauth/authorize"
        private const val ACCESS_TOKEN_URL = "https://www.openstreetmap.org/oauth/access_token"
    }
    
    /**
     * Initialize OAuth service
     */
    fun initialize() {
        oauthService = ServiceBuilder(CONSUMER_KEY)
            .apiSecret(CONSUMER_SECRET)
            .callback(CALLBACK_URL)
            .build(OSMOAuthApi.instance())
    }
    
    /**
     * Get authorization URL
     */
    suspend fun getAuthorizationUrl(): String = withContext(Dispatchers.IO) {
        val service = oauthService ?: throw IllegalStateException("OAuth service not initialized")
        requestToken = service.requestToken
        service.getAuthorizationUrl(requestToken)
    }
    
    /**
     * Exchange verifier for access token
     */
    suspend fun getAccessToken(verifier: String): OAuthTokens = withContext(Dispatchers.IO) {
        val service = oauthService ?: throw IllegalStateException("OAuth service not initialized")
        val reqToken = requestToken ?: throw IllegalStateException("Request token not found")
        
        val accessToken = service.getAccessToken(reqToken, verifier)
        
        OAuthTokens(
            accessToken = accessToken.token,
            accessTokenSecret = accessToken.tokenSecret
        )
    }
    
    /**
     * Sign a request with OAuth tokens
     */
    fun signRequest(
        url: String,
        method: Verb,
        tokens: OAuthTokens
    ): OAuthRequest {
        val service = oauthService ?: throw IllegalStateException("OAuth service not initialized")
        val accessToken = OAuth1AccessToken(tokens.accessToken, tokens.accessTokenSecret)
        
        val request = OAuthRequest(method, url)
        service.signRequest(accessToken, request)
        
        return request
    }
    
    /**
     * Clean up
     */
    fun cleanup() {
        oauthService?.close()
        oauthService = null
        requestToken = null
    }
}

/**
 * Custom OSM OAuth API implementation for ScribeJava
 */
class OSMOAuthApi private constructor() : com.github.scribejava.core.builder.api.DefaultApi10a() {
    
    override fun getRequestTokenEndpoint(): String = 
        "https://www.openstreetmap.org/oauth/request_token"
    
    override fun getAccessTokenEndpoint(): String = 
        "https://www.openstreetmap.org/oauth/access_token"
    
    override fun getAuthorizationBaseUrl(): String = 
        "https://www.openstreetmap.org/oauth/authorize"
    
    companion object {
        private val INSTANCE = OSMOAuthApi()
        
        fun instance(): OSMOAuthApi = INSTANCE
    }
}


