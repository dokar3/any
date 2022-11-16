package any.ui.home.fresh

import any.base.R as BaseR
import any.ui.common.R as CommonUiR
import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import any.base.ImmutableHolder
import any.base.image.ImageRequest
import any.base.prefs.forceHeaderImageForAllServices
import any.base.prefs.headerImage
import any.base.prefs.preferencesStore
import any.base.util.Intents
import any.base.util.Permissions
import any.base.util.isHttpUrl
import any.data.entity.ServiceResource
import any.data.entity.ServiceViewType
import any.domain.entity.UiPost
import any.domain.entity.UiServiceManifest
import any.navigation.NavEvent
import any.navigation.Routes
import any.navigation.navPushEvent
import any.navigation.navigateToMedia
import any.navigation.navigateToPost
import any.navigation.navigateToUser
import any.navigation.post
import any.navigation.search
import any.navigation.settings
import any.ui.comments.CommentsSheet
import any.ui.common.ServiceDropdownButton
import any.ui.common.dialog.AddToCollectionsDialog
import any.ui.common.dialog.AddToFolderDialog
import any.ui.common.lazy.LazyGridScrollableState
import any.ui.common.lazy.rememberLazyGridScrollableState
import any.ui.common.menu.PostOptionMenu
import any.ui.common.modifier.verticalScrollBar
import any.ui.common.post.LoadingItem
import any.ui.common.post.PostList
import any.ui.common.post.RetryItem
import any.ui.common.theme.statusBar
import any.ui.common.theme.themeColorOrPrimary
import any.ui.common.theme.topBarBackground
import any.ui.common.widget.EditDialog
import any.ui.common.widget.EmojiEmptyContent
import any.ui.common.widget.EndOfList
import any.ui.common.widget.QuickReturnScreen
import any.ui.common.widget.TitleActionButton
import any.ui.common.widget.UiMessagePopup
import any.ui.common.widget.rememberQuickReturnScreenState
import any.ui.home.HomeScrollToTopManager
import any.ui.home.IconTintTheme
import any.ui.home.ScrollToTopHandler
import any.ui.home.SettingsButton
import any.ui.home.TitleBar
import any.ui.home.fresh.viewmodel.FreshUiState
import any.ui.home.fresh.viewmodel.FreshViewModel
import any.ui.jslogger.FloatingLoggerService
import any.ui.service.ServiceConfiguringDialog
import com.dokar.sheets.rememberBottomSheetState
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FreshScreen(
    onNavigate: (NavEvent) -> Unit,
    titleBarHeight: Dp,
    bottomBarHeight: Dp,
    changeStatusBarColor: (Color) -> Unit,
    onBottomBarOffsetUpdate: (Int) -> Unit,
    isRunningTransitions: Boolean,
    scrollToTopManager: HomeScrollToTopManager,
    modifier: Modifier = Modifier,
    viewModel: FreshViewModel = viewModel(
        factory = FreshViewModel.Factory(LocalContext.current)
    ),
) {
    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    val uiState by viewModel.freshUiState.collectAsState()
    val services = uiState.services
    val currentService = uiState.currService

    val scrollableState = rememberLazyGridScrollableState()

    val screenState = rememberQuickReturnScreenState(lazyScrollableState = scrollableState)

    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val listPadding = remember(bottomBarHeight, bottomInset) {
        PaddingValues(bottom = bottomBarHeight + bottomInset)
    }

    var showCommandDialog by rememberSaveable(inputs = emptyArray()) {
        mutableStateOf(false)
    }

    var showConfigureServiceDialog by rememberSaveable(inputs = emptyArray()) {
        mutableStateOf(false)
    }

    var titleAlpha by remember { mutableStateOf(0f) }
    var titleBarIconTintTheme by remember { mutableStateOf(IconTintTheme.Light) }
    var titleBarBackgroundColor by remember { mutableStateOf(Color.Transparent) }

    fun selectService(service: UiServiceManifest) {
        viewModel.setCurrentService(service)
        // scroll to top
        scope.launch { scrollableState.quickScrollToTop() }
    }

    fun navigateToServiceManagement() {
        val route = Routes.settings(subSettings = Routes.Settings.SERVICE_MGT)
        onNavigate(navPushEvent(route))
    }

    LaunchedEffect(viewModel) {
        viewModel.loadServices()
    }

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.clearMessage()
            viewModel.cancelLoadings()
        }
    }

    DisposableEffect(scrollToTopManager) {
        val handler = ScrollToTopHandler {
            scope.launch {
                screenState.resetBars()
                scrollableState.quickScrollToTop()
            }
        }
        scrollToTopManager.addHandler(handler)
        onDispose {
            scrollToTopManager.removeHandler(handler)
        }
    }

    QuickReturnScreen(
        state = screenState,
        modifier = modifier,
        bottomBarHeight = listPadding.calculateBottomPadding(),
        topBar = {
            TitleBar(
                height = titleBarHeight,
                startActionButton = {
                    SettingsButton(
                        onClick = { onNavigate(navPushEvent(Routes.settings())) },
                    )
                },
                endActionButton = {
                    TitleActionButton(
                        label = stringResource(BaseR.string.search),
                        onClick = {
                            if (currentService != null) {
                                onNavigate(navPushEvent(Routes.search(serviceId = currentService.id)))
                            }
                        },
                        onLongClick = { showCommandDialog = true },
                    ) {
                        Icon(
                            painter = painterResource(CommonUiR.drawable.ic_search),
                            contentDescription = "",
                        )
                    }
                },
                iconTintTheme = titleBarIconTintTheme,
                backgroundColor = { titleBarBackgroundColor },
                dividerAlpha = { titleAlpha },
                titleAlpha = { titleAlpha },
            ) {
                ServiceDropdownButton(
                    onSelectService = ::selectService,
                    onServiceManagementClick = ::navigateToServiceManagement,
                    onLongClickCurrentService = { showConfigureServiceDialog = true },
                    services = ImmutableHolder(services),
                    currentService = currentService,
                    fontSize = 16.sp,
                    dropdownAlignmentToAnchor = Alignment.TopCenter,
                    dropdownTransformOrigin = TransformOrigin(0.5f, 0f),
                )
            }
        },
        bottomBar = { onBottomBarOffsetUpdate(it) },
    ) {
        var headerHeightPx by remember { mutableStateOf(0) }
        var firstItemOffsetY by remember { mutableStateOf(0) }

        val statusBarColor = MaterialTheme.colors.statusBar
        val topBarBackgroundColor = MaterialTheme.colors.topBarBackground
        LaunchedEffect(scrollableState, statusBarColor) {
            val statusBarAlpha = statusBarColor.alpha
            launch {
                snapshotFlow { scrollableState.visibleItemsInfo }
                    .mapNotNull { it.firstOrNull() }
                    .map { it.index == 0 }
                    .distinctUntilChanged()
                    .collect { firstItemVisible ->
                        if (!firstItemVisible) {
                            changeStatusBarColor(statusBarColor)
                            titleBarIconTintTheme = IconTintTheme.Auto
                            titleBarBackgroundColor = topBarBackgroundColor
                            titleAlpha = 1f
                        }
                    }
            }
            snapshotFlow { scrollableState.visibleItemsInfo }
                .mapNotNull { it.firstOrNull() }
                .filter { it.index == 0 }
                .map { it.offset }
                .distinctUntilChanged()
                .collect {
                    firstItemOffsetY = it
                    val progress = (-it.toFloat() / headerHeightPx).coerceIn(0f, 1f)
                    val alpha = statusBarAlpha * progress
                    changeStatusBarColor(statusBarColor.copy(alpha = alpha))
                    titleAlpha = alpha
                    titleBarIconTintTheme = if (alpha > 0.4f) {
                        IconTintTheme.Auto
                    } else {
                        IconTintTheme.Light
                    }
                    titleBarBackgroundColor = topBarBackgroundColor.copy(alpha = alpha)
                }
        }

        FreshScreenContent(
            onNavigate = onNavigate,
            onCollectPost = { viewModel.collectPost(it) },
            onDiscardPost = { viewModel.discardPost(it) },
            onAddPostToFolder = { post, folder ->
                viewModel.addToFolder(post, folder)
            },
            onFetchFirstPage = { remoteOnly ->
                if (currentService != null) {
                    viewModel.fetchInitialPosts(
                        service = currentService,
                        remoteOnly = remoteOnly
                    )
                }
            },
            onFetchMore = { force ->
                if (currentService?.isPageable == true &&
                    !uiState.isLoadingMorePosts &&
                    !uiState.requireRefreshInitialPage &&
                    (uiState.hasMore || force)
                ) {
                    viewModel.fetchMorePosts(service = currentService)
                }
            },
            onRefresh = {
                if (currentService != null) {
                    scope.launch { scrollableState.quickScrollToTop() }
                    viewModel.fetchInitialPosts(
                        service = currentService,
                        remoteOnly = true
                    )
                }
            },
            onFetchPreviousRequest = { remoteOnly ->
                if (currentService != null) {
                    viewModel.fetchPreviousRequest(
                        service = currentService,
                        remoteOnly = remoteOnly
                    )
                }
            },
            onClearMessage = { viewModel.clearMessage() },
            appBarHeight = { screenState.topBarHeight },
            appBarOffsetProvider = { screenState.topBarOffsetY },
            uiState = uiState,
            service = currentService,
            scrollableState = scrollableState,
            headerContent = {
                HeaderContent(
                    scrollProvider = { firstItemOffsetY },
                    services = ImmutableHolder(services),
                    currentService = currentService,
                    defaultHeaderIcons = ImmutableHolder(uiState.allPostMediaImages),
                    onServiceManagementClick = ::navigateToServiceManagement,
                    onSelectService = ::selectService,
                    onLongClickCurrentService = { showConfigureServiceDialog = true },
                    modifier = Modifier.onSizeChanged { headerHeightPx = it.height },
                )
            },
            listPadding = listPadding,
            isRunningTransitions = isRunningTransitions,
        )
    }

    if (showCommandDialog) {
        var text by remember { mutableStateOf("") }
        EditDialog(
            onDismissRequest = { showCommandDialog = false },
            value = text,
            onValueChange = { text = it },
            title = { Text(stringResource(BaseR.string.lets_go)) },
            label = { Text(stringResource(BaseR.string.enter_a_url_or_command)) },
            onConfirmClick = {
                if (text.isEmpty()) {
                    return@EditDialog
                }

                if (text == "jslogs") {
                    val activity = context as Activity
                    if (Permissions.checkOrRequestFloatingPermission(activity)) {
                        FloatingLoggerService.show(context)
                    }
                    showCommandDialog = false
                    return@EditDialog
                }

                if (text == "clear") {
                    viewModel.clearFreshPosts()
                    showCommandDialog = false
                    return@EditDialog
                }

                if (text.isHttpUrl()) {
                    onNavigate(navPushEvent(Routes.post(url = text.trimEnd(), serviceId = null)))
                    showCommandDialog = false
                    return@EditDialog
                }
            },
            acceptEmpty = false,
        )
    }

    if (showConfigureServiceDialog && currentService != null) {
        ServiceConfiguringDialog(
            onDismissRequest = { showConfigureServiceDialog = false },
            service = currentService,
            isUpdate = true,
        )
    }
}

