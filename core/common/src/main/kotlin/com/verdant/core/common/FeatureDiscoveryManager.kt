package com.verdant.core.common

import com.verdant.core.datastore.UserPreferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class VerdantFeature {
    BINARY_TRACKING,        // Always available
    NUMERIC_TRACKING,       // Day 4+
    DURATION_TRACKING,      // Day 4+
    WIDGETS,               // Day 4+
    ANALYTICS,             // Week 2+
    ADAPTIVE_TARGETS,      // Week 2+
    AI_INSIGHTS,           // Day 7+
    VOICE_INPUT,           // Day 7+
    SOCIAL_BUDDIES,        // Day 30+
    LIFE_DASHBOARD,        // Day 14+
    ADVANCED_PREDICTIONS,  // Day 30+
}

@Singleton
class FeatureDiscoveryManager @Inject constructor(
    private val prefs: UserPreferencesDataStore,
) {
    fun isUnlocked(feature: VerdantFeature): Flow<Boolean> =
        prefs.onboardingCompleted.map { completed ->
            if (!completed) return@map feature == VerdantFeature.BINARY_TRACKING
            // For now, all features are unlocked once onboarding is complete.
            // In production, this would check daysActive and habitsLoggedCount.
            true
        }

    suspend fun isUnlockedNow(feature: VerdantFeature): Boolean =
        isUnlocked(feature).first()

    fun getUnlockLevel(feature: VerdantFeature): String = when (feature) {
        VerdantFeature.BINARY_TRACKING -> "Available now"
        VerdantFeature.NUMERIC_TRACKING -> "Unlocks after 3 days"
        VerdantFeature.DURATION_TRACKING -> "Unlocks after 3 days"
        VerdantFeature.WIDGETS -> "Unlocks after 3 days"
        VerdantFeature.AI_INSIGHTS -> "Unlocks after 1 week"
        VerdantFeature.VOICE_INPUT -> "Unlocks after 1 week"
        VerdantFeature.ANALYTICS -> "Unlocks after 2 weeks"
        VerdantFeature.ADAPTIVE_TARGETS -> "Unlocks after 2 weeks"
        VerdantFeature.LIFE_DASHBOARD -> "Unlocks after 2 weeks"
        VerdantFeature.SOCIAL_BUDDIES -> "Unlocks after 1 month"
        VerdantFeature.ADVANCED_PREDICTIONS -> "Unlocks after 1 month"
    }
}
