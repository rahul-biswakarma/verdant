package com.verdant.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.verdant.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

data class CategoryTotal(val category: String?, val total: Double)

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY transaction_date DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE transaction_date BETWEEN :start AND :end ORDER BY transaction_date DESC")
    fun observeByDateRange(start: Long, end: Long): Flow<List<TransactionEntity>>

    @Query(
        "SELECT category, SUM(amount) as total FROM transactions " +
        "WHERE transaction_type = 'DEBIT' AND transaction_date BETWEEN :start AND :end " +
        "GROUP BY category ORDER BY total DESC"
    )
    fun spendingByCategory(start: Long, end: Long): Flow<List<CategoryTotal>>

    @Query("SELECT * FROM transactions WHERE raw_sms_id = :smsId LIMIT 1")
    suspend fun getByRawSmsId(smsId: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int

    @Query(
        "SELECT SUM(amount) FROM transactions " +
        "WHERE transaction_type = 'DEBIT' AND transaction_date BETWEEN :start AND :end"
    )
    fun totalSpent(start: Long, end: Long): Flow<Double?>

    @Query(
        "SELECT SUM(amount) FROM transactions " +
        "WHERE transaction_type = 'CREDIT' AND transaction_date BETWEEN :start AND :end"
    )
    fun totalIncome(start: Long, end: Long): Flow<Double?>
}
