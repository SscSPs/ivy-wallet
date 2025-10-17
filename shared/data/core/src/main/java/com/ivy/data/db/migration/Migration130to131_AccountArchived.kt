package com.ivy.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 130 to 131.
 * Adds an 'archived' column to the accounts table to support archiving accounts.
 */
@Suppress("MagicNumber")
class Migration130to131_AccountArchived : Migration(130, 131) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE accounts ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")
    }
}