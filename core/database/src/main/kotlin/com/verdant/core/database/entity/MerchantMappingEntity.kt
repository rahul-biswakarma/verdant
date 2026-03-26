package com.verdant.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merchant_mappings")
data class MerchantMappingEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "merchant_pattern") val merchantPattern: String,
    val category: String,
    @ColumnInfo(name = "sub_category") val subCategory: String?,
    @ColumnInfo(name = "use_count") val useCount: Int = 1,
)
