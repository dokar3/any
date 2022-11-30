package any.ui.settings.files.viewmodel

import any.base.R as BaseR
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import any.base.AndroidStrings
import any.base.Strings
import any.base.util.updateIf
import any.data.backup.BackupItem
import any.data.backup.BackupManager
import any.data.backup.BackupManagerImpl
import any.data.cleanable.Cleanable
import any.data.cleanable.CleanableProvider
import any.data.cleanable.CleanableProviderImpl
import any.data.entity.AppDataType
import any.data.entity.Post
import any.data.entity.ServiceManifest
import any.data.repository.BookmarkRepository
import any.data.repository.PostContentRepository
import any.data.repository.PostRepository
import any.data.repository.ServiceRepository
import any.data.source.post.LocalPostDataSource
import any.data.source.post.LocalPostDataSourceImpl
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class FilesAndDataViewModel(
    private val serviceRepository: ServiceRepository,
    private val postDataSource: LocalPostDataSource,
    private val postContentRepository: PostContentRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val cleanableProvider: CleanableProvider,
    private val backupManager: BackupManager,
    private val strings: Strings,
    private val bgDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val _filesUiState = MutableStateFlow(FilesUiState())
    val filesUiState: StateFlow<FilesUiState> = _filesUiState

    private val _backupUiState = MutableStateFlow(BackupUiState())
    val backupUiState: StateFlow<BackupUiState> = _backupUiState

    init {
        loadCleanableItems()
    }

    /**
     * Clean up unused items in the database
     */
    fun cleanUpDatabase() {
        viewModelScope.launch(bgDispatcher) {
            val services = serviceRepository.getDbServices()
                .map(ServiceManifest::id)
                .toHashSet()
            val now = System.currentTimeMillis()
            val posts = postDataSource.loadAll()
            val toRemove = posts.filter {
                if (!services.contains(it.serviceId)) {
                    // Remove: The target service does not exist anymore and it's not collected
                    // or in-download
                    return@filter !(it.isCollected() || it.isInDownload())
                }
                // Do NOT remove: First page, collected, in-download
                if (it.isInUsing()) return@filter false
                // Remove: Expired
                if (now - it.createdAt > PostRepository.MAX_AGE_FOR_INVALID_CACHE)
                    return@filter true
                false
            }

            // Remove invalid posts
            postDataSource.remove(toRemove)
            toRemove.forEach {
                // Remove post contents
                postContentRepository.remove(it.url)
                // Remove bookmarks
                bookmarkRepository.removePostBookmarks(it.serviceId, it.url)
            }

            val restPosts = (posts - toRemove.toSet()).map(Post::url).toHashSet()

            // Remove invalid contents
            val contents = postContentRepository.keys()
            for (contentUrl in contents) {
                if (!restPosts.contains(contentUrl)) {
                    postContentRepository.remove(contentUrl)
                }
            }

            // Remove invalid bookmarks
            val bookmarks = bookmarkRepository.getAll()
            for (bookmark in bookmarks) {
                if (!restPosts.contains(bookmark.postUrl)) {
                    bookmarkRepository.removeBookmark(bookmark)
                }
            }
        }
    }

    private fun loadCleanableItems() = viewModelScope.launch(bgDispatcher) {
        val downloadedImagesInfo = Pair(
            strings(BaseR.string.downloaded_images),
            strings(BaseR.string.downloaded_images_clean_alert)
        )
        val diskCacheImagesInfo = Pair(
            strings(BaseR.string.image_cache),
            strings(BaseR.string.image_cache_clean_alert)
        )
        val diskCacheVideosInfo = Pair(
            strings(BaseR.string.video_cache),
            strings(BaseR.string.video_cache_clean_alert)
        )
        val items = listOf(
            cleanableProvider.get(Cleanable.Type.DownloadedImage) to downloadedImagesInfo,
            cleanableProvider.get(Cleanable.Type.DiskCacheImages) to diskCacheImagesInfo,
            cleanableProvider.get(Cleanable.Type.DiskCacheVideos) to diskCacheVideosInfo,
        ).map {
            val cleanable = it.first
            val info = it.second
            val id = cleanable.hashCode()
            CleanableItem(
                id = id,
                name = info.first,
                cleanDescription = info.second,
                spaceInfo = cleanable.spaceInfo(),
                onClean = { clean(id, cleanable) },
            )
        }
        _filesUiState.update {
            it.copy(cleanableItems = items)
        }
    }

    private fun clean(
        id: Int,
        cleanable: Cleanable
    ) = viewModelScope.launch(bgDispatcher) {
        cleanable.clean()
        val spaceInfo = cleanable.spaceInfo()
        val items = filesUiState.value.cleanableItems.toMutableList()
        items.updateIf(
            predicate = { it.id == id },
            update = { it.copy(spaceInfo = spaceInfo) },
        )
        _filesUiState.update { it.copy(cleanableItems = items) }
    }

    fun loadBackupItemsToExport() = viewModelScope.launch(bgDispatcher) {
        _backupUiState.update {
            it.copy(
                items = emptyList(),
                isExporting = false,
                isExported = false,
            )
        }
        val types = AppDataType.values()
        val counts = backupManager.exportableCounts(types = types.toList())
        val items = AppDataType.values().mapIndexed { index, type ->
            val id = type.ordinal
            BackupUiItem(
                id = id,
                type = type,
                typeName = getAppDataTypeName(type),
                select = { selectBackupItem(id) },
                unselect = { unselectBackupItem(id) },
                count = counts[index],
            )
        }
        _backupUiState.update { it.copy(items = items) }
    }

    fun exportSelectedBackupItems(backupFile: File) = viewModelScope.launch(bgDispatcher) {
        _backupUiState.update { it.copy(isExporting = true) }

        val selectedItems = _backupUiState.value.items.filter { it.isSelected }
        val selectedTypes = selectedItems.map { it.type }
        val results = try {
            backupManager.export(types = selectedTypes, output = backupFile)
        } catch (e: Exception) {
            e.printStackTrace()
            // TODO: Display an Ui message
            return@launch
        }

        val uiItems = _backupUiState.value.items.toMutableList()
        for ((index, item) in uiItems.withIndex()) {
            val resultIdx = selectedItems.indexOf(item)
            if (resultIdx != -1) {
                uiItems[index] = item.copy(successCount = results[resultIdx].getOrDefault(0))
            }
        }

        _backupUiState.update {
            it.copy(
                items = uiItems,
                isExporting = false,
                isExported = true,
            )
        }
    }

    fun loadBackupItemsToImport(backupFile: File) = viewModelScope.launch(bgDispatcher) {
        _backupUiState.update {
            it.copy(
                items = emptyList(),
                isExporting = false,
                isExported = false,
                isLoadingBackup = true,
            )
        }
        try {
            val items = backupManager.open(backupFile).sortedBy { it.type.ordinal }
            val counts = backupManager.importableCounts(items)
            val uiItems = items.mapIndexed { index, item ->
                BackupUiItem(
                    id = index,
                    type = item.type,
                    typeName = getAppDataTypeName(item.type),
                    file = item.file,
                    select = { selectBackupItem(index) },
                    unselect = { unselectBackupItem(index) },
                    count = counts[index],
                )
            }
            _backupUiState.update {
                it.copy(items = uiItems, isLoadingBackup = false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _backupUiState.update {
                it.copy(isLoadingBackup = false)
            }
        }
    }

    fun importSelectedBackupItems() = viewModelScope.launch(bgDispatcher) {
        _backupUiState.update { it.copy(isImporting = true) }

        val selectedItems = _backupUiState.value.items.filter { it.isSelected && it.file != null }
        val items = selectedItems.map { BackupItem(type = it.type, file = it.file!!) }
        val results = try {
            backupManager.import(items = items)
        } catch (e: Exception) {
            e.printStackTrace()
            // TODO: Display an Ui message
            return@launch
        }

        val uiItems = _backupUiState.value.items.toMutableList()
        for ((index, item) in uiItems.withIndex()) {
            val resultIdx = selectedItems.indexOf(item)
            if (resultIdx != -1) {
                uiItems[index] = item.copy(successCount = results[resultIdx].getOrDefault(0))
            }
        }

        _backupUiState.update {
            it.copy(
                items = uiItems,
                isImporting = false,
                isImported = true,
            )
        }
    }

    private fun selectBackupItem(id: Int) {
        updateBackupItem(id) {
            it.copy(isSelected = true)
        }
    }

    private fun unselectBackupItem(id: Int) {
        updateBackupItem(id) {
            it.copy(isSelected = false)
        }
    }

    private fun getAppDataTypeName(type: AppDataType): String {
        return when (type) {
            AppDataType.Services -> strings(BaseR.string.services)
            AppDataType.Users -> strings(BaseR.string.users)
            AppDataType.Posts -> strings(BaseR.string.posts)
            AppDataType.Bookmarks -> strings(BaseR.string.bookmarks)
            AppDataType.PostContents -> strings(BaseR.string.post_contents)
        }
    }

    private fun updateBackupItem(
        id: Int,
        update: (BackupUiItem) -> BackupUiItem,
    ) {
        val items = _backupUiState.value.items.toMutableList()
        val idx = items.indexOfFirst { it.id == id }
        if (idx != -1) {
            val updated = update(items[idx])
            items[idx] = updated
            _backupUiState.update { it.copy(items = items) }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FilesAndDataViewModel(
                serviceRepository = ServiceRepository.getDefault(context),
                postDataSource = LocalPostDataSourceImpl.getDefault(context),
                postContentRepository = PostContentRepository.getDefault(context),
                bookmarkRepository = BookmarkRepository.getDefault(context),
                cleanableProvider = CleanableProviderImpl(context),
                backupManager = BackupManagerImpl(context = context),
                strings = AndroidStrings(context),
            ) as T
        }
    }
}