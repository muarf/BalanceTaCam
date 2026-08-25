package com.osmcamera.mapper.presentation.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osmcamera.mapper.data.location.LocationService
import com.osmcamera.mapper.data.model.Camera
import com.osmcamera.mapper.data.repository.CameraRepository
import com.osmcamera.mapper.offline.OfflineRegionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import javax.inject.Inject

/**
 * ViewModel for the map screen
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val cameraRepository: CameraRepository,
    private val locationService: LocationService,
    private val regionManager: OfflineRegionManager,
    preferences: com.osmcamera.mapper.data.local.PreferencesManager
) : ViewModel() {

    fun installedBasemapFiles(): List<java.io.File> = regionManager.installedBasemapFiles()

    fun tileCacheFile(regionId: String): java.io.File = regionManager.tileCacheFile(regionId)

    fun installedTileCacheIds(): List<String> = regionManager.installedTileCacheIds()

    fun findTileCacheFor(lat: Double, lon: Double): String? {
        return regionManager.installedTileCacheIds().firstOrNull { id ->
            regionManager.tileCacheFile(id).let { it.isFile && it.length() > 0 }
        }
    }

    val offlineMode: StateFlow<Boolean> = preferences.offlineMode
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, false)
    
    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Loading)
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()
    
    private val _cameras = MutableStateFlow<List<Camera>>(emptyList())
    val cameras: StateFlow<List<Camera>> = _cameras.asStateFlow()
    
    private val _userLocation = MutableStateFlow<GeoPoint?>(null)
    val userLocation: StateFlow<GeoPoint?> = _userLocation.asStateFlow()
    
    // Route display
    private val _selectedRoute = MutableStateFlow<com.osmcamera.mapper.data.model.Route?>(null)
    val selectedRoute: StateFlow<com.osmcamera.mapper.data.model.Route?> = _selectedRoute.asStateFlow()

    fun setSelectedRoute(route: com.osmcamera.mapper.data.model.Route?) {
        _selectedRoute.value = route
    }

    // Route points (start/end) - stored here because MapViewModel survives navigation
    // between the map and the routing screen, unlike RoutingViewModel
    private val _routeStartPoint = MutableStateFlow<GeoPoint?>(null)
    val routeStartPoint: StateFlow<GeoPoint?> = _routeStartPoint.asStateFlow()

    private val _routeEndPoint = MutableStateFlow<GeoPoint?>(null)
    val routeEndPoint: StateFlow<GeoPoint?> = _routeEndPoint.asStateFlow()

    // When non-null, the map is waiting for a tap to place this route point
    private val _routePickTarget = MutableStateFlow<RoutePickTarget?>(null)
    val routePickTarget: StateFlow<RoutePickTarget?> = _routePickTarget.asStateFlow()

    fun setRoutePickTarget(target: RoutePickTarget?) {
        _routePickTarget.value = target
    }

    fun setRoutePoint(target: RoutePickTarget, point: GeoPoint?) {
        when (target) {
            RoutePickTarget.START -> _routeStartPoint.value = point
            RoutePickTarget.END -> _routeEndPoint.value = point
        }
    }

    fun clearRoutePoints() {
        _routeStartPoint.value = null
        _routeEndPoint.value = null
    }

    private val _isLoadingCameras = MutableStateFlow(false)
    val isLoadingCameras: StateFlow<Boolean> = _isLoadingCameras.asStateFlow()
    
    init {
        _uiState.value = MapUiState.Ready
        getUserLocation()
        // Immediately load all cached cameras on launch so map is never empty
        viewModelScope.launch {
            try {
                val cached = cameraRepository.getAllCamerasList()
                if (cached.isNotEmpty()) {
                    _cameras.value = cached
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
    
    fun getUserLocation() {
        viewModelScope.launch {
            try {
                val location = locationService.getLastLocation() 
                    ?: locationService.getCurrentLocation()
                
                location?.let {
                    _userLocation.value = GeoPoint(it.latitude, it.longitude)
                }
            } catch (e: Exception) {
                // Silently fail - location is optional
            }
        }
    }
    
    fun loadCamerasInBounds(south: Double, west: Double, north: Double, east: Double) {
        viewModelScope.launch {
            // 1. Immediately show cached cameras in bounds (0 ms latency)
            val local = try {
                cameraRepository.getCamerasInBoundsList(south, west, north, east)
            } catch (e: Exception) {
                emptyList()
            }
            if (local.isNotEmpty()) {
                _cameras.value = local
            }
            
            // 2. Fetch fresh updates from Overpass in the background
            _isLoadingCameras.value = true
            try {
                val result = cameraRepository.fetchCamerasFromOverpass(south, west, north, east)
                if (result.isSuccess) {
                    val fresh = result.getOrNull()
                    if (!fresh.isNullOrEmpty()) {
                        _cameras.value = fresh
                    }
                }
            } catch (e: Exception) {
                // Keep existing cached cameras
            } finally {
                _isLoadingCameras.value = false
            }
        }
    }
    
    fun refreshCameras(south: Double, west: Double, north: Double, east: Double) {
        loadCamerasInBounds(south, west, north, east)
    }
}

sealed class MapUiState {
    object Loading : MapUiState()
    object Ready : MapUiState()
    data class Error(val message: String) : MapUiState()
}

/**
 * Which route point the user wants to pick by tapping the map
 */
enum class RoutePickTarget {
    START,
    END
}


