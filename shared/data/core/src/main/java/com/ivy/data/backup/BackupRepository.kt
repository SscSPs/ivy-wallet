package com.ivy.data.backup

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.ivy.data.datastore.DatastoreKeys
import com.ivy.data.datastore.dataStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

interface BackupRepository {
    val isAutoBackupEnabled: Flow<Boolean>
    val autoBackupUri: Flow<String?>
    val backupFrequency: Flow<BackupFrequency>
    val lastBackupTimestamp: Flow<Long>

    suspend fun setAutoBackupEnabled(enabled: Boolean)
    suspend fun setAutoBackupUri(uri: String)
    suspend fun setBackupFrequency(frequency: BackupFrequency)
    suspend fun setLastBackupTimestamp(timestamp: Long)
}

@Singleton
class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BackupRepository {

    override val isAutoBackupEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DatastoreKeys.AUTO_BACKUP_ENABLED] ?: false
    }

    override val autoBackupUri: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[DatastoreKeys.AUTO_BACKUP_URI]
    }

    override val backupFrequency: Flow<BackupFrequency> = context.dataStore.data.map { preferences ->
        val jsonString = preferences[DatastoreKeys.AUTO_BACKUP_FREQUENCY]
        if (jsonString != null) {
            try {
                Json.decodeFromString(jsonString)
            } catch (e: Exception) {
                BackupFrequency.Daily
            }
        } else {
            BackupFrequency.Daily
        }
    }

    override val lastBackupTimestamp: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[DatastoreKeys.LAST_BACKUP_TIMESTAMP] ?: 0L
    }

    override suspend fun setAutoBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DatastoreKeys.AUTO_BACKUP_ENABLED] = enabled
        }
    }

    override suspend fun setAutoBackupUri(uri: String) {
        context.dataStore.edit { preferences ->
            preferences[DatastoreKeys.AUTO_BACKUP_URI] = uri
        }
    }

    override suspend fun setBackupFrequency(frequency: BackupFrequency) {
        context.dataStore.edit { preferences ->
            preferences[DatastoreKeys.AUTO_BACKUP_FREQUENCY] = Json.encodeToString(frequency)
        }
    }

    override suspend fun setLastBackupTimestamp(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[DatastoreKeys.LAST_BACKUP_TIMESTAMP] = timestamp
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface BackupRepositoryModule {
    @Binds
    fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository
}
