package com.verdant.core.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DashboardLayoutDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("layout_json") val layoutJson: String,
    @SerialName("generated_at") val generatedAt: Long,
    @SerialName("expires_at") val expiresAt: Long,
    @SerialName("schema_version") val schemaVersion: Int,
)
