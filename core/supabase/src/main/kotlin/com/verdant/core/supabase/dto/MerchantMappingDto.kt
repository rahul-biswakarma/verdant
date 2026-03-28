package com.verdant.core.supabase.dto

import com.verdant.core.model.MerchantMapping
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MerchantMappingDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("merchant_pattern") val merchantPattern: String,
    val category: String,
    @SerialName("sub_category") val subCategory: String? = null,
    @SerialName("use_count") val useCount: Int = 1,
)

fun MerchantMappingDto.toDomain(): MerchantMapping = MerchantMapping(
    id = id,
    merchantPattern = merchantPattern,
    category = category,
    subCategory = subCategory,
    useCount = useCount,
)

fun MerchantMapping.toDto(userId: String): MerchantMappingDto = MerchantMappingDto(
    id = id,
    userId = userId,
    merchantPattern = merchantPattern,
    category = category,
    subCategory = subCategory,
    useCount = useCount,
)
