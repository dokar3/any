package any.ui.home.downloads.viewmodel

import androidx.compose.runtime.Immutable

@Immutable
data class DownloadsUiState(
    val isLoadingDownloads: Boolean = false,
    val downloads: List<PostDownload> = emptyList(),
    val selectedDownloadUrls: Set<String> = emptySet(),
    val downloadTypes: List<DownloadType> = emptyList(),
    val selectedDownloadType: DownloadType = DownloadType.All(0),
) {
    fun isInSelection(): Boolean = selectedDownloadUrls.isNotEmpty()
}

@Immutable
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

@Immutable
sealed class DownloadType(val count: Int) {
    @Immutable
    class All(count: Int) : DownloadType(count)

    @Immutable
    class Downloading(count: Int) : DownloadType(count)

    @Immutable
    class Downloaded(count: Int) : DownloadType(count)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DownloadType

        if (count != other.count) return false

        return true
    }

    override fun hashCode(): Int {
        return count
    }
}
