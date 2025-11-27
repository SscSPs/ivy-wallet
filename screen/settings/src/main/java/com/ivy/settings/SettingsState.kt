package com.ivy.settings

import com.ivy.base.legacy.Theme
import com.ivy.data.backup.BackupFrequency

data class SettingsState(
    val currencyCode: String,
    val name: String,
    val currentTheme: Theme,
    val lockApp: Boolean,
    val showNotifications: Boolean,
    val hideCurrentBalance: Boolean,
    val hideIncome: Boolean,
    val treatTransfersAsIncomeExpense: Boolean,
    val startDateOfMonth: String,
    val progressState: Boolean,
    val languageOptionVisible: Boolean = false,
    val autoBackupEnabled: Boolean = false,
    val autoBackupUri: String? = null,
    val backupFrequency: BackupFrequency = BackupFrequency.Daily
)
