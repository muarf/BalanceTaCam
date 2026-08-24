package com.osmcamera.mapper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val installed: Set<String> = emptySet(),
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
                        _state.value = _state.value.copy(
                            downloadingId = _state.value.downloadingId,
                            downloadProgress = dl.progress
                        )
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
                            installed = regionManager.installedRegionIds().toSet()
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
                    _state.value = RegionUiState(
                        regions = regions,
                        installed = regionManager.installedRegionIds().toSet(),
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

    fun download(region: RegionInfo) {
        if (_state.value.downloadingId != null) return
        _state.value = _state.value.copy(
            downloadingId = region.id,
            downloadProgress = 0f,
            statusText = formatBytes(0, region.graphBytes)
        )
        viewModelScope.launch {
            regionManager.downloadRegion(region)
            // final state applied by the downloadState collector; refresh anyway on failure
            if (regionManager.graphCacheDir(region.id) == null) {
                _state.value = _state.value.copy(downloadingId = null, statusText = "")
            } else {
                _state.value = _state.value.copy(
                    downloadingId = null, statusText = "",
                    installed = regionManager.installedRegionIds().toSet()
                )
            }
        }
    }

    fun delete(region: RegionInfo) {
        regionManager.deleteRegion(region.id)
        _state.value = _state.value.copy(installed = regionManager.installedRegionIds().toSet())
    }

    private fun formatBytes(done: Long, total: Long): String {
        val nf = NumberFormat.getPercentInstance(Locale.FRENCH)
        return "Téléchargement ${nf.format(if (total > 0) done.toDouble() / total else 0.0)}"
    }
}
