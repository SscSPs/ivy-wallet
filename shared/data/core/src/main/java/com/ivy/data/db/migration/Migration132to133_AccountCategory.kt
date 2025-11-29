package com.ivy.data.db.migration
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 132 to 133.
 * Adds a 'accountCategory' column to the accounts table.
 * Defaults existing accounts to ASSET category.
 */
@Suppress("MagicNumber")
public class Migration132to133_AccountCategory : Migration(132, 133) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add the accountCategory column with default value 'ASSET'
        db.execSQL("ALTER TABLE accounts ADD COLUMN accountCategory TEXT NOT NULL DEFAULT 'ASSET'")
    }
}
