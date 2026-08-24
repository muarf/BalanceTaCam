package com.osmcamera.mapper

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import com.osmcamera.mapper.offline.CameraSyncWorker
import dagger.hilt.android.HiltAndroidApp
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
    
    override fun onCreate() {
        super.onCreate()
        
        // Configure osmdroid
        Configuration.getInstance().apply {
            userAgentValue = "${packageName}/${BuildConfig.VERSION_NAME}"
            // Set cache path
            osmdroidBasePath = filesDir
            osmdroidTileCache = getExternalFilesDir(null)
        }

        // Weekly camera database refresh (unmetered networks only)
        CameraSyncWorker.schedule(this)
    }

    override val workManagerConfiguration: androidx.work.Configuration
        get() = androidx.work.Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}


