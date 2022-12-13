package any.ui.common.dialog

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import any.base.model.PostFolderSelectionSorting
import any.base.prefs.PreferencesStore
import any.base.prefs.postFolderSelectionSorting
import any.base.prefs.preferencesStore
import any.base.util.PathJoiner
import any.base.util.findOrAdd
import any.base.util.updateIf
import any.data.Comparators
import any.data.entity.Folder
import any.data.entity.FolderInfo
import any.data.entity.HierarchicalFolder
import any.data.repository.FolderInfoRepository
import any.data.source.post.LocalPostDataSource
import any.data.source.post.LocalPostDataSourceImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max

class PostFolderSelectionViewModel(
    private val localPostDataSource: LocalPostDataSource,
    private val folderInfoRepository: FolderInfoRepository,
    private val preferencesStore: PreferencesStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PostFolderSelectionUiState())
    val uiState: StateFlow<PostFolderSelectionUiState> = _uiState

    private val newFolders = mutableListOf<NewFolder>()

    private val allFolders = mutableListOf<HierarchicalFolder>()

    private var loadAllFolderJob: Job? = null

    init {
        _uiState.update {
            it.copy(folderSorting = preferencesStore.postFolderSelectionSorting)
        }
    }

    fun loadAllFolder() {
        loadAllFolderJob?.cancel()
        loadAllFolderJob = viewModelScope.launch(Dispatchers.Default) {
            loadAllFolderSync()

            val flattedFolders = mutableListOf<HierarchicalFolder>()
            flatFolders(dest = flattedFolders, folders = allFolders)

            _uiState.update {
                it.copy(flattedFolders = flattedFolders)
            }
        }
    }

    private suspend fun loadAllFolderSync() {
        val expandedPaths = findFolders(allFolders) { it.expanded }
            .map { it.path }
            .toHashSet()
        val folderMap = newFolders.associate { it.path to it.createdAt }
            .toMutableMap()
        val rawFolders = localPostDataSource.fetchCollectedPosts()
            .first()
            .fold(folderMap) { acc, post ->
                val folder = post.folder
                if (!folder.isNullOrEmpty()) {
                    acc[folder] = max(acc[folder] ?: 0L, post.collectedAt)
                }
                acc
            }
            .map { (path, updatedAt) ->
                HierarchicalFolder(
                    path = path,
                    expanded = expandedPaths.contains(path),
                    updatedAt = updatedAt,
                )
            }

        val folders = mutableListOf<HierarchicalFolder>()

        fun getOrPutFolder(path: String) {
            val folder = HierarchicalFolder(path)
            val segments = folder.pathSegments
            check(segments.isNotEmpty())
            var parentFolders = folders
            var currFolder: HierarchicalFolder?
            val pathJoiner = PathJoiner()
            for (segment in segments) {
                pathJoiner.add(segment)
                currFolder = parentFolders.findOrAdd(
                    predicate = { it.name == segment },
                    newEntry = {
                        val currPath = pathJoiner.join()
                        HierarchicalFolder(
                            path = currPath,
                            expanded = expandedPaths.contains(currPath)
                        )
                    },
                )
                parentFolders = currFolder.subFolders
            }
        }

        for (folder in rawFolders) {
            getOrPutFolder(folder.path)
            if (folder.pathSegments.size == 1) {
                // Keep the expanded state
                folders.updateIf(
                    predicate = { it.path == folder.path },
                    update = { it.copy(expanded = folder.expanded) },
                )
            }
        }

        folders.add(0, HierarchicalFolder.ROOT)

        allFolders.clear()
        allFolders.addAll(folders)
    }

    private fun flatFolders(
        dest: MutableList<HierarchicalFolder>,
        folders: List<HierarchicalFolder>,
        depth: Int = 0,
    ) {
        val sortedFolders = when (_uiState.value.folderSorting) {
            PostFolderSelectionSorting.ByTitle -> {
                folders.sortedWith(Comparators.hierarchicalFolderNameComparator)
            }

            PostFolderSelectionSorting.ByLastUpdated -> {
                folders.sortedByDescending { it.updatedAt }
            }
        }
        for (folder in sortedFolders) {
            dest.add(folder.copy(depth = depth))
            if (folder.expanded && folder.subFolders.isNotEmpty()) {
                flatFolders(dest = dest, folders = folder.subFolders, depth = depth + 1)
            }
        }
    }

    fun newFolder(path: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val trimmedPath = path.trim('/')
            if (newFolders.find { it.path == trimmedPath } != null ||
                allFolders.find { it.path == trimmedPath } != null ||
                trimmedPath == Folder.ROOT.path
            ) {
                return@launch
            }

            newFolders.add(
                NewFolder(
                    path = trimmedPath,
                    createdAt = System.currentTimeMillis(),
                )
            )

            folderInfoRepository.add(FolderInfo(path = trimmedPath))

            loadAllFolderSync()
            selectFolderByPath(path)
        }
    }

    fun selectFolder(folder: HierarchicalFolder) {
        _uiState.update {
            it.copy(selectedFolder = folder)
        }
    }

    private inline fun HierarchicalFolder.onEachParentPath(
        block: (String) -> Unit,
    ) {
        val pathJoiner = PathJoiner()
        val segments = pathSegments
        for (i in 0 until segments.size - 1) {
            pathJoiner.add(segments[i])
            block(pathJoiner.join())
        }
    }

    fun selectFolderByPath(path: String?) {
        val folders = allFolders
        if (folders.isEmpty()) {
            // Folders are not loaded yet
            val selectedFolder = if (!path.isNullOrEmpty()) {
                HierarchicalFolder(path = path).also {
                    // Set parent folders to the expanded state
                    it.onEachParentPath { path ->
                        val parentFolder = HierarchicalFolder(
                            path = path,
                            expanded = true,
                        )
                        allFolders.add(parentFolder)
                    }
                    // Reload all folders
                    loadAllFolder()
                }
            } else {
                HierarchicalFolder.ROOT
            }
            _uiState.update {
                it.copy(selectedFolder = selectedFolder)
            }
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            if (!path.isNullOrEmpty()) {
                val target = findFolder(folders = allFolders) { it.path == path }
                    ?: HierarchicalFolder(path = path)
                // Expand parent folders
                var parentFolders = allFolders
                val pathSegments = target.pathSegments
                val pathJoiner = PathJoiner()
                for (i in 0 until pathSegments.lastIndex) {
                    pathJoiner.add(pathSegments[i])
                    val parentPath = pathJoiner.join()
                    val parent = findAndUpdateFolder(
                        folders = parentFolders,
                        predicate = { it.path == parentPath },
                        update = {
                            it.copy(expanded = true)
                        },
                    )
                    if (parent != null) {
                        parentFolders = parent.subFolders
                    } else {
                        break
                    }
                }

                val flattedFolders = mutableListOf<HierarchicalFolder>()
                flatFolders(dest = flattedFolders, folders = allFolders)

                _uiState.update {
                    it.copy(
                        flattedFolders = flattedFolders,
                        selectedFolder = target,
                    )
                }
            } else {
                _uiState.update {
                    it.copy(selectedFolder = HierarchicalFolder.ROOT)
                }
            }
        }
    }

    fun toggleFolder(folder: HierarchicalFolder) {
        val folders = allFolders
        val updatedFolder = findAndUpdateFolder(
            folders = folders,
            predicate = { it.path == folder.path },
            update = { it.copy(expanded = !it.expanded) },
        )
        if (updatedFolder != null) {
            val flattedFolders = mutableListOf<HierarchicalFolder>()
            flatFolders(dest = flattedFolders, folders = folders)
            _uiState.update {
                it.copy(flattedFolders = flattedFolders, selectedFolder = updatedFolder)
            }
        }
    }

    private fun findFolder(
        folders: List<HierarchicalFolder>,
        predicate: (HierarchicalFolder) -> Boolean,
    ): HierarchicalFolder? {
        for (folder in folders) {
            if (predicate(folder)) {
                return folder
            }
            if (folder.subFolders.isNotEmpty()) {
                val target = findFolder(folder.subFolders, predicate)
                if (target != null) {
                    return target
                }
            }
        }
        return null
    }

    private fun findFolders(
        folders: List<HierarchicalFolder>,
        predicate: (HierarchicalFolder) -> Boolean,
    ): List<HierarchicalFolder> {
        val matched = mutableListOf<HierarchicalFolder>()
        for (folder in folders) {
            if (predicate(folder)) {
                matched.add(folder)
            }
            val subFolders = folder.subFolders
            if (subFolders.isNotEmpty()) {
                matched.addAll(findFolders(folders = subFolders, predicate = predicate))
            }
        }
        return matched
    }

    private fun findAndUpdateFolder(
        folders: MutableList<HierarchicalFolder>,
        predicate: (HierarchicalFolder) -> Boolean,
        update: (HierarchicalFolder) -> HierarchicalFolder,
    ): HierarchicalFolder? {
        for (i in folders.indices) {
            val folder = folders[i]
            val subFolders = folder.subFolders
            if (predicate(folder)) {
                folders[i] = update(folder)
                return folders[i]
            }
            if (folder.subFolders.isNotEmpty()) {
                val updated = findAndUpdateFolder(subFolders, predicate, update)
                if (updated != null) {
                    return updated
                }
            }
            folders[i] = folder.copy(subFolders = subFolders)
        }
        return null
    }

    fun setFolderSorting(sorting: PostFolderSelectionSorting) {
        _uiState.update {
            it.copy(folderSorting = sorting)
        }
        val flattedFolders = mutableListOf<HierarchicalFolder>()
        flatFolders(dest = flattedFolders, folders = allFolders)
        _uiState.update {
            it.copy(flattedFolders = flattedFolders)
        }
        preferencesStore.postFolderSelectionSorting = sorting
    }

    fun reset() {
        newFolders.clear()
        allFolders.clear()
        _uiState.update {
            it.copy(
                flattedFolders = emptyList(),
                selectedFolder = HierarchicalFolder.ROOT,
            )
        }
    }

    private data class NewFolder(
        val path: String,
        val createdAt: Long,
    )

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PostFolderSelectionViewModel(
                localPostDataSource = LocalPostDataSourceImpl.getDefault(context),
                folderInfoRepository = FolderInfoRepository.getDefault(context),
                preferencesStore = context.preferencesStore(),
            ) as T
        }
    }
}
