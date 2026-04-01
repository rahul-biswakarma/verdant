package com.verdant.core.genui.generation

import com.verdant.core.genui.model.ComponentType
import com.verdant.core.genui.model.DashboardLayout
import com.verdant.core.model.repository.DashboardLayoutRepository
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates dashboard layout generation:
 *  1. Builds a [DashboardGenerationContext] from current user data.
 *  2. Calls the LLM (via [DashboardLayoutGenerator]) to produce a layout.
 *  3. Validates and sanitises the result.
 *  4. Persists to Supabase.
 *
 * Fallback chain: LLM → cached layout → static fallback.
 */
@Singleton
class DashboardGenerator @Inject constructor(
    private val layoutGenerator: DashboardLayoutGenerator,
    private val layoutRepository: DashboardLayoutRepository,
    private val contextBuilder: DashboardContextBuilder,
    private val fallbackLayoutProvider: FallbackLayoutProvider,
    private val json: Json,
) {

    companion object {
        const val SCHEMA_VERSION = 1
        private const val LAYOUT_TTL_MS = 24 * 60 * 60 * 1_000L // 24 hours
    }

    suspend fun generate(): DashboardLayout {
        val now = System.currentTimeMillis()

        // Clean up expired layouts
        layoutRepository.deleteExpired(now)

        return try {
            val context = contextBuilder.build()
            val layout = layoutGenerator.generate(context)
            val validated = validate(layout)
            val layoutJson = json.encodeToString(DashboardLayout.serializer(), validated)
            layoutRepository.save(
                layoutJson = layoutJson,
                generatedAt = now,
                expiresAt = now + LAYOUT_TTL_MS,
                schemaVersion = SCHEMA_VERSION,
            )
            validated
        } catch (e: Exception) {
            // Try cached layout
            getCachedLayout() ?: fallbackLayoutProvider.defaultLayout()
        }
    }

    suspend fun getCachedLayout(): DashboardLayout? {
        val cachedJson = layoutRepository.getLatestJson() ?: return null
        return try {
            json.decodeFromString(DashboardLayout.serializer(), cachedJson)
        } catch (_: Exception) {
            null
        }
    }

    private fun validate(layout: DashboardLayout): DashboardLayout {
        val knownTypes = ComponentType.entries.toSet()
        val validSections = layout.sections.filter { section ->
            section.component in knownTypes
        }
        return layout.copy(sections = validSections)
    }
}

/**
 * Abstraction for the actual LLM call. Implemented by the Supabase Edge
 * Function caller in :core:supabase or directly via CloudAI.
 */
interface DashboardLayoutGenerator {
    suspend fun generate(context: DashboardGenerationContext): DashboardLayout
}
