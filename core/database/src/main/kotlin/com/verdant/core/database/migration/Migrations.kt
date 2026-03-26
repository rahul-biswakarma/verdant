package com.verdant.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE habit_entries ADD COLUMN missed_reason TEXT")
        db.execSQL("ALTER TABLE habit_entries ADD COLUMN stress_level INTEGER")
        db.execSQL("ALTER TABLE habit_entries ADD COLUMN energy_level INTEGER")
    }
}