@ExperimentalFoundationApi
@Composable
private fun FreshScreenContent(
    onNavigate: (NavEvent) -> Unit,
    onCollectPost: (UiPost) -> Unit,
    onDiscardPost: (UiPost) -> Unit,
    onAddPostToFolder: (post: UiPost, folder: String) -> Unit,
    onFetchFirstPage: (remoteOnly: Boolean) -> Unit,
    onFetchMore: (force: Boolean) -> Unit,
    onRefresh: () -> Unit,
    onFetchPreviousRequest: (remoteOnly: Boolean) -> Unit,
    onClearMessage: () -> Unit,
    appBarHeight: () -> Int,
    appBarOffsetProvider: () -> Float,
    uiState: FreshUiState,
    service: UiServiceManifest?,
    scrollableState: LazyGridScrollableState,
    headerContent: @Composable () -> Unit,
    listPadding: PaddingValues = PaddingValues(0.dp),
    isRunningTransitions: Boolean,
) {
    val statusBarHeight = with(LocalDensity.current) {
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding().toPx()
    }

    val posts = if (service != null) uiState.posts else emptyList()

    val isRefreshing = uiState.isLoadingInitialPosts
    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing = isRefreshing),
        swipeEnabled = service != null,
        onRefresh = { onFetchFirstPage(true) },
        indicator = { state, trigger ->
            var prevAlpha by remember { mutableStateOf(0f) }
            val alphaAnim = remember { Animatable(0f) }

            suspend fun animateAlpha() {
                alphaAnim.snapTo(prevAlpha)
                alphaAnim.animateTo(if (state.isRefreshing) 1f else 0f)
                prevAlpha = alphaAnim.value
            }

            LaunchedEffect(state.isRefreshing) {
                animateAlpha()
            }

            LaunchedEffect(state.isSwipeInProgress) {
                if (!state.isSwipeInProgress) {
                    animateAlpha()
                }
            }

            SwipeRefreshIndicator(
                state = state,
                refreshTriggerDistance = trigger,
                refreshingOffset = 8.dp,
                fade = true,
                modifier = Modifier.graphicsLayer {
                    val swipeProgress = (state.indicatorOffset / (trigger.toPx() / 2f))
                        .coerceIn(0f, 1f)
                    alpha = if (state.isSwipeInProgress) {
                        prevAlpha = swipeProgress
                        swipeProgress
                    } else {
                        if (alphaAnim.isRunning) {
                            alphaAnim.value
                        } else {
                            prevAlpha
                        }
                    }
                    val height = appBarHeight()
                    if (height >= statusBarHeight) {
                        val minOffY = height - statusBarHeight
                        val offY = appBarOffsetProvider().coerceIn(-minOffY, 0f)
                        translationY = (height + offY)
                    }
                },
            )
        },
    ) {
        FreshPostList(
            onNavigate = onNavigate,
            onCollectPost = onCollectPost,
            onDiscardPost = onDiscardPost,
            onAddPostToFolder = onAddPostToFolder,
            onFetchFirstPage = onFetchFirstPage,
            onFetchMore = onFetchMore,
            onRefresh = onRefresh,
            service = service,
            uiState = uiState,
            scrollableState = scrollableState,
            posts = ImmutableHolder(posts),
            headerContent = headerContent,
            contentPadding = listPadding,
            pageKey = ImmutableHolder(uiState.pageKey),
        )
    }

    if (!isRunningTransitions) {
        UiMessagePopup(
            message = uiState.message,
            onClearMessage = onClearMessage,
            onRetry = { onFetchPreviousRequest(true) },
            offset = DpOffset(0.dp, (-56).dp),
        )
    }
}

