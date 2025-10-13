package com.osmcamera.mapper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osmcamera.mapper.data.api.NominatimApi
import com.osmcamera.mapper.data.model.RouteComparison
import com.osmcamera.mapper.data.repository.RoutingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val routingRepository: RoutingRepository,
    private val nominatimApi: NominatimApi
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<RoutingUiState>(RoutingUiState.Idle)
    val uiState: StateFlow<RoutingUiState> = _uiState.asStateFlow()
    
    private val _startPoint = MutableStateFlow<GeoPoint?>(null)
    val startPoint: StateFlow<GeoPoint?> = _startPoint.asStateFlow()
    
    private val _endPoint = MutableStateFlow<GeoPoint?>(null)
    val endPoint: StateFlow<GeoPoint?> = _endPoint.asStateFlow()
    
    private val _addressSearchResults = MutableStateFlow<List<String>>(emptyList())
    val addressSearchResults: StateFlow<List<String>> = _addressSearchResults.asStateFlow()
    
    private var searchJob: Job? = null
    
    fun setStartPoint(point: GeoPoint?) {
        _startPoint.value = point
    }
    
    fun setEndPoint(point: GeoPoint?) {
        _endPoint.value = point
    }
    
    /**
     * Search address using Nominatim
     */
    fun searchAddress(query: String, isStart: Boolean) {
        if (query.length < 3) return
        
        // Cancel previous search
        searchJob?.cancel()
        
        searchJob = viewModelScope.launch {
            delay(500) // Debounce
            
            try {
                val response = nominatimApi.search(query)
                if (response.isSuccessful) {
                    val results = response.body() ?: emptyList()
                    if (results.isNotEmpty()) {
                        val result = results.first()
                        val lat = result.lat.toDouble()
                        val lon = result.lon.toDouble()
                        
                        if (isStart) {
                            setStartPoint(GeoPoint(lat, lon))
                        } else {
                            setEndPoint(GeoPoint(lat, lon))
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("BalanceTaCam", "Address search failed", e)
            }
        }
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
            android.util.Log.d("BalanceTaCam", "ViewModel: Setting Calculating state")
            _uiState.value = RoutingUiState.Calculating
            
            try {
                android.util.Log.d("BalanceTaCam", "ViewModel: Calling repository")
                val result = routingRepository.calculateAntiCameraRoutes(start, end)
                
                android.util.Log.d("BalanceTaCam", "ViewModel: Got result, isSuccess=${result.isSuccess}")
                
                if (result.isSuccess) {
                    val comparison = result.getOrNull()!!
                    android.util.Log.d("BalanceTaCam", "ViewModel: Got ${comparison.routes.size} routes")
                    _uiState.value = RoutingUiState.RoutesCalculated(comparison)
                    android.util.Log.d("BalanceTaCam", "ViewModel: UI state set to RoutesCalculated")
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Échec du calcul d'itinéraire"
                    android.util.Log.e("BalanceTaCam", "ViewModel: Error - $error")
                    _uiState.value = RoutingUiState.Error(error)
                }
            } catch (e: Exception) {
                android.util.Log.e("BalanceTaCam", "ViewModel: Exception", e)
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

