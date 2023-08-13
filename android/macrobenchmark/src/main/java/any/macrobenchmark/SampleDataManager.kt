package any.macrobenchmark

import android.annotation.SuppressLint
import android.content.Context
import androidx.benchmark.Shell
import androidx.room.Room
import any.data.db.AppDatabase
import any.data.db.PostDao
import any.data.db.ServiceDao
import any.data.db.UserDao
import any.data.entity.Checksums
import any.data.entity.Post
import any.data.entity.PostsViewType
import any.data.entity.ServiceManifest
import any.data.entity.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@SuppressLint("RestrictedApi")
class SampleDataManager(
    private val targetPackageName: String,
    private val context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) {
    private val targetDatabasesPath = context.getDatabasePath("whatever")
        .parentFile!!
        .absolutePath
        .replace(context.packageName, targetPackageName)
    private val targetBackupDatabasesPath =  "${targetDatabasesPath}_backup"

    private val appDbFile = File(context.cacheDir, SAMPLE_APP_DB_NAME)
    private val appDbClz = AppDatabase::class.java
    private val appDbPath = appDbFile.absolutePath
    private val sampleAppDb = Room.databaseBuilder(context, appDbClz, appDbPath).build()

    fun useSampleData() {
        if (!Shell.isSessionRooted()) {
            return
        }
        coroutineScope.launch {
            killTargetApp()
            generateSampleAppDb()
            backupTargetDatabases()
            overwriteTargetAppDatabase()
        }
    }

    fun clearSampleData() {
        if (!Shell.isSessionRooted()) {
            return
        }
        killTargetApp()
        appDbFile.delete()
        restoreDatabasesDir()
    }

    private fun killTargetApp() {
        Shell.executeScriptSilent("am force-stop $targetPackageName")
    }

    private fun backupTargetDatabases() {
        val checkCmd = "[ -d $targetDatabasesPath ] && echo exists"
        val checkRet = Shell.executeScriptCaptureStdout(checkCmd)
        if (checkRet.trim() != "exists") {
            return
        }

        val cmd = "cp -Rp $targetDatabasesPath $targetBackupDatabasesPath"
        val ret = Shell.executeScriptCaptureStdoutStderr(cmd)
        if (ret.stderr.isNotEmpty()) {
            throw Exception("Unable to backup databases dir for $targetPackageName: ${ret.stderr}")
        }
    }

    private fun overwriteTargetAppDatabase() {
        val targetDataDirInfo = getTargetDataDirInfo()

        val mkdirCmd = """
            if [ ! -d $targetDatabasesPath ]; then
                mkdir $targetDatabasesPath
                chmod 771 $targetDatabasesPath
                chown ${targetDataDirInfo.owner} $targetDatabasesPath
                chgrp ${targetDataDirInfo.group} $targetDatabasesPath
            fi
        """.trimIndent()
        Shell.executeScriptSilent(mkdirCmd)

        val targetDbPath = File(targetDatabasesPath, AppDatabase.DB_NAME).absolutePath
        val copyCmd = """
            cp $appDbPath $targetDbPath
            chmod 660 $targetDbPath
            chown ${targetDataDirInfo.owner} $targetDbPath
            chgrp ${targetDataDirInfo.group} $targetDbPath
        """.trimIndent()
        val ret = Shell.executeScriptCaptureStdoutStderr(copyCmd)
        if (ret.stderr.isNotEmpty()) {
            throw Exception("Cannot overwrite app db for $targetPackageName: ${ret.stderr}")
        }

        val cleanDbFilesCmd = """
            rm -f $targetDbPath-shm
            rm -f $targetDbPath-wal
        """.trimIndent()
        Shell.executeScriptSilent(cleanDbFilesCmd)
    }

    private fun getTargetDataDirInfo(): FileInfo {
        val targetDataPath = context.filesDir
            .parentFile!!
            .absolutePath
            .replace(context.packageName, TARGET_PACKAGE_NAME)
        return FileInfo(targetDataPath)
    }

    private fun restoreDatabasesDir() {
        val delCmd = "rm -r $targetDatabasesPath"
        val delRet = Shell.executeScriptCaptureStdoutStderr(delCmd)
        if (delRet.stderr.isNotEmpty()) {
            throw Exception("Cannot delete databases dir for $targetPackageName: ${delRet.stderr}")
        }

        val checkCmd = "[ -d $targetBackupDatabasesPath ] && echo exists"
        val checkRet = Shell.executeScriptCaptureStdout(checkCmd)
        if (checkRet.trim() != "exists") {
            return
        }

        val mvCmd = "mv $targetBackupDatabasesPath $targetDatabasesPath"
        val mvRet = Shell.executeScriptCaptureStdoutStderr(mvCmd)
        if (mvRet.stderr.isNotEmpty()) {
            throw Exception("Cannot restore databases dir for $targetPackageName: ${mvRet.stderr}")
        }
    }

    private suspend fun generateSampleAppDb() {
        sampleAppDb.clearAllTables()

        val serviceDao = sampleAppDb.serviceDao()
        addSampleServices(serviceDao)

        val userDao = sampleAppDb.userDao()
        addSampleUsers(userDao)

        val postsDao = sampleAppDb.postDao()
        addSamplePosts(postsDao)

        // Flush the content of db-wal to main db
        sampleAppDb.query("pragma wal_checkpoint(full)", emptyArray())
        sampleAppDb.close()
    }

    private suspend fun addSampleServices(serviceDao: ServiceDao) {
        serviceDao.add(sampleServices())
    }

    fun sampleServices(): List<ServiceManifest> {
        return PostsViewType.values().map {
            val id = serviceIdFromPostsViewType(it)
            ServiceManifest(
                id = id,
                originalId = id,
                name = serviceNameFromPostsViewType(it),
                description = "",
                developer = "",
                developerUrl = null,
                developerAvatar = null,
                homepage = null,
                changelog = null,
                version = "1.0.0",
                minApiVersion = "0.1.0",
                maxApiVersion = null,
                isPageable = false,
                postsViewType = it,
                mediaAspectRatio = "",
                icon = null,
                headerImage = null,
                themeColor = null,
                darkThemeColor = null,
                main = "",
                mainChecksums = Checksums(md5 = "", sha1 = "", sha256 = "", sha512 = ""),
                languages = listOf(),
                supportedPostUrls = listOf(),
                supportedUserUrls = listOf(),
                configs = listOf(),
                forceConfigsValidation = null,
                isEnabled = true,
                pageKeyOfPage2 = null,
                upgradeUrl = null,
                buildTime = 0,
                addedAt = 0,
                updatedAt = 0,
                source = ServiceManifest.Source.Builtin,
                localResources = listOf(),
            )
        }
    }

    private suspend fun addSampleUsers(userDao: UserDao) {
        val now = System.currentTimeMillis()
        val users = List(30) {
            User(
                serviceId = serviceIdFromPostsViewType(PostsViewType.List),
                id = "userid$it",
                name = "User $it",
                alternativeName = "user$it",
                url = null,
                avatar = "https://picsum.photos/300/300.jpg",
                banner = null,
                description = null,
                postCount = null,
                followerCount = null,
                followingCount = null,
                pageKeyOfPage2 = null,
                followedAt = now + it,
                group = null,
            )
        }
        userDao.add(users)
    }

    private suspend fun addSamplePosts(postDao: PostDao) {
        val postMedias = List(30) {
            listOf(
                Post.Media(
                    type = Post.Media.Type.Photo,
                    url = "https://picsum.photos/500/400.jpg",
                ),
            )
        }
        val postTitles = List(postMedias.size) {
            "Post $it"
        }
        val postSummaries = List(postMedias.size) {
            if (it % 2 == 0) "SAMPLE SUMMARY ".repeat(5) else null
        }
        val postFolders = List(postMedias.size) {
            if (it % 2 == 0) "Folder" else null
        }
        val now = System.currentTimeMillis()
        val posts = PostsViewType.values()
            .map { postsViewType ->
                List(postMedias.size) {
                    Post(
                        title = postTitles[it],
                        url = "https://sample_post_$it",
                        serviceId = serviceIdFromPostsViewType(postsViewType),
                        type = Post.Type.Article,
                        media = postMedias[it],
                        createdAt = now + it,
                        rating = null,
                        date = null,
                        summary = postSummaries[it],
                        author = "The author",
                        authorId = null,
                        avatar = "https://picsum.photos/300/300.jpg",
                        category = null,
                        tags = listOf(),
                        orderInFresh = it,
                        orderInProfile = it,
                        readPosition = 0,
                        collectedAt = now + it,
                        lastReadAt = 0,
                        downloadAt = now + it,
                        folder = postFolders[it],
                        commentCount = 0,
                        commentsKey = null,
                        openInBrowser = false,
                        reference = null
                    )
                }
            }
            .flatten()
        postDao.add(posts)
    }

    private fun serviceIdFromPostsViewType(viewType: PostsViewType): String {
        return "sample.service.${viewType.value}"
    }

    private fun serviceNameFromPostsViewType(viewType: PostsViewType): String {
        return "Sample service [${viewType.value}]"
    }

    private class FileInfo(
        val path: String,
    ) {
        val owner: String

        val group: String

        init {
            val getDetailsCmd = "ls -ld $path"
            val dataDirDetails = Shell.executeScriptCaptureStdout(getDetailsCmd).split(" ")
            owner = dataDirDetails[2]
            group = dataDirDetails[3]
        }
    }

    companion object {
        private const val SAMPLE_APP_DB_NAME = "app_sample.db"
    }
}