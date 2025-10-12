package com.osmcamera.mapper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osmcamera.mapper.data.model.CameraFormData
import com.osmcamera.mapper.data.repository.OSMRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for adding cameras
 */
@HiltViewModel
class AddCameraViewModel @Inject constructor(
    private val osmRepository: OSMRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<AddCameraUiState>(AddCameraUiState.Editing)
    val uiState: StateFlow<AddCameraUiState> = _uiState.asStateFlow()
    
    private val _cameraData = MutableStateFlow(CameraFormData())
    val cameraData: StateFlow<CameraFormData> = _cameraData.asStateFlow()
    
    private val _isDetailedMode = MutableStateFlow(false)
    val isDetailedMode: StateFlow<Boolean> = _isDetailedMode.asStateFlow()
    
    fun setPosition(latitude: Double, longitude: Double) {
        _cameraData.value = _cameraData.value.copy(
            latitude = latitude,
            longitude = longitude
        )
    }
    
    fun setDetailedMode(detailed: Boolean) {
        _isDetailedMode.value = detailed
    }
    
    fun updateCameraType(type: String?) {
        _cameraData.value = _cameraData.value.copy(cameraType = type)
    }
    
    fun updateCameraMount(mount: String?) {
        _cameraData.value = _cameraData.value.copy(cameraMount = mount)
    }
    
    fun updateCameraDirection(direction: String?) {
        _cameraData.value = _cameraData.value.copy(cameraDirection = direction)
    }
    
    fun updateSurveillance(surveillance: String?) {
        _cameraData.value = _cameraData.value.copy(surveillance = surveillance)
    }
    
    fun updateOperator(operator: String?) {
        _cameraData.value = _cameraData.value.copy(operator = operator)
    }
    
    fun updateOperatorType(operatorType: String?) {
        _cameraData.value = _cameraData.value.copy(operatorType = operatorType)
    }
    
    fun updateSurveillanceZone(zone: String?) {
        _cameraData.value = _cameraData.value.copy(surveillanceZone = zone)
    }
    
    fun updateDescription(description: String?) {
        _cameraData.value = _cameraData.value.copy(description = description)
    }
    
    fun updateLevel(level: String?) {
        _cameraData.value = _cameraData.value.copy(level = level)
    }
    
    fun updateHeight(height: String?) {
        _cameraData.value = _cameraData.value.copy(height = height)
    }
    
    fun publishCamera() {
        viewModelScope.launch {
            _uiState.value = AddCameraUiState.Publishing
            
            // Validate
            val validation = _cameraData.value.validate()
            if (!validation.isValid) {
                _uiState.value = AddCameraUiState.Error(validation.error ?: "Validation failed")
                return@launch
            }
            
            try {
                val result = osmRepository.createCamera(_cameraData.value)
                if (result.isSuccess) {
                    _uiState.value = AddCameraUiState.Success(result.getOrNull() ?: "")
                } else {
                    _uiState.value = AddCameraUiState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to create camera"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = AddCameraUiState.Error(e.message ?: "Failed to create camera")
            }
        }
    }
    
    fun resetState() {
        _uiState.value = AddCameraUiState.Editing
    }
    
    fun reset() {
        _cameraData.value = CameraFormData()
        _isDetailedMode.value = false
        _uiState.value = AddCameraUiState.Editing
    }
}

sealed class AddCameraUiState {
    object Editing : AddCameraUiState()
    object Publishing : AddCameraUiState()
    data class Success(val nodeId: String) : AddCameraUiState()
    data class Error(val message: String) : AddCameraUiState()
}


