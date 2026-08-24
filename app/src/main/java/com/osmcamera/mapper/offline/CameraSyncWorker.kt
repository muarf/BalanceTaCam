package com.osmcamera.mapper.offline

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.osmcamera.mapper.data.local.PreferencesManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Weekly camera database refresh (Overpass) with optional Tor proxy.
 * Runs on unmetered networks only.
 */
@HiltWorker
class CameraSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val cameraRepository: com.osmcamera.mapper.data.repository.CameraRepository,
    private val preferencesManager: PreferencesManager
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "CameraSync"
        private const val WORK_NAME = "camera_sync_weekly"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<CameraSyncWorker>(7, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.UNMETERED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.i(TAG, "Sync hebdomadaire planifiée")
        }
    }

    override suspend fun doWork(): Result {
        return try {
            Log.i(TAG, "Début sync caméras (Tor: ${preferencesManager.torProxyEnabled.first()})")
            // Refresh the Paris/ÎdF area used by routing; Tor routing is applied
            // at the OkHttp level via proxy settings when enabled.
            cameraRepository.fetchCamerasFromOverpass(
                south = 48.81, west = 2.22, north = 48.90, east = 2.47
            )
            Log.i(TAG, "Sync caméras terminée")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Échec sync caméras", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
