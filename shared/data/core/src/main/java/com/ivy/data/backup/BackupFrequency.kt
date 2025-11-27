package com.ivy.data.backup

import kotlinx.serialization.Serializable

@Serializable
sealed class BackupFrequency {
    @Serializable
    object Daily : BackupFrequency()

    @Serializable
    object Every3Days : BackupFrequency()

    @Serializable
    object Weekly : BackupFrequency()

    @Serializable
    object Every30Days : BackupFrequency()

    @Serializable
    data class MonthlyOnDay(val dayOfMonth: Int) : BackupFrequency()
}
