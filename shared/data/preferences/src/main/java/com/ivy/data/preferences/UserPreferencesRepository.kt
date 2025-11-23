package com.ivy.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val START_DAY_OF_MONTH = intPreferencesKey("start_day_of_month")
        val DATA_BACKUP_COMPLETED = booleanPreferencesKey("data_backup_completed")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val LAST_SELECTED_TAB = stringPreferencesKey("last_selected_tab")
    }

    val startDayOfMonth: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.START_DAY_OF_MONTH] ?: 1
    }

    val dataBackupCompleted: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.DATA_BACKUP_COMPLETED] ?: false
    }

    val onboardingCompleted: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_COMPLETED] ?: false
    }

    val lastSelectedTab: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.LAST_SELECTED_TAB]
    }

    suspend fun setStartDayOfMonth(day: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.START_DAY_OF_MONTH] = day
        }
    }

    suspend fun setDataBackupCompleted(completed: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.DATA_BACKUP_COMPLETED] = completed
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setLastSelectedTab(tab: String) {
        dataStore.edit { prefs ->
            prefs[Keys.LAST_SELECTED_TAB] = tab
        }
    }
}
