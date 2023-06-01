package any.data.backup

import android.content.Context
import any.base.util.Dirs
import any.base.util.FileUtil
import any.base.util.ZipUtil
import any.base.util.isHttpUrl
import any.data.db.AppDatabase
import any.data.db.PostContentDatabase
import any.data.entity.AppDataType
import any.data.entity.Bookmark
import any.data.entity.Post
import any.data.entity.PostContent
import any.data.entity.ServiceManifest
import any.data.entity.ServiceResource
import any.data.entity.User
import any.data.json.Json
import any.data.json.fromJson
import any.data.json.toJson
import any.data.service.ServiceInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class BackupManagerImpl(
    context: Context,
    private val json: Json = Json,
) : BackupManager {
    private val context = context.applicationContext

    private val appDatabase by lazy { AppDatabase.get(context) }

    private val postContentDatabase by lazy { PostContentDatabase.get(context) }

    private val serviceInstaller by lazy { ServiceInstaller.getDefault(context) }

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
                    return@map getManifestCount(item.file)
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
        items.map { importItem(type = it.type, input = it.file) }
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
        val results = items.map { exportItem(type = it.type, output = it.file) }
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

    private fun getManifestCount(servicesDir: File): Int {
        val serviceDirs = servicesDir.listFiles { dir, _ -> dir.isDirectory } ?: return 0
        return serviceDirs.count { serviceDir ->
            val manifest = File(serviceDir, SERVICE_MANIFEST_FILENAME)
            try {
                manifest.inputStream().use {
                    json.fromJson<ServiceManifest>(it)
                } != null
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * Import posts/bookmarks from json file
     */
    private suspend fun importItem(
        type: AppDataType,
        input: File,
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Read and import
            val count = when (type) {
                AppDataType.Services -> {
                    importServices(input)
                }

                AppDataType.Users -> {
                    input.inputStream().use {
                        importUsers(it)
                    }
                }

                AppDataType.Posts -> {
                    input.inputStream().use {
                        importPosts(it)
                    }
                }

                AppDataType.Bookmarks -> {
                    input.inputStream().use {
                        importBookmarks(it)
                    }
                }

                AppDataType.PostContents -> {
                    input.inputStream().use {
                        importPostContents(it)
                    }
                }
            }
            Result.success(count)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private suspend fun importServices(servicesDir: File): Int {
        val serviceDirs = servicesDir.listFiles { dir, _ -> dir.isDirectory } ?: return 0
        var importedCount = 0
        for (serviceDir in serviceDirs) {
            val manifest = File(serviceDir, SERVICE_MANIFEST_FILENAME)
            if (!manifest.exists()) {
                continue
            }
            val service = serviceInstaller.installFromManifest(manifest)
            if (service != null) {
                appDatabase.serviceDao().add(service)
                importedCount++
            }
        }
        return importedCount
    }

    private suspend fun importUsers(inputStream: InputStream): Int {
        val type = Json.parameterizedType<List<User>>()
        val users = json.fromJson<List<User>>(inputStream, type)
        if (users.isNullOrEmpty()) {
            return 0
        }
        appDatabase.userDao().add(users)
        return users.size
    }

    private suspend fun importPosts(inputStream: InputStream): Int {
        val type = Json.parameterizedType<List<Post>>()
        val posts = json.fromJson<List<Post>>(inputStream, type)
        if (posts.isNullOrEmpty()) {
            return 0
        }
        appDatabase.postDao().add(posts)
        return posts.size
    }

    private suspend fun importBookmarks(inputStream: InputStream): Int {
        val type = Json.parameterizedType<List<Bookmark>>()
        val bookmarks = json.fromJson<List<Bookmark>>(inputStream, type)
        if (bookmarks.isNullOrEmpty()) {
            return 0
        }
        appDatabase.bookmarkDao().add(bookmarks)
        return bookmarks.size
    }

    private suspend fun importPostContents(inputStream: InputStream): Int {
        val type = Json.parameterizedType<List<PostContent>>()
        val contents = json.fromJson<List<PostContent>>(inputStream, type)
        if (contents.isNullOrEmpty()) {
            return 0
        }
        postContentDatabase.postContentDao().add(contents)
        return contents.size
    }

    /**
     * Export posts/bookmarks to json file
     */
    private suspend fun exportItem(
        type: AppDataType,
        output: File
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Export
            val count = when (type) {
                AppDataType.Services -> {
                    exportServices(output)
                }

                AppDataType.Users -> {
                    output.outputStream().use {
                        exportUsers(it)
                    }
                }

                AppDataType.Posts -> {
                    output.outputStream().use {
                        exportPosts(it)
                    }
                }

                AppDataType.Bookmarks -> {
                    output.outputStream().use {
                        exportBookmarks(it)
                    }
                }

                AppDataType.PostContents -> {
                    output.outputStream().use {
                        exportPostContents(it)
                    }
                }
            }
            Result.success(count)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private suspend fun exportServices(dir: File): Int {
        if (!dir.exists() && !dir.mkdirs()) {
            throw Exception("Cannot create dir: $dir")
        }
        val services = appDatabase.serviceDao().getAll()
        for (service in services) {
            val serviceFolder = File(dir, FileUtil.buildValidFatFilename(service.id))
            serviceFolder.mkdir()
            val updatedResources = mutableMapOf<ServiceResource.Type, String>()
            for (res in service.resources()) {
                val type = res.type
                val path = res.path
                if (path.isEmpty() || path.isHttpUrl()) {
                    continue
                }
                val src = File(path)
                val dst = File(serviceFolder, src.name)
                if (src.exists()) {
                    // Local file
                    src.copyTo(dst)
                } else if (FileUtil.isAssetsFile(path)) {
                    // Assets file
                    FileUtil.readAssetsFile(context, path).use { input ->
                        dst.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                updatedResources[type] = dst.name
            }
            // Write manifest
            val localResources = updatedResources.map { ServiceResource(it.key, it.value) }
            val updatedService = service.copy(localResources = localResources)
            val manifest = File(serviceFolder, SERVICE_MANIFEST_FILENAME)
            manifest.outputStream().use {
                json.toJson(updatedService, it)
            }
        }
        return services.size
    }

    private suspend fun exportUsers(outputStream: OutputStream): Int {
        val users = appDatabase.userDao().getAll()
        json.toJson(users, outputStream)
        return users.size
    }

    private suspend fun exportPosts(outputStream: OutputStream): Int {
        val posts = appDatabase.postDao().getAll()
        json.toJson(posts, outputStream)
        return posts.size
    }

    private suspend fun exportBookmarks(outputStream: OutputStream): Int {
        val bookmarks = appDatabase.bookmarkDao().getAll()
        json.toJson(bookmarks, outputStream)
        return bookmarks.size
    }

    private suspend fun exportPostContents(outputStream: OutputStream): Int {
        val contents = postContentDatabase.postContentDao().getAll()
        json.toJson(contents, outputStream)
        return contents.size
    }

    companion object {
        private const val SERVICE_MANIFEST_FILENAME = "manifest.json"
    }
}
