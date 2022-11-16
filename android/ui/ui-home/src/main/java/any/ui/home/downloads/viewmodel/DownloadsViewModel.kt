package any.ui.home.downloads.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import any.base.util.FileUtil
import any.data.ThumbAspectRatio
import any.data.entity.Post
import any.data.entity.PostContent
import any.data.repository.PostContentRepository
import any.data.repository.ReactiveRepository
import any.data.repository.ServiceRepository
import any.data.source.post.LocalPostDataSource
import any.data.source.post.LocalPostDataSourceImpl
import any.download.PostImageDownloader
import any.download.PostImageDownloader.DownloadStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DownloadsViewModel(
    private val serviceRepository: ServiceRepository,
    private val postContentRepository: PostContentRepository,
    private val localPostDataSource: LocalPostDataSource,
    private val downloader: PostImageDownloader,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _downloadsUiState = MutableStateFlow(DownloadsUiState())
    val downloadsUiState: StateFlow<DownloadsUiState> = _downloadsUiState

    private var observeJob: Job? = null

    init {
        viewModelScope.launch {
            postContentRepository.changes.collect { changes ->
                changes.forEach {
                    val url = when (it) {
                        is ReactiveRepository.Change.Inserted<*> ->
                            (it.item as PostContent).url

                        is ReactiveRepository.Change.Updated<*> ->
                            (it.item as PostContent).url

                        is ReactiveRepository.Change.DeletedByItem<*> ->
                            (it.item as PostContent).url

                        is ReactiveRepository.Change.DeletedByKey<*> ->
                            it.key as String
                    }
                    downloader.tryUpdateDownloadStatus(url)
                }
            }
        }
    }

    fun loadAllDownloads() {
        viewModelScope.launch(ioDispatcher) {
            _downloadsUiState.update {
                it.copy(isLoadingDownloads = true)
            }

            val services = serviceRepository.loadDbServices().associateBy { it.id }

            val currDownloads = _downloadsUiState.value.downloads
                .associateBy { it.serviceId + it.url }

            val inDownloadPosts = localPostDataSource.fetchInDownloadPosts().first()

            val downloads = inDownloadPosts
                .map { post ->
                    val status = downloader.getDownloadStatusSync(post)

                    val thumb = post.media?.firstOrNull()
                    val service = services[post.serviceId]
                    val thumbAspectRatio = ThumbAspectRatio.parseOrNull(thumb?.aspectRatio)
                        ?: ThumbAspectRatio.parseOrNull(service?.mediaAspectRatio)

                    val currDownload = currDownloads[post.serviceId + post.url]

                    PostDownload(
                        url = post.url,
                        serviceId = post.serviceId,
                        title = post.title,
                        thumbnail = thumb?.thumbnailOrNull(),
                        thumbnailAspectRatio = thumbAspectRatio,
                        downloaded = status.downloaded,
                        total = status.total,
                        downloadAt = post.downloadAt,
                        onStart = { downloader.downloadPostImages(post) },
                        onCancel = { downloader.cancel(post.url) },
                        downloadedSize = currDownload?.downloadedSize,
                        isDownloading = currDownload?.isDownloading ?: false,
                        isPreparing = currDownload?.isPreparing ?: false,
                        isWaiting = currDownload?.isWaiting ?: false,
                        isFailure = currDownload?.isFailure ?: false,
                    )
                }
                .sortedByDescending(PostDownload::sortKey)

            _downloadsUiState.update {
                it.copy(
                    isLoadingDownloads = false,
                    downloads = downloads,
                )
            }

            observeJob?.cancel()
            observeJob = launch { observeDownloads(inDownloadPosts) }
        }
    }

    private suspend fun observeDownloads(posts: List<Post>) {
        downloader.getDownloadStatus(posts).collect { (postUrl, status) ->
            val downloads = _downloadsUiState.value.downloads.toMutableList()
            if (downloads.isEmpty()) return@collect
            val idx = downloads.indexOfFirst { it.url == postUrl }
            if (idx == -1) return@collect

            val isDownloading = status is DownloadStatus.Downloading
            val isWaiting = status is DownloadStatus.Waiting
            val download = downloads[idx]

            val dirLength = status.images
                .mapNotNull { downloader.getDownloadedFile(it) }
                .sumOf { if (it.isFile) it.length() else 0L }
            val downloadedSize = FileUtil.byteCountToString(dirLength)

            downloads[idx] = download.copy(
                downloaded = status.downloaded,
                total = status.total,
                isDownloading = isDownloading,
                isWaiting = isWaiting,
                isPreparing = status is DownloadStatus.Preparing,
                isFailure = status is DownloadStatus.Failure,
                downloadedSize = downloadedSize,
            )

            downloads.sortByDescending(PostDownload::sortKey)

            _downloadsUiState.update {
                it.copy(downloads = downloads)
            }
        }
    }

    fun selectAll() = viewModelScope.launch(ioDispatcher) {
        val downloads = _downloadsUiState.value.downloads.toMutableList()
        val urls = downloads.fold(mutableSetOf<String>()) { acc, download ->
            acc.add(download.url)
            acc
        }
        _downloadsUiState.update { it.copy(selectedDownloadUrls = urls) }
    }

    fun select(download: PostDownload) = viewModelScope.launch(ioDispatcher) {
        val urls = _downloadsUiState.value.selectedDownloadUrls.toMutableSet()
        urls.add(download.url)
        _downloadsUiState.update { it.copy(selectedDownloadUrls = urls) }
    }

    fun unselect(download: PostDownload) = viewModelScope.launch(ioDispatcher) {
        val urls = _downloadsUiState.value.selectedDownloadUrls.toMutableSet()
        urls.remove(download.url)
        _downloadsUiState.update { it.copy(selectedDownloadUrls = urls) }
    }

    fun unselectAll() = viewModelScope.launch(ioDispatcher) {
        _downloadsUiState.update { it.copy(selectedDownloadUrls = emptySet()) }
    }

    fun removeSelectedDownloads(
        deleteFiles: Boolean
    ) = viewModelScope.launch(ioDispatcher) {
        val urls = _downloadsUiState.value.selectedDownloadUrls
        val selected = _downloadsUiState.value.downloads.filter { urls.contains(it.url) }
        for (download in selected) {
            val dbPost = localPostDataSource.fetchPost(download.serviceId, download.url)
                .first() ?: continue
            localPostDataSource.update(dbPost.markUnDownload())
            if (deleteFiles) {
                downloader.delete(dbPost)
            }
        }
        _downloadsUiState.update {
            it.copy(selectedDownloadUrls = emptySet())
        }
        loadAllDownloads()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DownloadsViewModel(
                serviceRepository = ServiceRepository.getDefault(context),
                postContentRepository = PostContentRepository.getDefault(context),
                localPostDataSource = LocalPostDataSourceImpl.getDefault(context),
                downloader = PostImageDownloader.get(context),
            ) as T
        }
    }
}