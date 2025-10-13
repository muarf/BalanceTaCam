package com.osmcamera.mapper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osmcamera.mapper.data.model.RouteComparison
import com.osmcamera.mapper.data.repository.RoutingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import javax.inject.Inject

/**
 * ViewModel for routing with camera avoidance
 */
@HiltViewModel
class RoutingViewModel @Inject constructor(
    private val routingRepository: RoutingRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<RoutingUiState>(RoutingUiState.Idle)
    val uiState: StateFlow<RoutingUiState> = _uiState.asStateFlow()
    
    private val _startPoint = MutableStateFlow<GeoPoint?>(null)
    val startPoint: StateFlow<GeoPoint?> = _startPoint.asStateFlow()
    
    private val _endPoint = MutableStateFlow<GeoPoint?>(null)
    val endPoint: StateFlow<GeoPoint?> = _endPoint.asStateFlow()
    
    fun setStartPoint(point: GeoPoint) {
        _startPoint.value = point
    }
    
    fun setEndPoint(point: GeoPoint) {
        _endPoint.value = point
    }
    
    fun clearPoints() {
        _startPoint.value = null
        _endPoint.value = null
        _uiState.value = RoutingUiState.Idle
    }
    
    fun calculateRoute() {
        val start = _startPoint.value
        val end = _endPoint.value
        
        if (start == null || end == null) {
            _uiState.value = RoutingUiState.Error("Points de départ et d'arrivée requis")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = RoutingUiState.Calculating
            
            try {
                val result = routingRepository.calculateAntiCameraRoutes(start, end)
                
                if (result.isSuccess) {
                    val comparison = result.getOrNull()!!
                    _uiState.value = RoutingUiState.RoutesCalculated(comparison)
                } else {
                    _uiState.value = RoutingUiState.Error(
                        result.exceptionOrNull()?.message ?: "Échec du calcul d'itinéraire"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = RoutingUiState.Error(e.message ?: "Erreur inconnue")
            }
        }
    }
}

sealed class RoutingUiState {
    object Idle : RoutingUiState()
    object Calculating : RoutingUiState()
    data class RoutesCalculated(val comparison: RouteComparison) : RoutingUiState()
    data class Error(val message: String) : RoutingUiState()
}