private enum class ItemsAnimState {
    Preparing,
    Started,
    Finished,
}

@ExperimentalFoundationApi
@Composable
private fun FreshPostList(
    onNavigate: (NavEvent) -> Unit,
    onCollectPost: (UiPost) -> Unit,
    onDiscardPost: (UiPost) -> Unit,
    onAddPostToFolder: (UiPost, String) -> Unit,
    onFetchFirstPage: (remoteOnly: Boolean) -> Unit,
    onFetchMore: (force: Boolean) -> Unit,
    onRefresh: () -> Unit,
    service: UiServiceManifest?,
    uiState: FreshUiState,
    scrollableState: LazyGridScrollableState,
    posts: ImmutableHolder<List<UiPost>>,
    headerContent: @Composable () -> Unit,
    contentPadding: PaddingValues,
    pageKey: ImmutableHolder<Any?>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    var selectedPost: UiPost? by remember { mutableStateOf(null) }

    var postToAddToFolder: UiPost? by remember { mutableStateOf(null) }

    var postToAddToCollection: UiPost? by remember { mutableStateOf(null) }

    var postToLoadComments: UiPost? by remember { mutableStateOf(null) }

    val commentsSheetState = rememberBottomSheetState()

    val itemInitialOffset = with(LocalDensity.current) { 56.dp.toPx() }

    // Item animations
    var currPosts by remember { mutableStateOf(ImmutableHolder(emptyList<UiPost>())) }
    val itemAnimations = remember {
        mutableMapOf<Int, Animatable<Float, AnimationVector1D>>()
    }
    val itemAnimationJobs = remember { mutableListOf<Job>() }
    var animationAdditionTick by remember { mutableStateOf(0) }
    var itemsAnimState by remember { mutableStateOf(ItemsAnimState.Preparing) }
    var initialItemAnimValue by remember { mutableStateOf(0f) }
    var targetItemAnimValue by remember { mutableStateOf(1f) }

    fun stopItemAnimations() {
        itemAnimationJobs.onEach { it.cancel() }
        itemAnimationJobs.clear()
        itemAnimations.clear()
    }

    fun getItemAnimation(index: Int): Animatable<Float, AnimationVector1D> {
        return itemAnimations[index] ?: Animatable(initialItemAnimValue).also {
            itemAnimations[index] = it
            animationAdditionTick++
        }
    }

    LaunchedEffect(posts) {
        if (currPosts == posts) {
            return@LaunchedEffect
        }
        if (posts.value.isEmpty()) {
            currPosts = posts
            itemsAnimState = ItemsAnimState.Preparing
            return@LaunchedEffect
        }
        if (currPosts.value.isEmpty()) {
            // Posts loaded, run intro animations
            itemsAnimState = ItemsAnimState.Started
            initialItemAnimValue = 0f
            targetItemAnimValue = 1f
            stopItemAnimations()
        }
        currPosts = posts
    }

    LaunchedEffect(scrollableState) {
        snapshotFlow { scrollableState.visibleItemsInfo }
            .mapNotNull { it.firstOrNull()?.offset }
            .distinctUntilChanged()
            .collect {
                if (itemsAnimState == ItemsAnimState.Started) {
                    itemsAnimState = ItemsAnimState.Finished
                    stopItemAnimations()
                }
            }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { animationAdditionTick }
            .collect {
                for ((idx, anim) in itemAnimations) {
                    if (anim.isRunning || anim.value == targetItemAnimValue) {
                        continue
                    }
                    val job = launch {
                        anim.animateTo(
                            targetValue = targetItemAnimValue,
                            animationSpec = tween(
                                durationMillis = 325,
                                delayMillis = idx * 20,
                            ),
                        )
                    }
                    itemAnimationJobs.add(job)
                }
            }
    }

    PostList(
        state = scrollableState.gridState,
        posts = posts,
        pageKey = pageKey,
        viewType = service?.viewType ?: ServiceViewType.List,
        defThumbAspectRatio = service?.mediaAspectRatio,
        modifier = modifier
            .semantics { contentDescription = "FreshPostList" }
            .verticalScrollBar(
                state = scrollableState,
                padding = PaddingValues(
                    top = WindowInsets.statusBars
                        .asPaddingValues()
                        .calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding(),
                ),
            ),
        onCommentsClick = {
            postToLoadComments = it
            scope.launch { commentsSheetState.peek() }
        },
        onCollectClick = {
            if (it.isCollected()) {
                onDiscardPost(it)
            } else {
                onCollectPost(it)
            }
        },
        onMoreClick = { selectedPost = it },
        onMediaClick = { post, index ->
            navigateToMedia(onNavigate, context, post.raw, index)
        },
        onUserClick = { serviceId, userId -> navigateToUser(onNavigate, serviceId, userId) },
        onLinkClick = { Intents.openInBrowser(context, it) },
        onItemClick = { navigateToPost(onNavigate, context, it.raw) },
        onItemLongClick = { selectedPost = it },
        headerContent = headerContent,
        emptyContent = {
            if (uiState.isLoading()) {
                return@PostList
            }
            if (service == null) {
                EmojiEmptyContent(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(BaseR.string.no_services))

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        stringResource(BaseR.string.please_add_or_enable_services),
                        fontSize = 16.sp,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            val route = Routes.settings(subSettings = Routes.Settings.SERVICE_MGT)
                            onNavigate(navPushEvent(route))
                        },
                    ) {
                        Text(stringResource(BaseR.string.service_management))
                    }
                }
            } else {
                EmojiEmptyContent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp),
                ) {
                    Text(stringResource(BaseR.string.no_posts))

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { onFetchFirstPage(false) },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(stringResource(BaseR.string.refresh))
                    }
                }
            }
        },
        loadMoreContent = {
            if (!uiState.hasMore && !uiState.isLoadingMorePosts) {
                EndOfList(
                    onClick = { onFetchMore(true) },
                ) {
                    Text(stringResource(BaseR.string.no_more_posts))
                }
            } else if (!uiState.isLoading() && !uiState.isSuccess) {
                RetryItem(
                    message = stringResource(BaseR.string.load_failed),
                    onClick = { onFetchMore(true) },
                )
            } else if (uiState.requireRefreshInitialPage) {
                RefreshItem(onRefreshClick = onRefresh)
            } else {
                LoadingItem()
            }
        },
        itemGraphicsLayer = { index ->
            when (itemsAnimState) {
                ItemsAnimState.Preparing -> {
                    alpha = 0f
                    translationY = itemInitialOffset
                }

                ItemsAnimState.Started -> {
                    val value = getItemAnimation(index).value
                    alpha = value
                    translationY = itemInitialOffset * (1f - value)
                }

                ItemsAnimState.Finished -> {
                    alpha = 1f
                    translationY = 0f
                }
            }
        },
        onLoadMore = { onFetchMore(false) },
        showLoadMore = service?.isPageable == true,
        isLoadingMore = uiState.isLoading(),
        contentPadding = contentPadding,
    )

    if (selectedPost != null) {
        val post = selectedPost!!
        PostOptionMenu(
            post = post,
            showMultiSelectionItem = false,
            onDiscardRequest = { onDiscardPost(post) },
            onAddToCollectionsClick = { postToAddToCollection = post },
            onDismiss = { selectedPost = null },
            onAddToFolderClick = { postToAddToFolder = post },
        )
    }

    if (postToAddToFolder != null) {
        val post = postToAddToFolder!!
        AddToFolderDialog(
            post = post,
            onDismissRequest = { postToAddToFolder = null },
            onFolderConfirm = { onAddPostToFolder(post, it.path) },
        )
    }

    if (postToAddToCollection != null) {
        val post = postToAddToCollection!!
        AddToCollectionsDialog(
            onDismissRequest = { postToAddToCollection = null },
            onCollect = { onCollectPost(it) },
            post = post,
        )
    }

    if (postToLoadComments != null && service != null) {
        CommentsSheet(
            onNavigate = onNavigate,
            state = commentsSheetState,
            service = service,
            post = postToLoadComments!!
        )
    }
}

