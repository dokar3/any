package any.ui.home.collections

import android.content.Context
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import any.base.model.FolderViewType
import any.base.model.PostSorting
import any.base.prefs.PreferencesStore
import any.base.prefs.defaultFolderViewType
import any.base.prefs.forcedFolderViewType
import any.base.prefs.postSorting
import any.base.prefs.postSortingFlow
import any.base.prefs.preferencesStore
import any.base.util.PathJoiner
import any.base.util.joinToPath
import any.data.Comparators
import any.data.ThumbAspectRatio
import any.data.entity.Folder
import any.data.entity.FolderInfo
import any.data.entity.Post
import any.data.repository.FolderInfoRepository
import any.data.repository.PostRepository
import any.data.repository.ServiceRepository
import any.domain.entity.UiPost
import any.domain.post.containsRaw
import any.domain.post.toUiPost
import any.richtext.html.DefaultHtmlParser
import any.richtext.html.HtmlParser
import any.ui.common.BasePostViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CollectionsViewModel(
    private val serviceRepository: ServiceRepository,
    postRepository: PostRepository,
    private val folderInfoRepository: FolderInfoRepository,
    private val preferencesStore: PreferencesStore,
    htmlParser: HtmlParser = DefaultHtmlParser(),
    workerDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BasePostViewModel(
    postRepository = postRepository,
    htmlParser = htmlParser,
    workerDispatcher = workerDispatcher,
) {
    private val _collectionsUiState = MutableStateFlow(CollectionsUiState())
    val collectionsUiState: StateFlow<CollectionsUiState> = _collectionsUiState

    private var allCollectedPosts: List<Post>? = null

    private val _currFolder: Folder
        get() = _collectionsUiState.value.currentFolderUiState.folder

    private val _currFolders: List<Folder>
        get() = _collectionsUiState.value.currentFolderUiState.folders

    private val _currPosts: List<FolderPost>
        get() = _collectionsUiState.value.currentFolderUiState.posts

    private val _currTags: List<SelectableTag>
        get() = _collectionsUiState.value.currentFolderUiState.tags

    private var loadFolderJob: Job? = null

    init {
        viewModelScope.launch(workerDispatcher) {
            preferencesStore.postSortingFlow().collect { sorting ->
                _collectionsUiState.update {
                    it.copy(sorting = sorting)
                }
            }
        }
    }

    /**
     * Load collected posts for next folder
     */
    fun loadPostsForNextFolder(
        folder: Folder
    ) = viewModelScope.launch(workerDispatcher) {
        _collectionsUiState.update {
            it.copy(currentFolderUiState = it.currentFolderUiState.copy(isLoading = true))
        }
        val folderUiState = loadFolderPosts(folder)
        _collectionsUiState.update {
            it.copy(
                previousFolderUiState = it.currentFolderUiState.copy(isLoading = false),
                currentFolderUiState = folderUiState,
            )
        }
    }

    /**
     * Load collectd posts
     */
    fun loadCollectedPosts(
        folder: Folder = _currFolder
    ) {
        loadFolderJob?.cancel()
        loadFolderJob = viewModelScope.launch(workerDispatcher) {
            _collectionsUiState.update {
                it.copy(currentFolderUiState = it.currentFolderUiState.copy(isLoading = true))
            }
            val folderUiState = loadFolderPosts(folder)
            _collectionsUiState.update {
                it.copy(currentFolderUiState = folderUiState)
            }
        }
    }

    private suspend fun loadFolderPosts(
        folder: Folder
    ): FolderUiState = withContext(workerDispatcher) {
        postRepository.loadCollectedPosts()
            .onStart {
                _collectionsUiState.update {
                    it.copy(isLoading = true)
                }
                if (_currPosts.isNotEmpty() &&
                    preferencesStore.postSorting == PostSorting.ByRecentBrowsing
                ) {
                    // Make sure lastReadAt has been updated
                    // after exiting from post content screen
                    delay(30)
                }
            }
            .onEach { posts -> allCollectedPosts = posts }
            .map { posts ->
                val queryFilteredPosts = posts.filter { post ->
                    filterPostByQuery(post, _collectionsUiState.value.filterText.text)
                }

                val groupedResult = groupPostsByFolder(queryFilteredPosts, folder)

                val groupedPosts = groupedResult.posts.toMutableList()
                groupedResult.folders.forEach { folder ->
                    groupedPosts.addAll(folder.posts ?: emptyList())
                }

                val tags = updateTagsFromPosts(tags = _currTags, posts = groupedPosts)

                val filteredFolders = groupedResult.folders
                    .filter { !it.posts.isNullOrEmpty() }
                    .map {
                        val filteredFolderPosts = filterPostsByTags(
                            posts = it.posts!!,
                            tags = tags,
                        )
                        it.copy(posts = filteredFolderPosts)
                    }
                    .filter { !it.posts.isNullOrEmpty() }

                val tagsFilteredPosts = filterPostsByTags(groupedResult.posts, tags)

                val viewType = preferencesStore.forcedFolderViewType
                    ?: folderInfoRepository.get(path = folder.path)?.viewType
                    ?: preferencesStore.defaultFolderViewType

                FolderUiState(
                    folder = folder,
                    viewType = viewType,
                    tags = tags,
                    folders = filteredFolders,
                    posts = tagsFilteredPosts.toFolderPosts(),
                )
            }
            .first()
    }

    /**
     * Update tags
     */
    fun updateTags(
        tags: List<SelectableTag>?
    ) = viewModelScope.launch(workerDispatcher) {
        if (tags == null) {
            return@launch
        }
        val posts = allCollectedPosts?.filter {
            filterPostByQuery(it, _collectionsUiState.value.filterText.text)
        } ?: return@launch
        val filteredPosts = filterPostsByTags(
            posts = posts,
            tags = tags,
        )
        val groupingResult = groupPostsByFolder(filteredPosts, _currFolder)
        _collectionsUiState.update {
            val folderUiState = it.currentFolderUiState.copy(
                folders = groupingResult.folders,
                posts = groupingResult.posts.toFolderPosts(),
                tags = tags.toList(),
            )
            it.copy(currentFolderUiState = folderUiState)
        }
    }

    /**
     * Remove tag from current collected posts
     */
    fun removeTagFromCurrentPosts(
        tag: SelectableTag
    ) = viewModelScope.launch(workerDispatcher) {
        val folderPosts = _currFolders.fold(mutableListOf<Post>()) { acc, folder ->
            folder.posts?.let { acc.addAll(it) }
            acc
        }
        val posts = folderPosts + _currPosts.map { it.raw }
        val postsToUpdate = posts.filter {
            it.tags?.contains(tag.name) == true
        }.toMutableList()
        if (postsToUpdate.isEmpty()) {
            return@launch
        }
        val updatedPosts = postsToUpdate.map {
            val tags = it.tags?.toMutableList()!!.apply {
                remove(tag.name)
            }
            it.copy(tags = tags)
        }
        // Update
        postRepository.updatePosts(updatedPosts)
        // Reload
        loadCollectedPosts(_currFolder)
    }

    /**
     * Filter posts by tags
     *
     * @param posts Post list
     * @param tags Tag list
     *
     * @return Filtered post list
     */
    private fun filterPostsByTags(
        posts: List<Post>, tags: List<SelectableTag>?
    ): List<Post> {
        return if (tags.isNullOrEmpty() ||
            (tags[0].isAll && tags[0].isSelected) ||
            (tags.count { it.isSelected } == 0)
        ) {
            // 'All' is selected, return the original posts
            posts
        } else {
            // tags to hash set
            val enabledTags = tags.filter { it.isSelected }.map { it.name }.toHashSet()
            // filter by tags
            posts.filter { post ->
                val postTags = post.tags
                if (!postTags.isNullOrEmpty()) {
                    postTags.any { tag -> enabledTags.contains(tag) }
                } else {
                    false
                }
            }
        }
    }

    /**
     * Update tags from posts
     *
     * @param tags Current tag list
     * @param posts Post list
     *
     * @return New tag list
     */
    private fun updateTagsFromPosts(
        tags: List<SelectableTag>?,
        posts: List<Post>,
    ): List<SelectableTag> {
        // <name, count> map
        val collectedTags = HashMap<String, Int>()
        // add all tags
        posts.forEach { post ->
            post.tags?.forEach { tag ->
                if (collectedTags.containsKey(tag)) {
                    collectedTags[tag] = collectedTags[tag]!! + 1
                } else {
                    collectedTags[tag] = 1
                }
            }
        }

        val currTags = tags ?: emptyList()
        val currTagsMap = currTags.associateBy { it.name }
        val tagAll = tags?.find { it.isAll } ?: SelectableTag(name = "All", isAll = true)
        var hasSelectedTags = false
        // Sort
        return collectedTags.toList()
            .sortedBy { pair -> pair.first }
            .map { pair ->
                val selected = currTagsMap[pair.first]?.isSelected == true
                hasSelectedTags = hasSelectedTags or selected
                SelectableTag(
                    name = pair.first,
                    count = pair.second,
                    isSelected = selected,
                )
            }
            .let {
                it.toMutableList().apply {
                    val all = if (!hasSelectedTags) {
                        tagAll.copy(count = posts.size, isSelected = true)
                    } else {
                        tagAll.copy(count = posts.size)
                    }
                    add(0, all)
                }
            }
    }

    /**
     * Update the collections filter
     */
    fun updateSearchFilter(
        filterText: TextFieldValue
    ) = viewModelScope.launch(workerDispatcher) {
        if (filterText == _collectionsUiState.value.filterText) {
            return@launch
        }
        _collectionsUiState.update {
            it.copy(filterText = filterText)
        }
        val allPosts = allCollectedPosts ?: return@launch
        val filteredPosts = if (filterText.text.isNotEmpty()) {
            // Filter posts
            allPosts.filter { filterPostByQuery(it, filterText.text) }
        } else {
            allCollectedPosts
        }
        // Group
        val groupingResult = groupPostsByFolder(filteredPosts, _currFolder)
        // Update tags
        val posts = groupingResult.posts.toMutableList()
        groupingResult.folders.forEach { folder ->
            posts.addAll(folder.posts ?: emptyList())
        }
        val tags = updateTagsFromPosts(
            tags = _currTags,
            posts = posts,
        )
        // Update ui state
        _collectionsUiState.update {
            val folderUiState = it.currentFolderUiState.copy(
                folders = groupingResult.folders,
                posts = groupingResult.posts.toFolderPosts(),
                tags = tags,
            )
            it.copy(currentFolderUiState = folderUiState)
        }
    }

    private fun filterPostByQuery(post: Post, keyword: String): Boolean {
        if (keyword.isEmpty()) {
            return true
        }
        if (post.title.contains(keyword, ignoreCase = true)) {
            return true
        }
        if (post.author?.contains(keyword, ignoreCase = true) == true) {
            return true
        }
        if (post.category?.contains(keyword, ignoreCase = true) == true) {
            return true
        }
        if (post.serviceId.contains(keyword, ignoreCase = true)) {
            return true
        }
        val tags = post.tags
        if (tags.isNullOrEmpty()) {
            return false
        }
        if (tags.indexOfFirst { it.lowercase().contains(keyword, ignoreCase = true) } != -1) {
            return true
        }
        return false
    }

    private fun groupPostsByFolder(
        posts: List<Post>?,
        folder: Folder,
    ): GroupedResult {
        if (posts.isNullOrEmpty()) {
            return GroupedResult(emptyList(), emptyList())
        }
        val folders = mutableListOf<Folder>()
        val currFolderPosts = mutableListOf<Post>()

        val groups = if (!folder.isRoot()) {
            posts.filter {
                val f = it.folder
                if (f != null) {
                    folder.isTheSameOrSubFolder(Folder(f))
                } else {
                    false
                }
            }
        } else {
            posts
        }.groupBy {
            it.folder
        }

        for (group in groups) {
            val name = group.key ?: Folder.ROOT.path
            if (name == folder.path) {
                currFolderPosts.addAll(group.value)
                continue
            }
            // Merge posts in all sub-folders to the parent folder. E.g. All posts in folder
            // 'dir' and 'dir/sub' will be merged to folder 'dir'.
            val parentName = name.removePrefix(folder.path).trim('/').split("/").first()
            val parentIndex = folders.indexOfFirst { it.name == parentName }
            if (parentIndex != -1) {
                val parent = folders[parentIndex]
                val parentPosts = parent.posts ?: emptyList()
                folders[parentIndex] = parent.copy(posts = parentPosts + group.value)
            } else {
                val folderPath = PathJoiner(folder.path, parentName).join()
                folders.add(Folder(path = folderPath, posts = group.value))
            }
        }

        // Sort
        when (preferencesStore.postSorting) {
            PostSorting.ByAddTime -> {
                for (i in folders.indices) {
                    folders[i] = folders[i].copy(posts = folders[i].posts?.sortedByDescending {
                        it.collectedAt.toString() + it.title
                    })
                }
                currFolderPosts.sortByDescending {
                    it.collectedAt.toString() + it.title
                }
                folders.sortByDescending { f ->
                    val t = f.posts?.maxOf { it.collectedAt } ?: 0
                    t.toString() + f.name
                }
            }

            PostSorting.ByRecentBrowsing -> {
                for (i in folders.indices) {
                    folders[i] = folders[i].copy(posts = folders[i].posts?.sortedByDescending {
                        it.lastReadAt.toString() + it.title
                    })
                }
                currFolderPosts.sortByDescending {
                    it.lastReadAt.toString() + it.title
                }
                folders.sortByDescending {
                    val t = it.posts?.firstOrNull()?.lastReadAt ?: 0
                    t.toString() + it.name
                }
            }

            PostSorting.ByTitle -> {
                for (i in folders.indices) {
                    folders[i] = folders[i].copy(
                        posts = folders[i].posts?.sortedWith(Comparators.postTitleComparator),
                    )
                }
                currFolderPosts.sortWith(Comparators.postTitleComparator)
                folders.sortWith(Comparators.folderNameComparator)
            }
        }

        return GroupedResult(
            folders = folders,
            posts = currFolderPosts,
        )
    }

    fun setSorting(sorting: PostSorting) {
        if (_collectionsUiState.value.sorting == sorting) {
            return
        }
        viewModelScope.launch(workerDispatcher) {
            preferencesStore.postSorting = sorting
            loadCollectedPosts(_currFolder)
        }
    }

    fun setFolderViewType(
        folder: Folder,
        viewType: FolderViewType,
        applyToAllFolders: Boolean,
    ) {
        viewModelScope.launch(workerDispatcher) {
            if (!applyToAllFolders) {
                preferencesStore.forcedFolderViewType = null
                val info = folderInfoRepository.get(path = folder.path)
                if (info != null) {
                    folderInfoRepository.update(info.copy(viewType = viewType))
                } else {
                    folderInfoRepository.add(
                        FolderInfo(path = folder.path, viewType = viewType)
                    )
                }
            } else {
                preferencesStore.defaultFolderViewType = viewType
                preferencesStore.forcedFolderViewType = viewType
            }
            _collectionsUiState.update {
                it.copy(
                    currentFolderUiState = it.currentFolderUiState.copy(viewType = viewType),
                )
            }
        }
    }

    fun renameFolder(folder: Folder, newName: String) {
        if (folder.path == newName) {
            return
        }
        viewModelScope.launch(workerDispatcher) {
            val updatedPosts = folder.posts?.map {
                it.copy(folder = newName)
            }
            if (!updatedPosts.isNullOrEmpty()) {
                postRepository.updatePosts(updatedPosts)
            }

            val folderInfo = folderInfoRepository.get(path = folder.path)
            if (folderInfo != null) {
                folderInfoRepository.remove(folderInfo)
                folderInfoRepository.add(folderInfo.copy(path = newName))
            }

            loadCollectedPosts(_currFolder)
        }
    }

    /**
     * Unfold folder, posts in target folder will be moved to the current folder.
     */
    fun unfoldFolder(folder: Folder) {
        viewModelScope.launch(workerDispatcher) {
            val parentPath = folder.parentPath
            val updatedPosts = folder.posts?.map {
                it.copy(folder = parentPath)
            }
            if (updatedPosts != null) {
                postRepository.updatePosts(updatedPosts)
            }

            folderInfoRepository.remove(path = folder.path)

            loadCollectedPosts(_currFolder)
        }
    }

    fun gotoParentFolder() {
        val currFolder = _currFolder
        if (currFolder.isRoot()) {
            return
        }
        val pathSegments = currFolder.pathSegments
        val parentFolder = if (pathSegments.size > 1) {
            val parentSegments = pathSegments.subList(0, pathSegments.lastIndex)
            Folder(path = parentSegments.joinToPath())
        } else {
            Folder.ROOT
        }
        loadPostsForNextFolder(parentFolder)
    }

    fun startMultiSelection(post: UiPost) {
        _collectionsUiState.update {
            it.copy(selectedPosts = setOf(post))
        }
    }

    fun finishMultiSelection() {
        _collectionsUiState.update {
            it.copy(selectedPosts = emptySet())
        }
    }

    fun addToSelection(post: UiPost) {
        _collectionsUiState.update {
            val selectedPosts = it.selectedPosts.toMutableSet()
            selectedPosts.add(post)
            it.copy(selectedPosts = selectedPosts)
        }
    }

    fun removeFromSelection(post: UiPost) {
        _collectionsUiState.update {
            val selectedPosts = it.selectedPosts.toMutableSet()
            selectedPosts.remove(post)
            it.copy(selectedPosts = selectedPosts)
        }
    }

    fun removeSelectedFromCollections() {
        viewModelScope.launch(workerDispatcher) {
            val updatedPosts = getSelectedPosts().map {
                it.asUnCollected()
            }
            postRepository.updatePosts(updatedPosts)
            loadCollectedPosts()
            finishMultiSelection()
        }
    }

    fun addSelectedToFolder(folder: Folder) {
        viewModelScope.launch(workerDispatcher) {
            val toUpdate = getSelectedPosts().map {
                it.copy(
                    folder = folder.path,
                    collectedAt = System.currentTimeMillis(),
                )
            }
            postRepository.updatePosts(toUpdate)
            loadCollectedPosts(_currFolder)
            finishMultiSelection()
        }
    }

    private suspend fun List<Post>.toFolderPosts(): List<FolderPost> {
        val defThumbAspectRatioMap = serviceRepository.getDbServices()
            .associate { it.id to ThumbAspectRatio.parseOrNull(it.mediaAspectRatio) }
        return map {
            FolderPost(
                ui = it.toUiPost(htmlParser),
                defaultThumbAspectRatio = defThumbAspectRatioMap[it.serviceId],
            )
        }
    }

    private fun getSelectedPosts(): List<Post> {
        val selectedPosts = _collectionsUiState.value.selectedPosts
        return allCollectedPosts?.filter {
            selectedPosts.containsRaw(it)
        } ?: emptyList()
    }

    override suspend fun onPostsUpdated(posts: List<UiPost>) {
        loadCollectedPosts(_currFolder)
    }

    private data class GroupedResult(
        val folders: List<Folder>,
        val posts: List<Post>,
    )

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CollectionsViewModel(
                serviceRepository = ServiceRepository.getDefault(context),
                postRepository = PostRepository.getDefault(context),
                folderInfoRepository = FolderInfoRepository.getDefault(context),
                preferencesStore = context.preferencesStore(),
            ) as T
        }
    }
}