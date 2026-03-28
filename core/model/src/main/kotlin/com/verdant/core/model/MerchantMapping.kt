package com.verdant.core.model

data class MerchantMapping(
    val id: String,
    val merchantPattern: String,
    val category: String,
    val subCategory: String?,
    val useCount: Int = 1,
)
