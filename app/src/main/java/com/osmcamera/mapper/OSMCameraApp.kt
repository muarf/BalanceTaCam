package com.osmcamera.mapper

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

/**
 * Main Application class for OSM Camera Mapper
 * Initializes Hilt DI and osmdroid configuration
 */
@HiltAndroidApp
class OSMCameraApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Configure osmdroid
        Configuration.getInstance().apply {
            userAgentValue = "${packageName}/${BuildConfig.VERSION_NAME}"
            // Set cache path
            osmdroidBasePath = filesDir
            osmdroidTileCache = getExternalFilesDir(null)
        }
    }
}


