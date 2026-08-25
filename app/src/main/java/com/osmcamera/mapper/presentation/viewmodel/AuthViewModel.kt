package com.osmcamera.mapper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osmcamera.mapper.data.model.User
import com.osmcamera.mapper.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for authentication
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()
    
    init {
        checkAuthentication()
    }
    
    private fun checkAuthentication() {
        viewModelScope.launch {
            if (authRepository.isAuthenticated()) {
                // Token may have expired — refresh silently if needed
                val valid = authRepository.ensureValidToken()
                if (valid) {
                    _uiState.value = AuthUiState.Authenticated
                    loadUserDetails()
                } else {
                    _uiState.value = AuthUiState.NotAuthenticated
                }
            } else {
                _uiState.value = AuthUiState.NotAuthenticated
            }
        }
    }
    
    fun startAuthentication() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val authUrl = authRepository.startAuthentication()
                _uiState.value = AuthUiState.AuthorizationRequired(authUrl)
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Authentication failed")
            }
        }
    }
    
    fun completeAuthentication(code: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val result = authRepository.completeAuthentication(code)
                if (result.isSuccess) {
                    _uiState.value = AuthUiState.Authenticated
                    loadUserDetails()
                } else {
                    _uiState.value = AuthUiState.Error(
                        result.exceptionOrNull()?.message ?: "Authentication failed"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Authentication failed")
            }
        }
    }
    
    fun loadUserDetails() {
        viewModelScope.launch {
            try {
                val user = authRepository.getUserDetails()
                _user.value = user
            } catch (e: Exception) {
                // Silently fail user details loading
            }
        }
    }
    
    fun logout() {
        authRepository.logout()
        _user.value = null
        _uiState.value = AuthUiState.NotAuthenticated
    }
    
    fun isAuthenticated(): Boolean {
        return _uiState.value is AuthUiState.Authenticated
    }
}

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object NotAuthenticated : AuthUiState()
    object Authenticated : AuthUiState()
    data class AuthorizationRequired(val url: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}


