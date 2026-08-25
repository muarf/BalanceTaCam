package com.osmcamera.mapper.presentation.screens.map

import android.app.Application
import android.util.Log
import org.mapsforge.map.rendertheme.InternalRenderTheme
import org.osmdroid.mapsforge.MapsForgeTileProvider
import org.osmdroid.mapsforge.MapsForgeTileSource
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver
import org.osmdroid.views.MapView
import java.io.File

object OfflineBasemapHelper {

    private const val TAG = "OfflineBasemap"

    private var graphicFactoryReady = false

    data class ForgeMap(val source: MapsForgeTileSource, val provider: MapsForgeTileProvider)

    /**
     * Prépare la carte vectorielle si et seulement si on veut l'affichage
     * hors-ligne : mode hors-ligne actif, ou pas de réseau validé (Auto).
     * Sinon renvoie null -> la carte en ligne (raster MAPNIK) est utilisée.
     */
    fun prepare(
        context: android.content.Context,
        mapFiles: List<File>,
        lat: Double,
        lon: Double,
        preferOffline: Boolean
    ): ForgeMap? {
        if (mapFiles.isEmpty()) return null
        if (!preferOffline && isOnline(context)) {
            Log.i(TAG, "Réseau disponible + mode Auto : carte en ligne MAPNIK")
            return null
        }
        return try {
            if (!graphicFactoryReady) {
                MapsForgeTileSource.createInstance(context.applicationContext as Application)
                graphicFactoryReady = true
            }
            val source = MapsForgeTileSource.createFromFiles(
                mapFiles.toTypedArray(),
                InternalRenderTheme.DEFAULT,
                "mapsforge"
            )
            val bounds = source.boundsOsmdroid
            if (!bounds.contains(lat, lon)) {
                Log.i(TAG, "Carte hors-ligne installée mais ne couvre pas ($lat,$lon), bounds=$bounds")
                source.dispose()
                return null
            }
            val provider = MapsForgeTileProvider(SimpleRegisterReceiver(context), source, null)
            Log.i(TAG, "Carte hors-ligne prête : ${mapFiles.size} fichier(s), bounds=$bounds")
            ForgeMap(source, provider)
        } catch (e: Exception) {
            Log.e(TAG, "Préparation carte hors-ligne impossible", e)
            null
        }
    }

    private fun isOnline(context: android.content.Context): Boolean = try {
        val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE)
                as android.net.ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        caps != null && caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    } catch (e: Exception) {
        false
    }

    fun dispose(source: MapsForgeTileSource?) {
        try {
            source?.dispose()
        } catch (e: Exception) {
            Log.w(TAG, "dispose carte: ${e.message}")
        }
    }
}