@Composable
private fun RefreshItem(
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        Text(stringResource(BaseR.string.data_outdated_message))

        Spacer(modifier = Modifier.width(16.dp))

        OutlinedButton(onClick = onRefreshClick) {
            Text(stringResource(BaseR.string.refresh))
        }
    }
}

@Composable
private fun HeaderContent(
    scrollProvider: () -> Int,
    services: ImmutableHolder<List<UiServiceManifest>>,
    currentService: UiServiceManifest?,
    defaultHeaderIcons: ImmutableHolder<List<ImageRequest>>,
    onServiceManagementClick: () -> Unit,
    onSelectService: (UiServiceManifest) -> Unit,
    onLongClickCurrentService: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val preferencesStore = LocalContext.current.preferencesStore()

    val serviceHeaderImage = currentService?.localFirstResourcePath(
        type = ServiceResource.Type.HeaderImage,
        fallback = { currentService.headerImage }
    )
    val globalHeaderImage = preferencesStore.headerImage.value
    val forceHeaderImageForAll = preferencesStore.forceHeaderImageForAllServices.value
    val headerPicUrl = if (forceHeaderImageForAll || serviceHeaderImage.isNullOrEmpty()) {
        globalHeaderImage
    } else {
        serviceHeaderImage
    }

    ServiceHeader(
        currentServiceName = currentService?.name,
        modifier = modifier,
        scrollProvider = scrollProvider,
        headerPicUrl = headerPicUrl,
        defaultHeaderIcons = defaultHeaderIcons,
        themeColor = themeColorOrPrimary(
            themeColor = Color(currentService?.themeColor ?: 0),
            darkThemeColor = Color(currentService?.darkThemeColor ?: 0),
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                val text = if (currentService != null) {
                    stringResource(BaseR.string.you_are_browsing)
                } else {
                    "\n"
                }
                Text(
                    text = text,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            ServiceDropdownButton(
                onSelectService = onSelectService,
                onServiceManagementClick = onServiceManagementClick,
                onLongClickCurrentService = onLongClickCurrentService,
                services = services,
                currentService = currentService,
                modifier = Modifier.align(Alignment.Bottom),
            )
        }
    }
}
