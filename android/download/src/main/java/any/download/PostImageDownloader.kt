package any.download

import android.content.Context
import android.webkit.MimeTypeMap
import any.base.image.DownloadedImageFetcher
import any.base.image.ImageLoader
import any.base.image.ImageRequest
import any.base.image.ImageResult
import any.base.image.PostImageSources
import any.base.log.Logger
import any.base.util.Dirs
import any.base.util.Http
import any.base.util.NetworkUtil
import any.base.util.md5HexString
import any.data.FetchState
import any.data.entity.Post
import any.data.repository.PostContentRepository
import any.data.repository.PostRepository
import any.data.repository.ServiceRepository
import any.data.service.ServiceLookup
import any.data.source.post.LocalPostDataSource
import any.data.source.post.LocalPostDataSourceImpl
import any.domain.post.PostContentParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class PostImageDownloader private constructor(
    context: Context,
    private val serviceRepository: ServiceRepository,
    private val postRepository: PostRepository,
    private val postContentRepository: PostContentRepository,
    private val postContentParser: PostContentParser,
    private val localPostDataSource: LocalPostDataSource,
    private val downloadScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    maxSimultaneousDownloads: Int = 5,
) : DownloadedImageFetcher {
    private val appContext = context.applicationContext

    private val downloadDir = Dirs.postImageDownloadDir(appContext)

    private val downloads: MutableMap<String, DownloadTask> = ConcurrentHashMap()

    private val downloadSemaphore = Semaphore(permits = maxSimultaneousDownloads)

    private var downloadJob: Job? = null

    private val downloadStatusFlow = MutableSharedFlow<Pair<String, DownloadStatus>>(
        extraBufferCapacity = 2,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val httpClient: OkHttpClient = Http.DEFAULT_CLIENT

    init {
        require(maxSimultaneousDownloads in 1..64) {
            "Max simultaneous downloads must be in the range [0, 64]"
        }
    }

    fun downloadPostImages(post: Post) {
        if (downloads.containsKey(post.url)) {
            updateTask(post.url) {
                val minPriority = downloads.values.minOfOrNull { d -> d.priority }
                it.copy(
                    canceled = false,
                    status = DownloadStatus.Waiting(
                        it.images,
                        it.status.downloaded,
                        it.status.total,
                    ),
                    priority = if (minPriority != null) minPriority - 1 else 0
                )
            }
            if (!isDownloading(post)) {
                startDownload()
            }
        } else {
            downloadScope.launch {
                val task = newDownloadAndFetchStatus(post)
                downloads[post.url] = task
                startDownload()
            }
        }

        downloadScope.launch(Dispatchers.IO) {
            val dbPost = localPostDataSource.fetchPost(post.serviceId, post.url).first()
            if (dbPost == null) {
                localPostDataSource.insert(post.markInDownload())
            } else if (!dbPost.isInDownload()) {
                localPostDataSource.update(post.markInDownload())
            }
        }
    }

    fun getDownloadStatus(posts: List<Post>): Flow<Pair<String, DownloadStatus>> {
        return downloadStatusFlow
            .onStart {
                for (post in posts) {
                    val task = downloads.getOrPut(post.url) {
                        newDownloadAndFetchStatus(post)
                    }
                    emit(post.url to task.status)
                }
            }
    }

    fun getDownloadStatus(post: Post): Flow<DownloadStatus> {
        val postUrl = post.url
        return downloadStatusFlow
            .onStart {
                val task = downloads.getOrPut(postUrl) {
                    newDownloadAndFetchStatus(post)
                }
                emit(postUrl to task.status)
            }
            .filter { it.first == postUrl }
            .map { it.second }
            .distinctUntilChanged()
    }

    fun tryUpdateDownloadStatus(postUrl: String) {
        downloadScope.launch {
            val task = downloads[postUrl] ?: return@launch
            val currImages = task.images
            val currDownloaded = task.status.downloaded
            val images = getPostImages(postUrl)
            val downloaded = getDownloadedCount(images)
            if (currImages != images || currDownloaded != downloaded) {
                updateTask(postUrl) {
                    it.copy(
                        status = it.status.copy(
                            images = images,
                            downloaded = downloaded,
                            total = images.size,
                        ),
                        images = images,
                    )
                }
            }
        }
    }

    suspend fun getDownloadStatusSync(post: Post): DownloadStatus {
        return downloads.getOrPut(post.url) { newDownloadAndFetchStatus(post) }.status
    }

    fun hasDownloadingTasks(): Boolean {
        return downloads.values.find { it.status is DownloadStatus.Downloading } != null
    }

    fun isDownloading(post: Post): Boolean {
        return isDownloading() && downloads[post.url]?.status is DownloadStatus.Downloading
    }

    override fun getDownloadedFile(url: String): File? {
        val file = File(downloadDir, getImageFilename(url))
        return if (file.exists() && file.isFile && file.length() > 0) file else null
    }

    fun cancel(postUrl: String) {
        val task = downloads[postUrl] ?: return
        updateTask(postUrl) {
            it.jobs.forEach(Job::cancel)
            it.copy(
                canceled = true,
                status = DownloadStatus.Finished(
                    it.images,
                    it.status.downloaded,
                    it.status.total,
                ),
            )
        }
        // Cancel running http calls
        val images = task.images.toHashSet()
        for (call in httpClient.dispatcher.runningCalls()) {
            if (images.contains(call.request().tag())) {
                call.cancel()
            }
        }
    }

    fun cancelAll() {
        for ((postUrl, task) in downloads) {
            if (!task.canceled || task.status !is DownloadStatus.Finished) {
                cancel(postUrl)
            }
        }
        downloadJob?.cancel()
    }

    fun delete(post: Post) {
        fun done() {
            updateTask(post.url) {
                it.copy(
                    status = DownloadStatus.Finished(
                        images = it.images,
                        downloaded = 0,
                        total = it.images.size,
                    )
                )
            }
        }
        downloadScope.launch {
            if (isDownloading(post)) {
                cancel(post.url)
            }

            if (!downloadDir.exists()) {
                done()
                return@launch
            }

            val images = getPostImages(post.url)
            if (images.isEmpty()) {
                done()
                return@launch
            }

            val imageFiles = images
                .distinct()
                .mapNotNull(::getDownloadedFile)
            if (imageFiles.isEmpty()) {
                done()
                return@launch
            }

            imageFiles.forEach {
                if (!it.delete()) {
                    Logger.e(TAG, "Failed to delete image: $it")
                }
            }

            done()
        }
    }

    private fun newDownload(post: Post, images: List<String>): DownloadTask {
        return DownloadTask(
            status = DownloadStatus.Waiting(images, 0, images.size),
            serviceId = post.serviceId,
            postUrl = post.url,
            images = images,
            priority = downloads.size,
        )
    }

    private suspend fun newDownloadAndFetchStatus(post: Post): DownloadTask {
        val images = getPostImages(post.url)
        return newDownload(post, images).let {
            val initialStatus = DownloadStatus.Finished(
                images = images,
                downloaded = getDownloadedCount(images),
                total = images.size
            )
            it.copy(status = initialStatus)
        }
    }

    private fun startDownload() {
        if (isDownloading()) {
            return
        }
        downloadJob = startDownloadJob()
    }

    private fun startDownloadJob(): Job = downloadScope.launch(Dispatchers.IO) {
        while (downloads.isNotEmpty()) {
            val currTask = findNextTask() ?: return@launch
            val postUrl = currTask.postUrl

            val task = if (!postContentRepository.contains(postUrl)) {
                updateTask(postUrl) {
                    val status = DownloadStatus.Preparing(
                        images = it.images,
                        downloaded = 0,
                        total = 1,
                    )
                    it.copy(status = status)
                }
                try {
                    fetchPostContent(currTask.serviceId, currTask.postUrl)
                    val images = getPostImages(postUrl)
                    val updatedTask = currTask.copy(
                        images = images,
                        status = currTask.status.copy(
                            images = images,
                            downloaded = getDownloadedCount(images),
                            total = images.size,
                        ),
                    )
                    updateTask(postUrl) { updatedTask }
                    updatedTask
                } catch (e: Exception) {
                    Logger.e(TAG, "Cannot fetch post content: $e")
                    currTask
                }
            } else {
                currTask
            }

            val images = task.images
            val total = images.size

            val downloaded = AtomicInteger(getDownloadedCount(images))
            val remainingJobs = AtomicInteger(total)
            val hasError = AtomicBoolean(false)

            updateTask(postUrl) {
                it.copy(status = DownloadStatus.Downloading(it.images, downloaded.get(), total))
            }

            // Check directory
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }

            Logger.d(TAG, "Downloading task: ${task.postUrl}, images: ${images.size}")

            fun checkParentCancellation(): Boolean {
                if (isActive) {
                    return false
                }
                if (downloads[postUrl] == task) {
                    updateTask(postUrl) {
                        it.copy(
                            canceled = true,
                            status = DownloadStatus.Finished(it.images, downloaded.get(), total),
                        )
                    }
                } else {
                    updateTask(postUrl) {
                        it.copy(canceled = true)
                    }
                }
                Logger.d(TAG, "Parent download job canceled, current post: ${task.postUrl}")
                return true
            }

            fun checkTaskCancellation(): Boolean {
                if (downloads[postUrl]?.canceled != true) {
                    return false
                }
                updateTask(postUrl) {
                    val status = DownloadStatus.Finished(
                        images = it.images,
                        downloaded = it.status.downloaded,
                        total = it.status.total,
                    )
                    it.copy(status = status)
                }
                Logger.d(TAG, "Download task canceled, post: ${task.postUrl}")
                return true
            }

            fun checkNetwork(): Boolean {
                if (isNetworkAvailable()) {
                    return true
                }
                hasError.set(true)
                Logger.e(TAG, "Download failed, network not available. Post: ${task.postUrl}")
                return false
            }

            fun checkFinished() {
                if (remainingJobs.get() > 0) {
                    return
                }
                Logger.d(TAG, "Download finished, $downloaded / $total")
                val newStatus = if (hasError.get()) {
                    DownloadStatus.Failure(
                        error = Exception("Download not complete"),
                        images = task.images,
                        downloaded = downloaded.get(),
                        total = total,
                    )
                } else {
                    DownloadStatus.Finished(
                        images = task.images,
                        downloaded = downloaded.get(),
                        total = total,
                    )
                }
                updateTask(postUrl) {
                    it.copy(status = newStatus, canceled = false)
                }
            }

            val jobs = images.map { image ->
                async {
                    downloadSemaphore.withPermit {
                        if (checkParentCancellation() ||
                            checkTaskCancellation() ||
                            !checkNetwork()
                        ) {
                            return@async
                        }

                        val filename = getImageFilename(image)
                        // Download image
                        val result = downloadImage(
                            filename = filename,
                            url = image,
                        )

                        when (result) {
                            ImageDownloadResult.Failure -> {
                                val isCanceled = downloads[postUrl]?.canceled == true
                                hasError.set(hasError.get() || !isCanceled)
                            }

                            ImageDownloadResult.Success -> {
                                updateTask(postUrl) {
                                    val status = if (downloads[postUrl]?.canceled == true) {
                                        DownloadStatus.Finished(
                                            images = it.images,
                                            downloaded = downloaded.incrementAndGet(),
                                            total = total,
                                        )
                                    } else {
                                        DownloadStatus.Downloading(
                                            images = it.images,
                                            downloaded = downloaded.incrementAndGet(),
                                            total = total,
                                        )
                                    }
                                    it.copy(status = status)
                                }
                            }

                            ImageDownloadResult.Skipped -> {}
                        }
                    }
                }.also {
                    it.invokeOnCompletion {
                        remainingJobs.decrementAndGet()
                        checkFinished()
                    }
                }
            }

            updateTask(postUrl) {
                it.copy(
                    jobs = jobs,
                    status = DownloadStatus.Downloading(it.images, downloaded.get(), total),
                )
            }
        }
    }

    private suspend fun fetchPostContent(
        serviceId: String,
        postUrl: String,
    ) {
        val services = serviceRepository.getDbServices()
        val service = ServiceLookup.find(
            services = services,
            targetServiceId = serviceId,
            postUrl = postUrl,
        ) ?: return
        postRepository.fetchPost(
            service = service,
            postServiceId = serviceId,
            postUrl = postUrl,
            strategy = PostRepository.FetchPostStrategy.RemoteOnly,
        ).first { it is FetchState.Success || it is FetchState.Failure }
    }

    private suspend fun downloadImage(
        filename: String,
        url: String,
    ): ImageDownloadResult {
        val targetFile = File(downloadDir, filename)

        // Check if file was downloaded
        if (targetFile.exists() && targetFile.isFile && targetFile.length() > 0) {
            return ImageDownloadResult.Skipped
        }

        // Check caches
        val imageRequest = ImageRequest.Downloadable(url = url)
        val sources = PostImageSources.all() -
                // Skip bitmap cache
                PostImageSources.memory() -
                // Skip network loading, we use OkHttp to download directly
                PostImageSources.network()
        val cachedResult = ImageLoader
            .fetchImage(request = imageRequest, sources = sources)
            .firstOrNull()
        if (cachedResult is ImageResult.File) {
            val file = cachedResult.value
            return try {
                file.copyTo(target = targetFile, overwrite = true)
                ImageDownloadResult.Success
            } catch (e: IOException) {
                e.printStackTrace()
                ImageDownloadResult.Failure
            }
        }

        // Download
        return try {
            val request = Request.Builder()
                .tag(url)
                .url(url)
                .get()
                .build()
            val response = httpClient.newCall(request).execute()
            val tempFile = File(targetFile.absolutePath + ".downloading")
            response.body?.source()?.use { source ->
                tempFile.sink().buffer().use { sink ->
                    sink.writeAll(source)
                }
                tempFile.renameTo(targetFile)
            }
            ImageDownloadResult.Success
        } catch (e: IOException) {
            e.printStackTrace()
            ImageDownloadResult.Failure
        }
    }

    private fun findNextTask(): DownloadTask? {
        return downloads.values
            .filter { !it.canceled && it.status is DownloadStatus.Waiting }
            .maxByOrNull { it.priority }
    }

    private inline fun updateTask(
        postUrl: String,
        update: (DownloadTask) -> DownloadTask,
    ) {
        val task = downloads[postUrl] ?: return
        val updated = update(task)
        downloads[postUrl] = updated
        downloadStatusFlow.tryEmit(postUrl to updated.status)
    }

    private fun isDownloading(): Boolean {
        return downloadJob?.isActive == true
    }

    private fun isNetworkAvailable(): Boolean {
        return NetworkUtil.isNetworkConnected(appContext)
    }

    private fun getImageFilename(url: String): String {
        val extension = MimeTypeMap.getFileExtensionFromUrl(url).let {
            when (it) {
                "jpeg" -> "jpg"
                "" -> FALLBACK_EXTENSION
                else -> it
            }
        }
        return "${url.md5HexString()}.$extension"
    }

    private suspend fun getPostImages(postUrl: String): List<String> {
        val content = postContentRepository.get(postUrl) ?: return emptyList()
        return postContentParser.parse(content = content, reverseElement = false).images
    }

    private fun getDownloadedCount(images: List<String>): Int {
        var downloaded = 0
        for (i in images.indices) {
            if (getDownloadedFile(images[i]) != null) {
                downloaded++
            }
        }
        return downloaded
    }

    private data class DownloadTask(
        val status: DownloadStatus,
        val serviceId: String,
        val postUrl: String,
        val images: List<String>,
        val priority: Int,
        val jobs: List<Job> = emptyList(),
        val canceled: Boolean = false,
    )

    private sealed interface ImageDownloadResult {
        object Success : ImageDownloadResult

        object Failure : ImageDownloadResult

        object Skipped : ImageDownloadResult
    }

    sealed class DownloadStatus(
        val images: List<String>,
        val downloaded: Int,
        val total: Int
    ) {
        abstract fun copy(
            images: List<String> = this.images,
            downloaded: Int = this.downloaded,
            total: Int = this.total,
        ): DownloadStatus

        object FetchingStatus : DownloadStatus(emptyList(), -1, -1) {
            override fun copy(
                images: List<String>,
                downloaded: Int,
                total: Int,
            ): FetchingStatus {
                return this
            }
        }

        class Waiting(
            images: List<String>,
            downloaded: Int,
            total: Int,
        ) : DownloadStatus(
            images,
            downloaded,
            total,
        ) {
            override fun copy(
                images: List<String>,
                downloaded: Int,
                total: Int,
            ): Waiting {
                return Waiting(images, downloaded, total)
            }
        }

        class Preparing(
            images: List<String>,
            downloaded: Int,
            total: Int,
        ) : DownloadStatus(
            images,
            downloaded,
            total,
        ) {
            override fun copy(
                images: List<String>,
                downloaded: Int,
                total: Int,
            ): Preparing {
                return Preparing(images, downloaded, total)
            }
        }

        class Downloading(
            images: List<String>,
            downloaded: Int,
            total: Int,
        ) : DownloadStatus(
            images,
            downloaded,
            total,
        ) {
            override fun copy(
                images: List<String>,
                downloaded: Int,
                total: Int,
            ): Downloading {
                return Downloading(images, downloaded, total)
            }
        }

        class Finished(
            images: List<String>,
            downloaded: Int,
            total: Int,
        ) : DownloadStatus(
            images,
            downloaded,
            total,
        ) {
            override fun copy(
                images: List<String>,
                downloaded: Int,
                total: Int,
            ): Finished {
                return Finished(images, downloaded, total)
            }

            fun isComplete() = downloaded == total
        }

        class Failure(
            val error: Throwable,
            images: List<String>,
            downloaded: Int,
            total: Int
        ) : DownloadStatus(images, downloaded, total) {
            override fun copy(
                images: List<String>,
                downloaded: Int,
                total: Int,
            ): Failure {
                return Failure(error, images, downloaded, total)
            }
        }

        override fun toString(): String {
            return "${this.javaClass.simpleName}: $downloaded/$total"
        }
    }

    companion object {
        private const val TAG = "ImageDownloader"

        private const val FALLBACK_EXTENSION = "jpg"

        @Volatile
        private var INSTANCE: PostImageDownloader? = null

        fun get(context: Context): PostImageDownloader {
            return INSTANCE ?: synchronized(PostImageDownloader::class) {
                INSTANCE ?: PostImageDownloader(
                    context = context,
                    serviceRepository = ServiceRepository.getDefault(context),
                    postRepository = PostRepository.getDefault(context),
                    postContentRepository = PostContentRepository.getDefault(context),
                    postContentParser = PostContentParser(),
                    localPostDataSource = LocalPostDataSourceImpl.getDefault(context),
                ).also {
                    INSTANCE = it
                }
            }
        }
    }
}