package com.verdant.feature.settings.datasources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.core.model.repository.HealthRecordRepository
import com.verdant.core.model.repository.DeviceStatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DataCategory(
    val name: String,
    val recordCount: Int,
    val source: String,
)

data class DataAuditUiState(
    val categories: List<DataCategory> = emptyList(),
)

@HiltViewModel
class DataAuditViewModel @Inject constructor(
    private val healthRecordRepository: HealthRecordRepository,
    private val deviceStatRepository: DeviceStatRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataAuditUiState())
    val uiState: StateFlow<DataAuditUiState> = _uiState

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val categories = mutableListOf<DataCategory>()

            val healthCount = healthRecordRepository.count()
            if (healthCount > 0) {
                categories.add(DataCategory("Health Records", healthCount, "Health Connect"))
            }

            val deviceStatCount = deviceStatRepository.count()
            if (deviceStatCount > 0) {
                categories.add(DataCategory("Device Stats", deviceStatCount, "Device"))
            }

            // Always show these categories even if empty
            categories.add(DataCategory("Habits", 0, "Manual"))
            categories.add(DataCategory("Transactions", 0, "SMS"))
            categories.add(DataCategory("AI Insights", 0, "AI"))

            _uiState.value = DataAuditUiState(categories = categories)
        }
    }
}
