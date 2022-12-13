package any.home.test

import androidx.compose.ui.text.input.TextFieldValue
import any.base.log.Logger
import any.base.log.NoOpLogger
import any.base.model.PostSorting
import any.base.prefs.postSorting
import any.base.testing.preference.TestPreferencesStore
import any.data.entity.Folder
import any.data.testing.FakeData
import any.data.testing.repository.createTestFolderInfoRepository
import any.data.testing.repository.createTestPostRepository
import any.data.testing.repository.createTestServiceRepository
import any.data.testing.source.post.TestLocalPostDataSource
import any.data.testing.source.post.TestServiceBridge
import any.ui.home.collections.viewmodel.CollectionsViewModel
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private val testLocalPostSource = TestLocalPostDataSource()
    private val testServiceBridge = TestServiceBridge()
    private val serviceRepository = createTestServiceRepository()
    private val postRepository = createTestPostRepository(
        ioDispatcher = testDispatcher,
        localSource = testLocalPostSource,
        serviceBridge = testServiceBridge,
    )
    private val testFolderInfoRepository = createTestFolderInfoRepository()
    private val testPreferencesStore = TestPreferencesStore()

    private lateinit var viewModel: CollectionsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Disable android logs
        Logger.logger = NoOpLogger

        viewModel = CollectionsViewModel(
            serviceRepository = serviceRepository,
            postRepository = postRepository,
            folderInfoRepository = testFolderInfoRepository,
            preferencesStore = testPreferencesStore,
            workerDispatcher = testDispatcher,
        )

        testPreferencesStore.postSorting = PostSorting.ByAddTime
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadFolder() = runTest {
        val folders = listOf(
            "",
            "",
            "news",
            "news/movies",
            "news/movies/sci-fi",
            "news/movies/sci-fi/80s",
            "news/movies/sci-fi/80s",
            "news/movies/sci-fi",
            "news/movies",
            "article",
            "article/tech",
        )

        val tagList = listOf("read later", "cool")
        val tags = tagList + List(folders.size - tagList.size) { tagList.random() }

        val posts = FakeData.generatePosts(serviceId = "", count = 10)

        val collectedPosts = FakeData.generatePosts(count = folders.size) { index, post ->
            post.copy(
                collectedAt = System.currentTimeMillis(),
                folder = folders[index],
                tags = listOf(tags[index]),
            )
        }

        testLocalPostSource.setPosts(posts + collectedPosts)

        testPreferencesStore.postSorting = PostSorting.ByTitle

        // Load the root folder
        viewModel.loadCollectedPosts(folder = Folder.ROOT)
        advanceUntilIdle()
        viewModel.collectionsUiState.test {
            val listUiState = awaitItem().currentFolderUiState

            assertEquals(
                listOf("article", "news"),
                listUiState.folders.map { it.path },
            )

            assertEquals(
                listOf("All", *tagList.sorted().toTypedArray()),
                listUiState.tags.map { it.name }
            )

            assertEquals(
                collectedPosts.filter { it.folder == null || it.folder == "" },
                listUiState.posts.map { it.raw }
            )
        }

        // Load a sub-folder
        val folder = Folder(path = "news/movies/sci-fi")
        viewModel.loadCollectedPosts(folder = folder)
        advanceUntilIdle()
        viewModel.collectionsUiState.test {
            val listUiState = awaitItem().currentFolderUiState

            assertEquals(
                listOf("80s"),
                listUiState.folders.map { it.name },
            )

            assertEquals(
                collectedPosts.filter { it.folder == null || it.folder == folder.path },
                listUiState.posts.map { it.raw }
            )
        }
    }

    @Test
    fun setPostListSorting() = runTest {
        val folders = listOf(
            "", "", "", "",
            "A", "A", "A", "A",
            "B", "B", "B", "B",
            "C", "C", "C", "C",
        )
        val now = System.currentTimeMillis()
        val collectTimes = LongArray(folders.size) { now + it }
        val lastReadTimes = longArrayOf(
            0, now, now - 1, 0,
            0, now, 0, 0,
            0, now + 1, 0, 0,
            0, 0, 0, 0,
        )
        val posts = FakeData.generatePosts(count = folders.size) { index, post ->
            post.copy(
                title = index.toString(),
                folder = folders[index],
                collectedAt = collectTimes[index],
                lastReadAt = lastReadTimes[index],
            )
        }
        testLocalPostSource.setPosts(posts)

        // By title
        testPreferencesStore.postSorting = PostSorting.ByTitle
        viewModel.loadCollectedPosts(folder = Folder.ROOT)
        advanceUntilIdle()
        viewModel.collectionsUiState.test {
            val listUiState = awaitItem().currentFolderUiState

            assertEquals(
                listOf("0", "1", "2", "3"),
                listUiState.posts.map { it.raw.title },
            )

            assertEquals(
                listOf("A", "B", "C"),
                listUiState.folders.map { it.path },
            )
        }

        // By add time
        testPreferencesStore.postSorting = PostSorting.ByAddTime
        viewModel.loadCollectedPosts(folder = Folder.ROOT)
        advanceUntilIdle()
        viewModel.collectionsUiState.test {
            val listUiState = awaitItem().currentFolderUiState

            assertEquals(
                listOf("3", "2", "1", "0"),
                listUiState.posts.map { it.raw.title },
            )

            assertEquals(
                listOf("C", "B", "A"),
                listUiState.folders.map { it.path },
            )
        }

        // By recent browsing
        testPreferencesStore.postSorting = PostSorting.ByRecentBrowsing
        viewModel.loadCollectedPosts(folder = Folder.ROOT)
        advanceUntilIdle()
        viewModel.collectionsUiState.test {
            val listUiState = awaitItem().currentFolderUiState

            assertEquals(
                listOf("1", "2", "3", "0"),
                listUiState.posts.map { it.raw.title },
            )

            assertEquals(
                listOf("B", "A", "C"),
                listUiState.folders.map { it.path },
            )
        }
    }

    @Test
    fun filterPosts() = runTest {
        val folders = listOf(
            "", "", "", "",
            "A", "A", "A", "A",
            "B", "B", "B", "B",
            "C", "C", "C", "C",
        )
        val titles = listOf(
            "Never", "gonna", "give you", "up",
            "Never", "gonna", "let you", "down",
            "Never", "gonna", "make you", "cry",
            "Never", "gonna", "say", "goodbye",
        )
        val posts = FakeData.generatePosts(count = folders.size) { index, post ->
            post.copy(
                title = titles[index],
                folder = folders[index],
                collectedAt = System.currentTimeMillis() + index,
            )
        }
        testLocalPostSource.setPosts(posts)

        viewModel.loadCollectedPosts(folder = Folder.ROOT)
        advanceUntilIdle()

        viewModel.updateSearchFilter(TextFieldValue("Never"))
        advanceUntilIdle()
        viewModel.collectionsUiState.test {
            val uiState = awaitItem()

            assertEquals(TextFieldValue("Never"), uiState.filterText)

            val listUiState = uiState.currentFolderUiState

            assertEquals(
                listOf("Never"),
                listUiState.posts.map { it.raw.title },
            )

            assertEquals(
                listOf("C", "B", "A"),
                listUiState.folders.map { it.path },
            )
        }

        viewModel.updateSearchFilter(TextFieldValue("You"))
        advanceUntilIdle()
        viewModel.collectionsUiState.test {
            val uiState = awaitItem()

            assertEquals(TextFieldValue("You"), uiState.filterText)

            val listUiState = uiState.currentFolderUiState

            assertEquals(
                listOf("give you"),
                listUiState.posts.map { it.raw.title },
            )

            assertEquals(
                listOf("B", "A"),
                listUiState.folders.map { it.path },
            )
        }
    }

    @Test
    fun renameFolder() = runTest {
        val folders = listOf(
            "A", "A", "A", "A",
            "B", "B", "B", "B",
            "C", "C", "C", "C",
        )
        val posts = FakeData.generatePosts(count = folders.size) { index, post ->
            post.copy(
                folder = folders[index],
                collectedAt = System.currentTimeMillis() + index,
            )
        }
        testLocalPostSource.setPosts(posts)

        viewModel.loadCollectedPosts(folder = Folder.ROOT)
        advanceUntilIdle()

        val folder = viewModel.collectionsUiState.value.currentFolderUiState.folders.find {
            it.path == "A"
        }
        checkNotNull(folder)
        viewModel.renameFolder(folder, "Renamed")
        advanceUntilIdle()

        viewModel.collectionsUiState.test {
            val listUiState = awaitItem().currentFolderUiState

            assertEquals(
                listOf("C", "B", "Renamed"),
                listUiState.folders.map { it.path }
            )
        }
    }

    @Test
    fun unfoldFolder() = runTest {
        val folders = listOf(
            "A", "A", "A", "A",
            "B", "B", "B", "B",
            "C", "C", "C", "C",
        )
        val posts = FakeData.generatePosts(count = folders.size) { index, post ->
            post.copy(
                title = index.toString(),
                folder = folders[index],
                collectedAt = System.currentTimeMillis() + index,
            )
        }
        testLocalPostSource.setPosts(posts)

        viewModel.loadCollectedPosts(folder = Folder.ROOT)
        advanceUntilIdle()

        val folder = viewModel.collectionsUiState.value.currentFolderUiState.folders.find {
            it.path == "A"
        }
        checkNotNull(folder)
        viewModel.unfoldFolder(folder)
        advanceUntilIdle()

        viewModel.collectionsUiState.test {
            assertEquals(
                listOf("3", "2", "1", "0"),
                awaitItem().currentFolderUiState.posts.map { it.raw.title },
            )
        }
    }
}