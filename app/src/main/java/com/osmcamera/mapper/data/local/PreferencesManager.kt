package com.osmcamera.mapper.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.osmcamera.mapper.data.model.OAuthTokens
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Manages app preferences and secure storage
 */
@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore
    
    // Encrypted SharedPreferences for OAuth tokens
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "oauth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    // Preference keys
    private object PreferencesKeys {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME = stringPreferencesKey("theme")
        val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        val OFFLINE_MODE = booleanPreferencesKey("offline_mode")
        val TOR_PROXY_ENABLED = booleanPreferencesKey("tor_proxy_enabled")
    }
    
    // OAuth token keys
    private object OAuthKeys {
        const val ACCESS_TOKEN = "access_token"
        const val ACCESS_TOKEN_SECRET = "access_token_secret"
        const val REFRESH_TOKEN = "refresh_token"
        const val EXPIRES_AT = "expires_at"
        const val USER_ID = "user_id"
        const val USER_NAME = "user_name"
    }
    
    // Language preference
    val language: Flow<String?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LANGUAGE]
    }
    
    suspend fun setLanguage(language: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = language
        }
    }
    
    // Theme preference
    val theme: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.THEME] ?: "system"
    }
    
    suspend fun setTheme(theme: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme
        }
    }
    
    // First launch flag
    val isFirstLaunch: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.FIRST_LAUNCH] ?: true
    }
    
    suspend fun setFirstLaunchComplete() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FIRST_LAUNCH] = false
        }
    }

    // Offline routing mode
    val offlineMode: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.OFFLINE_MODE] ?: false
    }

    suspend fun setOfflineMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.OFFLINE_MODE] = enabled
        }
    }

    // Tor (Orbot) proxy for privacy-sensitive network calls
    val torProxyEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.TOR_PROXY_ENABLED] ?: false
    }

    suspend fun setTorProxyEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TOR_PROXY_ENABLED] = enabled
        }
    }
    
    // OAuth tokens (encrypted)
    fun saveOAuthTokens(tokens: OAuthTokens) {
        encryptedPrefs.edit().apply {
            putString(OAuthKeys.ACCESS_TOKEN, tokens.accessToken)
            putString(OAuthKeys.ACCESS_TOKEN_SECRET, tokens.accessTokenSecret)
            tokens.refreshToken?.let { putString(OAuthKeys.REFRESH_TOKEN, it) }
            tokens.expiresAt?.let { putLong(OAuthKeys.EXPIRES_AT, it) }
            tokens.userId?.let { putLong(OAuthKeys.USER_ID, it) }
            tokens.userName?.let { putString(OAuthKeys.USER_NAME, it) }
            apply()
        }
    }
    
    fun getOAuthTokens(): OAuthTokens? {
        val accessToken = encryptedPrefs.getString(OAuthKeys.ACCESS_TOKEN, null)
        val accessTokenSecret = encryptedPrefs.getString(OAuthKeys.ACCESS_TOKEN_SECRET, null)
        
        if (accessToken != null && accessTokenSecret != null) {
            return OAuthTokens(
                accessToken = accessToken,
                accessTokenSecret = accessTokenSecret,
                refreshToken = encryptedPrefs.getString(OAuthKeys.REFRESH_TOKEN, null),
                expiresAt = if (encryptedPrefs.contains(OAuthKeys.EXPIRES_AT)) {
                    encryptedPrefs.getLong(OAuthKeys.EXPIRES_AT, 0)
                } else null,
                userId = if (encryptedPrefs.contains(OAuthKeys.USER_ID)) {
                    encryptedPrefs.getLong(OAuthKeys.USER_ID, 0)
                } else null,
                userName = encryptedPrefs.getString(OAuthKeys.USER_NAME, null)
            )
        }
        
        return null
    }
    
    fun clearOAuthTokens() {
        encryptedPrefs.edit().apply {
            remove(OAuthKeys.ACCESS_TOKEN)
            remove(OAuthKeys.ACCESS_TOKEN_SECRET)
            remove(OAuthKeys.REFRESH_TOKEN)
            remove(OAuthKeys.EXPIRES_AT)
            remove(OAuthKeys.USER_ID)
            remove(OAuthKeys.USER_NAME)
            apply()
        }
    }
    
    fun isAuthenticated(): Boolean {
        return getOAuthTokens() != null
    }
}


