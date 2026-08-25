package com.osmcamera.mapper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osmcamera.mapper.offline.BasemapInfo
import com.osmcamera.mapper.offline.DownloadState
import com.osmcamera.mapper.offline.OfflineRegionManager
import com.osmcamera.mapper.offline.RegionInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

data class RegionUiState(
    val regions: List<RegionInfo> = emptyList(),
    val basemaps: List<BasemapInfo> = emptyList(),
    val installed: Set<String> = emptySet(),
    val installedBasemaps: Set<String> = emptySet(),
    val updatable: Set<String> = emptySet(),
    val updatableBasemaps: Set<String> = emptySet(),
    val loading: Boolean = true,
    val error: String? = null,
    val downloadingId: String? = null,
    val downloadProgress: Float = 0f,
    val statusText: String = ""
)

@HiltViewModel
class OfflineRegionsViewModel @Inject constructor(
    private val regionManager: OfflineRegionManager
) : ViewModel() {

    private val _state = MutableStateFlow(RegionUiState())
    val state: StateFlow<RegionUiState> = _state

    init {
        refresh()
        viewModelScope.launch {
            regionManager.downloadState.collect { dl ->
                when (dl) {
                    is DownloadState.Downloading -> {
                        _state.value = _state.value.copy(downloadProgress = dl.progress)
                    }
                    is DownloadState.Extracting -> {
                        _state.value = _state.value.copy(
                            statusText = "Extraction ${dl.fileIndex}/${dl.fileCount}…"
                        )
                    }
                    DownloadState.Done -> {
                        _state.value = _state.value.copy(
                            downloadingId = null,
                            statusText = "",
                            installed = regionManager.installedRegionIds().toSet(),
                            installedBasemaps = regionManager.installedBasemapIds().toSet(),
                            updatable = computeUpdatable(_state.value.regions, regionManager.installedRegionIds().toSet()),
                            updatableBasemaps = computeUpdatableBasemaps(_state.value.basemaps)
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            regionManager.fetchManifest().fold(
                onSuccess = { regions ->
                    val installed = regionManager.installedRegionIds().toSet()
                    val basemaps = regionManager.cachedBasemaps.value
                    _state.value = RegionUiState(
                        regions = regions,
                        basemaps = basemaps,
                        installed = installed,
                        installedBasemaps = regionManager.installedBasemapIds().toSet(),
                        updatable = computeUpdatable(regions, installed),
                        updatableBasemaps = computeUpdatableBasemaps(basemaps),
                        loading = false
                    )
                },
                onFailure = { e ->
                    _state.value = RegionUiState(
                        regions = emptyList(),
                        installed = regionManager.installedRegionIds().toSet(),
                        loading = false,
                        error = "Impossible de charger la liste : ${e.message}. Vérifiez la connexion."
                    )
                }
            )
        }
    }

    private fun computeUpdatable(regions: List<RegionInfo>, installed: Set<String>): Set<String> {
        return regions.filter { region ->
            region.id in installed && regionManager.installedGraphUrl(region.id)?.let { it != region.graphUrl } == true
        }.map { it.id }.toSet()
    }

    private fun computeUpdatableBasemaps(basemaps: List<BasemapInfo>): Set<String> {
        return basemaps.filter { b ->
            b.id in regionManager.installedBasemapIds()
                && regionManager.installedBasemapUrl(b.id)?.let { it != b.url } == true
        }.map { it.id }.toSet()
    }

    fun download(region: RegionInfo, overwrite: Boolean = false) {
        if (_state.value.downloadingId != null) return
        beginDownload(region.id, region.graphBytes)
        viewModelScope.launch {
            regionManager.downloadRegion(region, overwrite)
            finishDownload(region.id)
        }
    }

    fun downloadBasemap(basemap: BasemapInfo, overwrite: Boolean = false) {
        if (_state.value.downloadingId != null) return
        beginDownload(basemap.id, basemap.bytes)
        viewModelScope.launch {
            regionManager.downloadBasemap(basemap, overwrite)
            finishDownload(basemap.id)
        }
    }

    fun delete(region: RegionInfo) {
        regionManager.deleteRegion(region.id)
        _state.value = _state.value.copy(installed = regionManager.installedRegionIds().toSet())
    }

    fun deleteBasemap(basemap: BasemapInfo) {
        regionManager.deleteBasemap(basemap.id)
        _state.value = _state.value.copy(installedBasemaps = regionManager.installedBasemapIds().toSet())
    }

    private fun beginDownload(id: String, bytes: Long) {
        _state.value = _state.value.copy(downloadingId = id, downloadProgress = 0f, statusText = formatBytes(0, bytes))
    }

    private suspend fun finishDownload(id: String) {
        val done = regionManager.graphCacheDir(id) != null || regionManager.basemapFile(id) != null
        if (!done) {
            _state.value = _state.value.copy(downloadingId = null, statusText = "")
        } else {
            val installedRegions = regionManager.installedRegionIds().toSet()
            val installedMaps = regionManager.installedBasemapIds().toSet()
            _state.value = _state.value.copy(
                downloadingId = null, statusText = "",
                installed = installedRegions,
                installedBasemaps = installedMaps,
                updatable = computeUpdatable(_state.value.regions, installedRegions),
                updatableBasemaps = computeUpdatableBasemaps(_state.value.basemaps)
            )
        }
    }

    private fun formatBytes(done: Long, total: Long): String {
        val nf = NumberFormat.getPercentInstance(Locale.FRENCH)
        return "Téléchargement ${nf.format(if (total > 0) done.toDouble() / total else 0.0)}"
    }
}
