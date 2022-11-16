package any.data.backup

import android.content.Context
import any.base.util.Dirs
import any.base.util.ZipUtil
import any.data.Json
import any.data.db.AppDatabase
import any.data.db.PostContentDatabase
import any.data.entity.AppDataType
import any.data.entity.Bookmark
import any.data.entity.Post
import any.data.entity.PostContent
import any.data.entity.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class BackupManagerImpl(
    context: Context,
    private val json: Json = Json,
) : BackupManager {
    private val context = context.applicationContext

    private val backupHelper by lazy {
        BackupHelper(
            context = context,
            appDatabase = AppDatabase.get(context),
            postContentDatabase = PostContentDatabase.get(context),
            json = json,
        )
    }

    override suspend fun open(
        file: File
    ): List<BackupItem> = withContext(Dispatchers.IO) {
        val tempDir = newTempDir()
        // Unzip backup archive
        ZipUtil.unzip(file, tempDir)
        // Find backup items
        val extractedFiles = tempDir.listFiles() ?: emptyArray()
        val filenameToTypeMap = AppDataType.values().associateBy { getBackupFilename(it) }
        extractedFiles.mapNotNull {
            val type = filenameToTypeMap[it.name] ?: return@mapNotNull null
            BackupItem(type = type, file = it)
        }
    }

    override suspend fun importableCounts(
        items: List<BackupItem>
    ): List<Int> = withContext(Dispatchers.IO) {
        items.map { item ->
            val entityType = when (item.type) {
                AppDataType.Services -> {
                    return@map backupHelper.manifestCount(item.file)
                }

                AppDataType.Users -> {
                    Json.parameterizedType<List<User>>()
                }

                AppDataType.PostContents -> {
                    Json.parameterizedType<List<PostContent>>()
                }

                AppDataType.Posts -> {
                    Json.parameterizedType<List<Post>>()
                }

                AppDataType.Bookmarks -> {
                    Json.parameterizedType<List<Bookmark>>()
                }
            }
            try {
                item.file.inputStream()
                    .use { reader -> json.fromJson<List<*>>(reader, entityType) }!!
                    .size
            } catch (e: Exception) {
                e.printStackTrace()
                0
            }
        }
    }

    override suspend fun import(
        items: List<BackupItem>
    ): List<Result<Int>> = withContext(Dispatchers.IO) {
        // Import
        items.map {
            backupHelper.import(type = it.type, input = it.file)
        }
    }

    override suspend fun exportableCounts(types: List<AppDataType>): List<Int> {
        val db = AppDatabase.get(context)
        val postContentDb = PostContentDatabase.get(context)
        return types.map {
            when (it) {
                AppDataType.Services -> db.serviceDao().count()
                AppDataType.Users -> db.userDao().count()
                AppDataType.Posts -> db.postDao().count()
                AppDataType.Bookmarks -> db.bookmarkDao().count()
                AppDataType.PostContents -> postContentDb.postContentDao().count()
            }
        }
    }

    override suspend fun export(
        types: List<AppDataType>,
        output: File
    ): List<Result<Int>> = withContext(Dispatchers.IO) {
        val tempDir = newTempDir()
        val items = types.map {
            BackupItem(
                type = it,
                file = File(tempDir, getBackupFilename(it)),
            )
        }
        // Export to json files
        val results = items.map {
            backupHelper.export(type = it.type, output = it.file)
        }
        // Compress json files to archive
        ZipUtil.zip(
            inputs = items.map { it.file },
            output = output,
        )
        // Clear json files
        items.forEach { it.file.deleteRecursively() }
        results
    }

    private fun newTempDir(): File {
        val dir = Dirs.backupTempDir(context)
        if (!dir.exists() && !dir.createNewFile()) {
            throw IOException("Cannot create dir: $dir")
        }
        return dir
    }

    private fun getBackupFilename(type: AppDataType): String {
        return when (type) {
            AppDataType.Services -> "services" // folder
            AppDataType.Users -> "users.json"
            AppDataType.Posts -> "posts.json"
            AppDataType.Bookmarks -> "bookmarks.json"
            AppDataType.PostContents -> "post_contents.json"
        }
    }
}
