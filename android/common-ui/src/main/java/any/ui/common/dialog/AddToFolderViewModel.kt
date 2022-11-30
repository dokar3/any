package any.ui.common.dialog

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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

class AddToFolderViewModel(
    private val localPostDataSource: LocalPostDataSource,
    private val folderInfoRepository: FolderInfoRepository,
) : ViewModel() {
    private val _addToFolderUiState = MutableStateFlow(AddToFolderUiState())
    val addToFolderUiState: StateFlow<AddToFolderUiState> = _addToFolderUiState

    private val newFolders = mutableListOf<String>()

    private val allFolders = mutableListOf<HierarchicalFolder>()

    private var loadAllFolderJob: Job? = null

    fun loadAllFolder() {
        loadAllFolderJob?.cancel()
        loadAllFolderJob = viewModelScope.launch(Dispatchers.Default) {
            loadAllFolderSync()

            val flattedFolders = mutableListOf<HierarchicalFolder>()
            flatFolders(dest = flattedFolders, folders = allFolders)

            _addToFolderUiState.update {
                it.copy(flattedFolders = flattedFolders)
            }
        }
    }

    private suspend fun loadAllFolderSync() {
        val expandedPaths = findFolders(allFolders) { it.expanded }
            .map { it.path }
            .toHashSet()
        val rawFolders = localPostDataSource.fetchCollectedPosts()
            .first()
            .fold(newFolders.toMutableSet()) { acc, post ->
                val folder = post.folder
                if (!folder.isNullOrEmpty()) {
                    acc.add(folder)
                }
                acc
            }
            .map {
                HierarchicalFolder(path = it, expanded = expandedPaths.contains(it))
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
                // Keep expanded state
                folders.updateIf(
                    predicate = { it.path == folder.path },
                    update = { it.copy(expanded = folder.expanded) },
                )
            }
        }

        folders.add(0, HierarchicalFolder.ROOT)
        folders.sortWith(Comparators.hierarchicalFolderNameComparator)

        allFolders.clear()
        allFolders.addAll(folders)
    }

    private fun flatFolders(
        dest: MutableList<HierarchicalFolder>,
        folders: List<HierarchicalFolder>,
        depth: Int = 0,
    ) {
        for (folder in folders) {
            dest.add(folder.copy(depth = depth))
            if (folder.expanded && folder.subFolders.isNotEmpty()) {
                flatFolders(dest = dest, folders = folder.subFolders, depth = depth + 1)
            }
        }
    }

    fun newFolder(path: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val trimmedPath = path.trim('/')
            if (newFolders.find { it == trimmedPath } != null ||
                allFolders.find { it.path == trimmedPath } != null ||
                trimmedPath == Folder.ROOT.path
            ) {
                return@launch
            }

            newFolders.add(trimmedPath)

            folderInfoRepository.add(FolderInfo(path = trimmedPath))

            loadAllFolderSync()
            selectFolderByPath(path)
        }
    }

    fun selectFolder(folder: HierarchicalFolder) {
        _addToFolderUiState.update {
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
            _addToFolderUiState.update {
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

                _addToFolderUiState.update {
                    it.copy(
                        flattedFolders = flattedFolders,
                        selectedFolder = target,
                    )
                }
            } else {
                _addToFolderUiState.update {
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
            _addToFolderUiState.update {
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

    fun reset() {
        newFolders.clear()
        allFolders.clear()
        _addToFolderUiState.update { AddToFolderUiState() }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddToFolderViewModel(
                localPostDataSource = LocalPostDataSourceImpl.getDefault(context),
                folderInfoRepository = FolderInfoRepository.getDefault(context),
            ) as T
        }
    }
}
