package com.verdant.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.verdant.core.database.converter.Converters
import com.verdant.core.database.dao.AIInsightDao
import com.verdant.core.database.dao.HabitDao
import com.verdant.core.database.dao.HabitEntryDao
import com.verdant.core.database.dao.LabelDao
import com.verdant.core.database.dao.MerchantMappingDao
import com.verdant.core.database.dao.TransactionDao
import com.verdant.core.database.entity.AIInsightEntity
import com.verdant.core.database.entity.HabitEntity
import com.verdant.core.database.entity.HabitEntryEntity
import com.verdant.core.database.entity.LabelEntity
import com.verdant.core.database.entity.MerchantMappingEntity
import com.verdant.core.database.entity.TransactionEntity

@Database(
    entities = [
        HabitEntity::class,
        HabitEntryEntity::class,
        LabelEntity::class,
        AIInsightEntity::class,
        TransactionEntity::class,
        MerchantMappingEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class VerdantDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitEntryDao(): HabitEntryDao
    abstract fun labelDao(): LabelDao
    abstract fun aiInsightDao(): AIInsightDao
    abstract fun transactionDao(): TransactionDao
    abstract fun merchantMappingDao(): MerchantMappingDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE habits ADD COLUMN visualization_type TEXT NOT NULL DEFAULT 'CONTRIBUTION_GRID'"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "UPDATE habits SET tracking_type = 'NUMERIC' WHERE tracking_type IN ('QUANTITATIVE','DURATION','FINANCIAL')"
                )
                db.execSQL(
                    "UPDATE habits SET visualization_type = 'PROGRESS_FILL' WHERE visualization_type = 'PHYSICS_JAR'"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS transactions (
                        id TEXT NOT NULL PRIMARY KEY,
                        amount REAL NOT NULL,
                        transaction_type TEXT NOT NULL,
                        merchant TEXT,
                        category TEXT,
                        sub_category TEXT,
                        account_tail TEXT,
                        bank TEXT,
                        upi_id TEXT,
                        balance_after REAL,
                        transaction_date INTEGER NOT NULL,
                        raw_sms_id INTEGER,
                        raw_sms_body TEXT,
                        is_recurring INTEGER NOT NULL DEFAULT 0,
                        parse_confidence REAL NOT NULL DEFAULT 1.0,
                        user_verified INTEGER NOT NULL DEFAULT 0,
                        created_at INTEGER NOT NULL
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_transaction_date ON transactions(transaction_date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_category ON transactions(category)")

                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS merchant_mappings (
                        id TEXT NOT NULL PRIMARY KEY,
                        merchant_pattern TEXT NOT NULL,
                        category TEXT NOT NULL,
                        sub_category TEXT,
                        use_count INTEGER NOT NULL DEFAULT 1
                    )"""
                )
            }
        }
    }
}
