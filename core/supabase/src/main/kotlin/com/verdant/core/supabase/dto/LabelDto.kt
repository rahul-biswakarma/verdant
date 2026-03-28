package com.verdant.core.supabase.dto

import com.verdant.core.model.Label
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LabelDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    val name: String,
    val color: Long,
)

fun LabelDto.toDomain(): Label = Label(
    id = id,
    name = name,
    color = color,
)

fun Label.toDto(userId: String): LabelDto = LabelDto(
    id = id,
    userId = userId,
    name = name,
    color = color,
)
