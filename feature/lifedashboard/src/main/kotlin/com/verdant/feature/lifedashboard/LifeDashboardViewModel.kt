package com.verdant.feature.lifedashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.core.database.repository.EmotionalContextRepository
import com.verdant.core.database.repository.LifeScoreRepository
import com.verdant.core.database.repository.PlayerProfileRepository
import com.verdant.core.database.repository.QuestRepository
import com.verdant.core.model.EvolutionPath
import com.verdant.core.model.PlayerProfile
import com.verdant.core.model.PlayerRank
import com.verdant.core.model.PlayerStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LifeDashboardViewModel @Inject constructor(
    private val playerProfileRepository: PlayerProfileRepository,
    private val questRepository: QuestRepository,
    private val emotionalContextRepository: EmotionalContextRepository,
    private val lifeScoreRepository: LifeScoreRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LifeDashboardUiState())
    val uiState: StateFlow<LifeDashboardUiState> = _uiState

    init {
        loadProfile()
        loadQuests()
        loadEmotionalContext()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            playerProfileRepository.observe().collectLatest { profile ->
                val playerProfile = profile ?: createDefaultProfile()
                _uiState.value = _uiState.value.copy(
                    playerProfile = playerProfile,
                    isLoading = false,
                )
            }
        }
    }

    private fun loadQuests() {
        viewModelScope.launch {
            questRepository.observeActive().collectLatest { quests ->
                _uiState.value = _uiState.value.copy(activeQuests = quests)
            }
        }
    }

    private fun loadEmotionalContext() {
        viewModelScope.launch {
            emotionalContextRepository.observeLatest().collectLatest { context ->
                if (context != null) {
                    _uiState.value = _uiState.value.copy(
                        currentMood = context.inferredMood,
                        energyLevel = context.energyLevel,
                    )
                }
            }
        }
    }

    private suspend fun createDefaultProfile(): PlayerProfile {
        val profile = PlayerProfile(
            id = "default",
            level = 1,
            title = PlayerRank.E.displayName(),
            totalXP = 0,
            currentLevelXP = 0,
            xpToNextLevel = 100,
            rank = PlayerRank.E,
            stats = PlayerStats(),
            evolutionPath = EvolutionPath.BALANCE,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        playerProfileRepository.insert(profile)
        return profile
    }
}
