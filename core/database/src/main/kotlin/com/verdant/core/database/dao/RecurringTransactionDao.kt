package com.verdant.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.verdant.core.database.entity.RecurringTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recurring: RecurringTransactionEntity)

    @Update
    suspend fun update(recurring: RecurringTransactionEntity)

    @Query("SELECT * FROM recurring_transactions WHERE is_active = 1 ORDER BY next_expected ASC")
    fun observeActive(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE merchant = :merchant AND is_active = 1 LIMIT 1")
    suspend fun getByMerchant(merchant: String): RecurringTransactionEntity?

    @Query("DELETE FROM recurring_transactions")
    suspend fun deleteAll()
}
