package com.verdant.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.verdant.core.database.converter.Converters
import com.verdant.core.database.dao.AchievementDao
import com.verdant.core.database.dao.ActivityRecordDao
import com.verdant.core.database.dao.AIInsightDao
import com.verdant.core.database.dao.BudgetDao
import com.verdant.core.database.dao.CrossCorrelationDao
import com.verdant.core.database.dao.DeviceSignalDao
import com.verdant.core.database.dao.DeviceStatDao
import com.verdant.core.database.dao.EmotionalContextDao
import com.verdant.core.database.dao.HabitDao
import com.verdant.core.database.dao.HabitEntryDao
import com.verdant.core.database.dao.HealthRecordDao
import com.verdant.core.database.dao.LabelDao
import com.verdant.core.database.dao.LifeScoreDao
import com.verdant.core.database.dao.PendingAIRequestDao
import com.verdant.core.database.dao.PlayerProfileDao
import com.verdant.core.database.dao.PredictionDao
import com.verdant.core.database.dao.QuestDao
import com.verdant.core.database.dao.RecurringTransactionDao
import com.verdant.core.database.dao.StreakCacheDao
import com.verdant.core.database.dao.WeatherDao
import com.verdant.core.database.entity.AchievementEntity
import com.verdant.core.database.entity.ActivityRecordEntity
import com.verdant.core.database.entity.AIInsightEntity
import com.verdant.core.database.entity.BudgetEntity
import com.verdant.core.database.entity.CrossCorrelationEntity
import com.verdant.core.database.entity.DeviceSignalEntity
import com.verdant.core.database.entity.DeviceStatEntity
import com.verdant.core.database.entity.EmotionalContextEntity
import com.verdant.core.database.entity.HabitEntity
import com.verdant.core.database.entity.HabitEntryEntity
import com.verdant.core.database.entity.HabitPlaceEntity
import com.verdant.core.database.entity.HabitRiskSnapshotEntity
import com.verdant.core.database.entity.HabitTargetHistoryEntity
import com.verdant.core.database.entity.HealthRecordEntity
import com.verdant.core.database.entity.LabelEntity
import com.verdant.core.database.entity.LifeScoreEntity
import com.verdant.core.database.entity.MerchantMappingEntity
import com.verdant.core.database.entity.PendingAIRequestEntity
import com.verdant.core.database.entity.PlayerProfileEntity
import com.verdant.core.database.entity.PredictionEntity
import com.verdant.core.database.entity.QuestEntity
import com.verdant.core.database.entity.RecurringTransactionEntity
import com.verdant.core.database.entity.StreakCacheEntity
import com.verdant.core.database.entity.TransactionEntity
import com.verdant.core.database.entity.WeatherSnapshotEntity

