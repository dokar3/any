package any.data.backup

import any.data.entity.AppDataType
import java.io.File

interface BackupManager {
    suspend fun open(file: File): List<BackupItem>

    suspend fun importableCounts(items: List<BackupItem>): List<Int>

    suspend fun import(items: List<BackupItem>): List<Result<Int>>

    suspend fun exportableCounts(types: List<AppDataType>): List<Int>

    suspend fun export(types: List<AppDataType>, output: File): List<Result<Int>>
}