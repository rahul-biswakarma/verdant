package com.verdant.core.model.repository

import com.verdant.core.model.MerchantMapping

interface MerchantMappingRepository {
    suspend fun findByMerchant(merchant: String): MerchantMapping?
    suspend fun upsert(mapping: MerchantMapping)
    suspend fun incrementUseCount(id: String)
    suspend fun getAll(): List<MerchantMapping>
    suspend fun deleteAll()
}
