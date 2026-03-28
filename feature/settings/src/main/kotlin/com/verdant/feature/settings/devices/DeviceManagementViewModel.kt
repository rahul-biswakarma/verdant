package com.verdant.feature.settings.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.core.sync.DeviceSyncManager
import com.verdant.core.sync.RegisteredDevice
import com.verdant.core.sync.SignalPublisher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceManagementUiState(
    val devices: List<RegisteredDevice> = emptyList(),
    val thisDeviceId: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class DeviceManagementViewModel @Inject constructor(
    private val syncManager: DeviceSyncManager,
    private val signalPublisher: SignalPublisher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceManagementUiState())
    val uiState: StateFlow<DeviceManagementUiState> = _uiState

    init {
        loadDevices()
    }

    fun loadDevices() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val thisId = signalPublisher.getDeviceId()
                val devices = syncManager.getRegisteredDevices()
                _uiState.update {
                    it.copy(
                        devices = devices,
                        thisDeviceId = thisId,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load devices")
                }
            }
        }
    }

    fun removeDevice(deviceId: String) {
        viewModelScope.launch {
            try {
                syncManager.unregisterDevice(deviceId)
                _uiState.update { state ->
                    state.copy(devices = state.devices.filter { it.id != deviceId })
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to remove device") }
            }
        }
    }
}
