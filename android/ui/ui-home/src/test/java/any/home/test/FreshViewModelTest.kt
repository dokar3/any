package any.home.test

import any.base.Strings
import any.base.log.Logger
import any.base.log.StdOutLogger
import any.base.prefs.currentService
import any.base.testing.TestFileReader
import any.base.testing.preference.TestPreferencesStore
import any.data.entity.JsPageKey
import any.data.entity.JsType
import any.data.entity.Post
import any.data.testing.FakeData
import any.data.testing.markInFresh
import any.data.testing.repository.createTestPostRepository
import any.data.testing.repository.createTestServiceRepository
import any.data.testing.source.post.TestLocalPostDataSource
import any.data.testing.source.post.TestServiceBridge
import any.domain.entity.UiServiceManifest
import any.domain.service.toUiManifest
import any.testing.MainDispatcherRule
import any.ui.home.fresh.viewmodel.FreshUiState
import any.ui.home.fresh.viewmodel.FreshViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class FreshViewModelTest {
    @get: Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferencesStore = TestPreferencesStore()

    private val fileReader = TestFileReader()

    private val htmlParser = NoopHtmlParser

    private val testLocalPostSource = TestLocalPostDataSource()
    private val testServiceBridge = TestServiceBridge()
    private val postRepository = createTestPostRepository(
        ioDispatcher = mainDispatcherRule.testDispatcher,
        localSource = testLocalPostSource,
        serviceBridge = testServiceBridge,
    )

    private lateinit var viewModel: FreshViewModel

    @Before
    fun setup() {
        // Disable the android logger
        Logger.logger = StdOutLogger

        viewModel = FreshViewModel(
            serviceRepository = createTestServiceRepository(
                builtinService = emptyList(),
                localServices = FakeData.SERVICES,
            ),
            postRepository = postRepository,
            preferencesStore = preferencesStore,
            strings = Strings.None,
            fileReader = TestFileReader(),
            htmlParser = htmlParser,
            workerDispatcher = mainDispatcherRule.testDispatcher,
        )
    }

    private fun item(): FreshUiState {
        return viewModel.freshUiState.value
    }

    @Test
    fun loadServicesWhenCurrentServiceAbsent() = runTest {
        viewModel.loadServices()
        advanceUntilIdle()

        val expectedServices = FakeData.SERVICES.sortedBy { it.name }
        val item = viewModel.freshUiState.value
        assertEquals(expectedServices, item.services.map { it.raw })
        assertEquals(expectedServices.first(), item.currService?.raw)
    }

    @Test
    fun loadServicesWhenCurrentServicePresent() = runTest {
        val currentService = FakeData.SERVICES.random()
        preferencesStore.currentService.value = currentService.id

        viewModel.loadServices()
        advanceUntilIdle()

        val item = viewModel.freshUiState.value
        assertEquals(currentService, item.currService?.raw)
    }

    @Test
    fun setCurrentService() = runTest {
        viewModel.loadServices()
        advanceUntilIdle()

        val targetService = FakeData.SERVICES.random()
            .toUiManifest(fileReader, htmlParser)

        viewModel.setCurrentService(targetService)
        advanceUntilIdle()

        // Preference value should be updated
        assertEquals(targetService.id, preferencesStore.currentService.value)

        val item = viewModel.freshUiState.value
        assertEquals(targetService, item.currService)
    }

    @Test
    fun loadInitialPosts() = runTest {
        val service = FakeData.SERVICES.random().toUiManifest(fileReader, htmlParser)
        viewModel.setCurrentService(service)

        val localPosts = FakeData.generatePosts(serviceId = service.id, count = 1)
            .markInFresh()
        val remotePosts = FakeData.generatePosts(serviceId = service.id, count = 2)
            .markInFresh()

        // No local posts
        loadInitialPosts(
            service = service,
            localPosts = emptyList(),
            remotePosts = remotePosts,
            remoteOnly = false,
        )
        advanceUntilIdle()
        assertEquals(remotePosts, item().posts.map { it.raw })

        // Both local posts and remote posts are available
        loadInitialPosts(
            service = service,
            localPosts = localPosts,
            remotePosts = remotePosts,
            remoteOnly = false,
        )
        advanceUntilIdle()
        assertEquals(localPosts, item().posts.map { it.raw })

        // Both local posts and remote posts are available but remote only
        loadInitialPosts(
            service = service,
            localPosts = localPosts,
            remotePosts = remotePosts,
            remoteOnly = true,
        )
        advanceUntilIdle()
        assertEquals(remotePosts, item().posts.map { it.raw })

        // Remote posts are not available
        loadInitialPosts(
            service = service,
            localPosts = localPosts,
            remotePosts = emptyList(),
            remoteOnly = false,
        )
        advanceUntilIdle()
        assertEquals(localPosts, item().posts.map { it.raw })
    }

    private fun loadInitialPosts(
        service: UiServiceManifest,
        localPosts: List<Post>,
        remotePosts: List<Post>,
        remoteOnly: Boolean,
    ) {
        testLocalPostSource.setPosts(localPosts)
        testServiceBridge.setPosts(page = 1, posts = remotePosts)
        viewModel.fetchInitialPosts(
            service = service,
            remoteOnly = remoteOnly
        )
    }

    @Test
    fun initialPostsCaching() = runTest {
        val service = FakeData.SERVICES.random()
            .toUiManifest(fileReader, htmlParser)

        val initialPosts = FakeData.generatePosts(serviceId = service.id, count = 10)

        testLocalPostSource.setPosts(emptyList())
        testServiceBridge.setPosts(page = 1, posts = initialPosts)

        // Load initial posts
        viewModel.fetchInitialPosts(service = service)
        advanceUntilIdle()
        // Make sure there is no data comes from the remote source
        testServiceBridge.setPosts(page = 1, posts = emptyList())
        viewModel.fetchInitialPosts(service = service)
        advanceUntilIdle()

        // Initial posts should be cached
        assertEquals(initialPosts.markInFresh(), item().posts.map { it.raw })
    }

    @Test
    fun loadMorePosts() = runTest {
        val pageKeyOfPage2 = JsPageKey(value = "2", type = JsType.Number)
        val service = FakeData.SERVICES.random().copy(pageKeyOfPage2 = pageKeyOfPage2)
            .toUiManifest(fileReader, htmlParser)

        val page1Posts = FakeData.generatePosts(serviceId = service.id, count = 1)
        val page2Posts = FakeData.generatePosts(serviceId = service.id, count = 1)
        val page3Posts = FakeData.generatePosts(serviceId = service.id, count = 1)

        testLocalPostSource.setPosts(page1Posts.markInFresh())
        testServiceBridge.setPosts(page = 2, page2Posts)
        testServiceBridge.setPosts(page = 3, page3Posts)

        // Fetch initial posts
        viewModel.fetchInitialPosts(service = service)

        advanceUntilIdle()

        // Load page 2
        viewModel.fetchMorePosts(service = service)

        assertTrue(item().isLoadingMorePosts)

        advanceUntilIdle()

        assertEquals(
            page1Posts.markInFresh() + page2Posts,
            item().posts.map { it.raw },
        )

        // Load page 3
        viewModel.fetchMorePosts(service = service)
        advanceUntilIdle()

        assertEquals(
            page1Posts.markInFresh() + page2Posts + page3Posts,
            item().posts.map { it.raw },
        )
    }

    @Test
    fun remotePostFieldsUpdating() = runTest {
        val service = FakeData.SERVICES.random().toUiManifest(fileReader, htmlParser)
        viewModel.setCurrentService(service)

        val posts = FakeData.generatePosts(serviceId = service.id, count = 5)
        val localPosts = posts
            .markInFresh()
            .map {
                it.copy(
                    collectedAt = Random.nextLong(),
                    downloadAt = Random.nextLong(),
                    lastReadAt = Random.nextLong(),
                    folder = "Folder ${Random.nextInt()}",
                    readPosition = Random.nextInt(),
                )
            }
        val remotePosts = posts.map {
            it.copy(
                rating = "${it.rating} [updated]",
                title = "${it.title} [updated]",
            )
        }
        val expectedPosts = remotePosts.map { post ->
            val cached = localPosts.find {
                it.serviceId == post.serviceId && it.url == post.url
            }
            cached?.copy(
                collectedAt = cached.collectedAt,
                downloadAt = cached.downloadAt,
                lastReadAt = cached.lastReadAt,
                folder = cached.folder,
                readPosition = cached.readPosition,
            ) ?: post
        }

        testLocalPostSource.setPosts(localPosts)
        testServiceBridge.setPosts(page = 1, posts = remotePosts)

        viewModel.fetchInitialPosts(
            service = service,
            remoteOnly = true
        )

        advanceUntilIdle()

        assertEquals(expectedPosts, item().posts.map { it.raw })
    }
}