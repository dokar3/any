package any.data.backup

import android.content.Context
import any.base.util.FileUtil
import any.base.util.isHttpUrl
import any.data.Json
import any.data.ServiceInstaller
import any.data.db.AppDatabase
import any.data.db.PostContentDatabase
import any.data.entity.AppDataType
import any.data.entity.Bookmark
import any.data.entity.Post
import any.data.entity.PostContent
import any.data.entity.ServiceManifest
import any.data.entity.ServiceResource
import any.data.entity.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream

internal class BackupHelper(
    context: Context,
    private val appDatabase: AppDatabase,
    private val postContentDatabase: PostContentDatabase,
    private val json: Json = Json,
) {
    private val appContext = context.applicationContext

    private val serviceInstaller = ServiceInstaller.getDefault(context)

    fun manifestCount(servicesDir: File): Int {
        val serviceDirs = servicesDir.listFiles { dir, _ -> dir.isDirectory } ?: return 0
        return serviceDirs.count { serviceDir ->
            val manifest = File(serviceDir, MANIFEST_FILENAME)
            manifest.inputStream().use {
                json.fromJson(it, ServiceManifest::class.java)
            } != null
        }
    }

    /**
     * Import posts/bookmarks from json file
     */
    suspend fun import(
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
            val manifest = File(serviceDir, MANIFEST_FILENAME)
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
    suspend fun export(
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
                    FileUtil.readAssetsFile(appContext, path).use { input ->
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
            val manifest = File(serviceFolder, MANIFEST_FILENAME)
            manifest.outputStream().use {
                json.toJson(updatedService, it, ServiceManifest::class.java)
            }
        }
        return services.size
    }

    private suspend fun exportUsers(outputStream: OutputStream): Int {
        val users = appDatabase.userDao().getAll()
        json.toJson(users, outputStream, List::class.java)
        return users.size
    }

    private suspend fun exportPosts(outputStream: OutputStream): Int {
        val posts = appDatabase.postDao().getAll()
        json.toJson(posts, outputStream, List::class.java)
        return posts.size
    }

    private suspend fun exportBookmarks(outputStream: OutputStream): Int {
        val bookmarks = appDatabase.bookmarkDao().getAll()
        json.toJson(bookmarks, outputStream, List::class.java)
        return bookmarks.size
    }

    private suspend fun exportPostContents(outputStream: OutputStream): Int {
        val contents = postContentDatabase.postContentDao().getAll()
        json.toJson(contents, outputStream, List::class.java)
        return contents.size
    }

    companion object {
        private const val MANIFEST_FILENAME = "manifest.json"
    }
}