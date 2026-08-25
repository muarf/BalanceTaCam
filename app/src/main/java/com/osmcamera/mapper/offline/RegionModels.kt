package com.osmcamera.mapper.offline

/**
 * Models for downloadable offline regions (routing graphs)
 */
data class RegionsManifest(
    val version: Int = 1,
    val regions: List<RegionInfo> = emptyList(),
    val basemaps: List<BasemapInfo> = emptyList()
)

data class BasemapInfo(
    val id: String,
    val name: String,
    val url: String,
    val bytes: Long,
    val bbox: List<Double>
) {
    fun contains(lat: Double, lon: Double): Boolean {
        if (bbox.size < 4) return false
        return lat >= bbox[0] && lon >= bbox[1] && lat <= bbox[2] && lon <= bbox[3]
    }
}

data class RegionInfo(
    val id: String,
    val name: String,
    val country: String,
    val graphUrl: String,
    val graphBytes: Long,
    val bbox: List<Double>,
    val builtAt: String
) {
    fun contains(lat: Double, lon: Double): Boolean {
        if (bbox.size < 4) return false
        return lat >= bbox[0] && lon >= bbox[1] && lat <= bbox[2] && lon <= bbox[3]
    }
}

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : DownloadState()
    data class Extracting(val fileIndex: Int, val fileCount: Int) : DownloadState()
    data class DownloadingTiles(val tilesDone: Int, val tilesTotal: Int) : DownloadState()
    object Done : DownloadState()
    data class Error(val message: String) : DownloadState()
}
