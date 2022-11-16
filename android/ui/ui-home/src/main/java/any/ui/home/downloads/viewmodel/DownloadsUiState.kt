package any.ui.home.downloads.viewmodel

import androidx.compose.runtime.Stable

data class DownloadsUiState(
    val isLoadingDownloads: Boolean = false,
    val downloads: List<PostDownload> = emptyList(),
    val selectedDownloadUrls: Set<String> = emptySet(),
) {
    fun isInSelection(): Boolean = selectedDownloadUrls.isNotEmpty()
}

@Stable
data class PostDownload(
    val url: String,
    val serviceId: String,
    val title: String,
    val thumbnail: String?,
    val thumbnailAspectRatio: Float?,
    val downloaded: Int,
    val total: Int,
    val downloadAt: Long,
    val downloadedSize: String? = null,
    val isDownloading: Boolean = false,
    val isWaiting: Boolean = false,
    val isPreparing: Boolean = false,
    val isFailure: Boolean = false,
    private val onStart: () -> Unit,
    private val onCancel: () -> Unit,
) {
    val isComplete: Boolean = downloaded > 0 && downloaded == total

    val sortKey: String = (if (isComplete) "0" else "1") + downloadAt

    fun onStart() = onStart.invoke()

    fun onCancel() = onCancel.invoke()
}