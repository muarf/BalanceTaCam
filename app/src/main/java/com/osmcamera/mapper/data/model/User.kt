package com.osmcamera.mapper.data.model

/**
 * OSM User information
 */
data class User(
    val id: Long,
    val displayName: String,
    val accountCreated: String? = null,
    val description: String? = null,
    val contributor: Boolean = false,
    val changesetCount: Int = 0
)

/**
 * OAuth tokens for authentication
 */
data class OAuthTokens(
    val accessToken: String,
    val accessTokenSecret: String,
    val refreshToken: String? = null,
    val expiresAt: Long? = null,
    val userId: Long? = null,
    val userName: String? = null
) {
    fun isExpired(): Boolean {
        val exp = expiresAt ?: return false
        return System.currentTimeMillis() >= exp
    }
}


