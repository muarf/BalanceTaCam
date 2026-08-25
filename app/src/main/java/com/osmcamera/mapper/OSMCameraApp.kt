package com.osmcamera.mapper

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import com.osmcamera.mapper.offline.CameraSyncWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import javax.inject.Inject

/**
 * Main Application class for OSM Camera Mapper
 * Initializes Hilt DI and osmdroid configuration
 */
@HiltAndroidApp
class OSMCameraApp : Application(), androidx.work.Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var preferences: com.osmcamera.mapper.data.local.PreferencesManager

    override fun onCreate() {
        super.onCreate()

        // Configure osmdroid
        Configuration.getInstance().apply {
            userAgentValue = "${packageName}/${BuildConfig.VERSION_NAME}"
            // Set cache path
            osmdroidBasePath = filesDir
            osmdroidTileCache = getExternalFilesDir(null)
        }

        // Route API traffic through Tor (Orbot SOCKS5) when the user enables it
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            preferences.torProxyEnabled.collect { enabled ->
                com.osmcamera.mapper.offline.TorProxyHolder.enabled = enabled
            }
        }

        // Weekly camera database refresh (unmetered networks only)
        CameraSyncWorker.schedule(this)
    }

    override val workManagerConfiguration: androidx.work.Configuration
        get() = androidx.work.Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}


