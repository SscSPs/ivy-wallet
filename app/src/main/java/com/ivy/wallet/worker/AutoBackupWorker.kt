package com.ivy.wallet.worker

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ivy.data.backup.BackupDataUseCase
import com.ivy.data.backup.BackupFrequency
import com.ivy.data.backup.BackupRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val backupDataUseCase: BackupDataUseCase,
    private val backupRepository: BackupRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val isAutoBackupEnabled = backupRepository.isAutoBackupEnabled.first()

            if (!isAutoBackupEnabled) {
                Timber.d("Auto backup is disabled, skipping.")
                return Result.success()
            }

            val backupUriString = backupRepository.autoBackupUri.first()

            if (backupUriString.isNullOrBlank()) {
                Timber.e("Auto backup URI is missing.")
                return Result.failure()
            }

            // Check Frequency
            val frequency = backupRepository.backupFrequency.first()
            val lastBackupTimestamp = backupRepository.lastBackupTimestamp.first()
            val lastBackupDateTime = if (lastBackupTimestamp > 0) {
                LocalDateTime.ofInstant(Instant.ofEpochMilli(lastBackupTimestamp), ZoneId.systemDefault())
            } else {
                LocalDateTime.MIN
            }
            val now = LocalDateTime.now()

            val shouldRun = when (frequency) {
                BackupFrequency.Daily -> {
                    ChronoUnit.DAYS.between(lastBackupDateTime, now) >= 1
                }

                BackupFrequency.Every3Days -> {
                    ChronoUnit.DAYS.between(lastBackupDateTime, now) >= 3
                }

                BackupFrequency.Weekly -> {
                    ChronoUnit.DAYS.between(lastBackupDateTime, now) >= 7
                }

                BackupFrequency.Every30Days -> {
                    ChronoUnit.DAYS.between(lastBackupDateTime, now) >= 30
                }

                is BackupFrequency.MonthlyOnDay -> {
                    val isCorrectDay = now.dayOfMonth == frequency.dayOfMonth
                    val notRunToday = ChronoUnit.DAYS.between(lastBackupDateTime, now) >= 1
                    isCorrectDay && notRunToday
                }
            }

            if (!shouldRun) {
                Timber.d("Backup skipped due to frequency settings. Frequency: $frequency, Last run: $lastBackupDateTime")
                return Result.success()
            }

            val backupUri = Uri.parse(backupUriString)
            val backupFolder = DocumentFile.fromTreeUri(applicationContext, backupUri)

            if (backupFolder == null || !backupFolder.canWrite()) {
                Timber.e("Cannot write to backup folder: $backupUriString")
                return Result.failure()
            }

            // 1. Generate Backup
            val jsonBackup = backupDataUseCase.generateJsonBackup()
            val timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val fileName = "ivy_backup_$timestamp.json"

            val newFile = backupFolder.createFile("application/json", fileName)
            if (newFile == null) {
                Timber.e("Failed to create backup file.")
                return Result.failure()
            }

            applicationContext.contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                outputStream.write(jsonBackup.toByteArray(Charsets.UTF_8))
            } ?: return Result.failure()

            Timber.d("Backup created successfully: $fileName")

            // Update last backup timestamp
            backupRepository.setLastBackupTimestamp(System.currentTimeMillis())

            // 2. Delete old backups (older than 30 days)
            val thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS)
            val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

            backupFolder.listFiles().forEach { file ->
                val name = file.name
                if (name != null && name.startsWith("ivy_backup_") && name.endsWith(".json")) {
                    try {
                        val datePart = name.removePrefix("ivy_backup_").removeSuffix(".json")
                        val fileDate = LocalDateTime.parse(datePart, formatter)

                        if (fileDate.isBefore(thirtyDaysAgo)) {
                            file.delete()
                            Timber.d("Deleted old backup: $name")
                        }
                    } catch (e: Exception) {
                        Timber.w("Failed to parse backup file date: $name")
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Error during daily backup")
            Result.retry()
        }
    }
}
