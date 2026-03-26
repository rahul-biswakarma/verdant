package com.verdant.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.verdant.core.database.entity.MerchantMappingEntity

@Dao
interface MerchantMappingDao {

    @Query("SELECT * FROM merchant_mappings WHERE :merchant LIKE '%' || merchant_pattern || '%' ORDER BY use_count DESC LIMIT 1")
    suspend fun findByMerchant(merchant: String): MerchantMappingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(mapping: MerchantMappingEntity)

    @Query("UPDATE merchant_mappings SET use_count = use_count + 1 WHERE id = :id")
    suspend fun incrementUseCount(id: String)

    @Query("SELECT * FROM merchant_mappings ORDER BY use_count DESC")
    suspend fun getAll(): List<MerchantMappingEntity>

    @Query("DELETE FROM merchant_mappings")
    suspend fun deleteAll()
}
