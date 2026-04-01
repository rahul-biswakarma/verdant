package com.verdant.core.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Calls the `generate-dashboard` Supabase Edge Function, which invokes Claude
 * server-side and returns a [DashboardLayout] JSON string.
 */
@Singleton
class DashboardLayoutService @Inject constructor(
    private val supabase: SupabaseClient,
) {
    /**
     * Sends the generation context JSON to the Edge Function and returns
     * the raw layout JSON string from Claude.
     *
     * @param contextJson Serialized [DashboardGenerationContext] payload.
     * @return Raw JSON string of the generated [DashboardLayout].
     */
    suspend fun generateLayout(contextJson: String): String {
        val response = supabase.functions.invoke(
            function = "generate-dashboard",
            body = contextJson,
            headers = Headers.build {
                append(HttpHeaders.ContentType, "application/json")
            },
        )
        return response.body<String>()
    }
}
