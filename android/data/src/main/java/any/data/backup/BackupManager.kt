package any.data.backup

import any.data.entity.AppDataType
import java.io.File
import java.io.IOException

interface BackupManager {
    /**
     * Open a backup zip.
     *
     * @throws IOException
     */
    suspend fun open(file: File): List<BackupItem>

    /**
     * Get importable counts
     *
     * @throws IOException
     */
    suspend fun importableCounts(items: List<BackupItem>): List<Int>

    /**
     * Import backup items
     *
     * @throws Exception
     */
    suspend fun import(items: List<BackupItem>): List<Result<Int>>

    /**
     * Get exportable counts
     */
    suspend fun exportableCounts(types: List<AppDataType>): List<Int>

    /**
     * Export selected app data types.
     *
     * @param types app data types to export.
     * @param output output file (zip).
     * @throws IOException
     */
    suspend fun export(types: List<AppDataType>, output: File): List<Result<Int>>
}