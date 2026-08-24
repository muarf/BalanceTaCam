package com.osmcamera.mapper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osmcamera.mapper.data.local.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OfflineSettingsViewModel @Inject constructor(
    private val preferences: PreferencesManager
) : ViewModel() {

    val offlineMode = preferences.offlineMode
    val torProxyEnabled = preferences.torProxyEnabled

    fun setOfflineMode(enabled: Boolean) {
        viewModelScope.launch { preferences.setOfflineMode(enabled) }
    }

    fun setTorProxy(enabled: Boolean) {
        viewModelScope.launch { preferences.setTorProxyEnabled(enabled) }
    }
}
