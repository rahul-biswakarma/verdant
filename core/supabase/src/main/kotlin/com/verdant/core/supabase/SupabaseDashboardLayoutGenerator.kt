package com.verdant.core.supabase

import com.verdant.core.genui.generation.DashboardGenerationContext
import com.verdant.core.genui.generation.DashboardLayoutGenerator
import com.verdant.core.genui.model.DashboardLayout
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implements [DashboardLayoutGenerator] by calling the `generate-dashboard`
 * Supabase Edge Function, which invokes Claude server-side.
 */
@Singleton
class SupabaseDashboardLayoutGenerator @Inject constructor(
    private val dashboardLayoutService: DashboardLayoutService,
    private val json: Json,
) : DashboardLayoutGenerator {

    override suspend fun generate(context: DashboardGenerationContext): DashboardLayout {
        val contextJson = json.encodeToString(
            DashboardGenerationContext.serializer(),
            context,
        )
        val responseJson = dashboardLayoutService.generateLayout(contextJson)
        return json.decodeFromString(DashboardLayout.serializer(), responseJson)
    }
}