@Database(
    entities = [
        HabitEntity::class,
        HabitEntryEntity::class,
        LabelEntity::class,
        AIInsightEntity::class,
        TransactionEntity::class,
        MerchantMappingEntity::class,
        HealthRecordEntity::class,
        ActivityRecordEntity::class,
        DeviceStatEntity::class,
        WeatherSnapshotEntity::class,
        LifeScoreEntity::class,
        PredictionEntity::class,
        BudgetEntity::class,
        RecurringTransactionEntity::class,
        HabitRiskSnapshotEntity::class,
        HabitPlaceEntity::class,
        HabitTargetHistoryEntity::class,
        PendingAIRequestEntity::class,
        StreakCacheEntity::class,
        EmotionalContextEntity::class,
        PlayerProfileEntity::class,
        QuestEntity::class,
        AchievementEntity::class,
        DeviceSignalEntity::class,
        CrossCorrelationEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class VerdantDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitEntryDao(): HabitEntryDao
    abstract fun labelDao(): LabelDao
    abstract fun aiInsightDao(): AIInsightDao
    abstract fun healthRecordDao(): HealthRecordDao
    abstract fun activityRecordDao(): ActivityRecordDao
    abstract fun deviceStatDao(): DeviceStatDao
    abstract fun weatherDao(): WeatherDao
    abstract fun lifeScoreDao(): LifeScoreDao
    abstract fun predictionDao(): PredictionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun emotionalContextDao(): EmotionalContextDao
    abstract fun playerProfileDao(): PlayerProfileDao
    abstract fun questDao(): QuestDao
    abstract fun achievementDao(): AchievementDao
    abstract fun deviceSignalDao(): DeviceSignalDao
    abstract fun crossCorrelationDao(): CrossCorrelationDao
    abstract fun streakCacheDao(): StreakCacheDao
    abstract fun pendingAIRequestDao(): PendingAIRequestDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habit_entries ADD COLUMN missed_reason TEXT")
                db.execSQL("ALTER TABLE habit_entries ADD COLUMN stress_level INTEGER")
                db.execSQL("ALTER TABLE habit_entries ADD COLUMN energy_level INTEGER")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Column additions to existing tables
                db.execSQL("ALTER TABLE habit_entries ADD COLUMN auto_logged INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE habit_entries ADD COLUMN auto_track_source TEXT")
                db.execSQL("ALTER TABLE habits ADD COLUMN auto_track_source TEXT")
                db.execSQL("ALTER TABLE habits ADD COLUMN geofence_enabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE habits ADD COLUMN outdoor_activity INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN recurring_group_id TEXT")

                // New tables
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS health_records (
                        id TEXT NOT NULL PRIMARY KEY,
                        record_type TEXT NOT NULL,
                        value REAL NOT NULL,
                        secondary_value REAL,
                        unit TEXT NOT NULL,
                        recorded_at INTEGER NOT NULL,
                        source TEXT NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS activity_records (
                        id TEXT NOT NULL PRIMARY KEY,
                        activity_type TEXT NOT NULL,
                        confidence INTEGER NOT NULL,
                        duration_minutes INTEGER NOT NULL,
                        recorded_at INTEGER NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS device_stats (
                        id TEXT NOT NULL PRIMARY KEY,
                        stat_type TEXT NOT NULL,
                        value REAL NOT NULL,
                        detail TEXT,
                        recorded_date INTEGER NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS weather_snapshots (
                        id TEXT NOT NULL PRIMARY KEY,
                        date INTEGER NOT NULL,
                        temperature REAL NOT NULL,
                        condition TEXT NOT NULL,
                        humidity INTEGER NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS life_scores (
                        id TEXT NOT NULL PRIMARY KEY,
                        score_type TEXT NOT NULL,
                        score INTEGER NOT NULL,
                        components TEXT NOT NULL,
                        computed_date INTEGER NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS predictions (
                        id TEXT NOT NULL PRIMARY KEY,
                        prediction_type TEXT NOT NULL,
                        target_period TEXT NOT NULL,
                        prediction_data TEXT NOT NULL,
                        confidence REAL NOT NULL,
                        generated_at INTEGER NOT NULL,
                        expires_at INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS budgets (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        category TEXT NOT NULL,
                        amount REAL NOT NULL,
                        period TEXT NOT NULL,
                        is_active INTEGER NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS recurring_transactions (
                        id TEXT NOT NULL PRIMARY KEY,
                        merchant TEXT NOT NULL,
                        category TEXT NOT NULL,
                        typical_amount REAL NOT NULL,
                        frequency_days INTEGER NOT NULL,
                        last_seen INTEGER NOT NULL,
                        next_expected INTEGER NOT NULL,
                        confidence REAL NOT NULL,
                        is_active INTEGER NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS habit_risk_snapshots (
                        id TEXT NOT NULL PRIMARY KEY,
                        habit_id TEXT NOT NULL,
                        score REAL NOT NULL,
                        computed_at INTEGER NOT NULL,
                        triggering_factors TEXT NOT NULL,
                        FOREIGN KEY (habit_id) REFERENCES habits(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_habit_risk_snapshots_habit_id ON habit_risk_snapshots(habit_id)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS habit_places (
                        id TEXT NOT NULL PRIMARY KEY,
                        habit_id TEXT NOT NULL,
                        name TEXT NOT NULL,
                        lat REAL NOT NULL,
                        lon REAL NOT NULL,
                        radius_meters REAL NOT NULL,
                        trigger_on TEXT NOT NULL,
                        FOREIGN KEY (habit_id) REFERENCES habits(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_habit_places_habit_id ON habit_places(habit_id)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS habit_target_history (
                        id TEXT NOT NULL PRIMARY KEY,
                        habit_id TEXT NOT NULL,
                        old_target REAL NOT NULL,
                        new_target REAL NOT NULL,
                        changed_at INTEGER NOT NULL,
                        reason TEXT NOT NULL,
                        FOREIGN KEY (habit_id) REFERENCES habits(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_habit_target_history_habit_id ON habit_target_history(habit_id)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS pending_ai_requests (
                        id TEXT NOT NULL PRIMARY KEY,
                        request_type TEXT NOT NULL,
                        payload TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        attempt_count INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS streak_cache (
                        habit_id TEXT NOT NULL PRIMARY KEY,
                        current_streak INTEGER NOT NULL,
                        longest_streak INTEGER NOT NULL,
                        completion_rate REAL NOT NULL,
                        cached_at INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS emotional_context (
                        id TEXT NOT NULL PRIMARY KEY,
                        date INTEGER NOT NULL,
                        inferred_mood TEXT NOT NULL,
                        energy_level INTEGER NOT NULL,
                        confidence REAL NOT NULL,
                        contributing_signals TEXT NOT NULL,
                        user_confirmed INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS player_profile (
                        id TEXT NOT NULL PRIMARY KEY,
                        level INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        total_xp INTEGER NOT NULL,
                        current_level_xp INTEGER NOT NULL,
                        xp_to_next_level INTEGER NOT NULL,
                        rank TEXT NOT NULL,
                        stats TEXT NOT NULL,
                        evolution_path TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS quests (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        difficulty TEXT NOT NULL,
                        xp_reward INTEGER NOT NULL,
                        conditions TEXT NOT NULL,
                        time_limit INTEGER,
                        generated_by TEXT NOT NULL,
                        reasoning TEXT NOT NULL,
                        status TEXT NOT NULL,
                        started_at INTEGER,
                        completed_at INTEGER
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS achievements (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        xp_reward INTEGER NOT NULL,
                        unlocked_at INTEGER NOT NULL,
                        category TEXT NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS device_signals (
                        id TEXT NOT NULL PRIMARY KEY,
                        device_id TEXT NOT NULL,
                        signal_type TEXT NOT NULL,
                        value REAL NOT NULL,
                        unit TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS cross_correlations (
                        id TEXT NOT NULL PRIMARY KEY,
                        dimension_a TEXT NOT NULL,
                        dimension_b TEXT NOT NULL,
                        correlation_strength REAL NOT NULL,
                        description TEXT NOT NULL,
                        discovered_at INTEGER NOT NULL,
                        sample_size INTEGER NOT NULL
                    )
                """)
            }
        }
    }
}
