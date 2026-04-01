package com.verdant.core.genui.model

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * Type-erased container returned by [DashboardDataResolver] for each
 * [DataSourceRef]. Components read typed values via the accessor helpers.
 */
data class ResolvedData(
    val values: Map<String, JsonElement> = emptyMap(),
) {
    fun intOrNull(key: String): Int? =
        (values[key] as? JsonPrimitive)?.intOrNull

    fun floatOrNull(key: String): Float? =
        (values[key] as? JsonPrimitive)?.floatOrNull

    fun stringOrNull(key: String): String? =
        (values[key] as? JsonPrimitive)?.contentOrNull

    fun booleanOrNull(key: String): Boolean? =
        (values[key] as? JsonPrimitive)?.contentOrNull?.toBooleanStrictOrNull()

    fun listOrNull(key: String): List<JsonElement>? =
        try { values[key]?.jsonArray?.toList() } catch (_: Exception) { null }

    fun stringListOrNull(key: String): List<String>? =
        listOrNull(key)?.mapNotNull {
            try { it.jsonPrimitive.contentOrNull } catch (_: Exception) { null }
        }

    val isEmpty: Boolean get() = values.isEmpty()
}
