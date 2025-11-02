package com.ivy.data.db.migration
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
/**
 * Migration from version 130 to 131.
 * Adds a 'reconciliationDate' column to the accounts table.
 */
@Suppress("MagicNumber")
public class Migration131to132_AccountRecon : Migration(131, 132) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE accounts ADD COLUMN reconciliationDate INTEGER NULL")
    }
}