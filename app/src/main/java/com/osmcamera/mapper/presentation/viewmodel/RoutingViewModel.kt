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
 * ViewModel for routing with camera avoidance.
 * Route points live in MapViewModel so they survive navigation to/from the map.
 */
@HiltViewModel
class RoutingViewModel @Inject constructor(
    private val routingRepository: RoutingRepository,
    private val nominatimApi: NominatimApi
) : ViewModel() {

    private val _uiState = MutableStateFlow<RoutingUiState>(RoutingUiState.Idle)
    val uiState: StateFlow<RoutingUiState> = _uiState.asStateFlow()

    private val _avoidanceRadius = MutableStateFlow(40.0)
    val avoidanceRadius: StateFlow<Double> = _avoidanceRadius.asStateFlow()

    fun setAvoidanceRadius(radius: Double) {
        _avoidanceRadius.value = radius
    }

    private var searchJob: Job? = null

    /**
     * Search address using Nominatim, result delivered via callback
     */
    fun searchAddress(query: String, onResult: (GeoPoint?) -> Unit) {
        if (query.length < 3) {
            onResult(null)
            return
        }

        // Cancel previous search
        searchJob?.cancel()

        searchJob = viewModelScope.launch {
            delay(500) // Debounce

            try {
                val response = nominatimApi.search(query)
                val first = if (response.isSuccessful) response.body()?.firstOrNull() else null
                onResult(first?.let { GeoPoint(it.lat.toDouble(), it.lon.toDouble()) })
            } catch (e: Exception) {
                android.util.Log.e("BalanceTaCam", "Address search failed", e)
                onResult(null)
            }
        }
    }

    fun clearResults() {
        _uiState.value = RoutingUiState.Idle
    }

    fun calculateRoute(start: GeoPoint?, end: GeoPoint?) {
        if (start == null || end == null) {
            _uiState.value = RoutingUiState.Error("Points de départ et d'arrivée requis")
            return
        }

        viewModelScope.launch {
            android.util.Log.d("BalanceTaCam", "ViewModel: Setting Calculating state (radius: ${_avoidanceRadius.value}m)")
            _uiState.value = RoutingUiState.Calculating

            try {
                android.util.Log.d("BalanceTaCam", "ViewModel: Calling repository")
                val result = routingRepository.calculateAntiCameraRoutes(
                    start = start,
                    end = end,
                    avoidanceRadius = _avoidanceRadius.value
                )

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
