package com.ivy.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 133 to 134.
 * Adds 'toAccountId' and 'toAmount' columns to planned_payment_rules table for transfer support.
 * This enables planned payments to support TRANSFER transaction types in addition to INCOME and EXPENSE.
 */
@Suppress("MagicNumber")
val Migration133to134_PlannedPaymentTransfer = object : Migration(133, 134) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add destination account column for transfers
        database.execSQL(
            "ALTER TABLE planned_payment_rules ADD COLUMN toAccountId TEXT NULL"
        )
        
        // Add destination amount column for transfers (useful for currency conversion)
        database.execSQL(
            "ALTER TABLE planned_payment_rules ADD COLUMN toAmount REAL NULL"
        )
    }
}
