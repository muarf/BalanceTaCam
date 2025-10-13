package com.osmcamera.mapper.presentation.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osmcamera.mapper.data.location.LocationService
import com.osmcamera.mapper.data.model.Camera
import com.osmcamera.mapper.data.repository.CameraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import javax.inject.Inject

/**
 * ViewModel for the map screen
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val cameraRepository: CameraRepository,
    private val locationService: LocationService
) : ViewModel() {
    
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
    
    private val _isLoadingCameras = MutableStateFlow(false)
    val isLoadingCameras: StateFlow<Boolean> = _isLoadingCameras.asStateFlow()
    
    init {
        _uiState.value = MapUiState.Ready
        getUserLocation()
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
            _isLoadingCameras.value = true
            try {
                val result = cameraRepository.fetchCamerasFromOverpass(south, west, north, east)
                if (result.isSuccess) {
                    _cameras.value = result.getOrNull() ?: emptyList()
                }
            } catch (e: Exception) {
                // Error loading cameras - keep existing cameras
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


