package any.ui.home.fresh

import any.base.R as BaseR
import any.ui.common.R as CommonUiR
import android.app.Activity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
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
import any.base.compose.ImmutableHolder
import any.base.compose.LocalBenchmarkBuild
import any.base.image.ImageRequest
import any.base.prefs.headerImage
import any.base.prefs.overrideServiceHeaderImage
import any.base.prefs.preferencesStore
import any.base.util.Permissions
import any.base.util.isHttpUrl
import any.data.entity.ServiceResource
import any.domain.entity.UiPost
import any.domain.entity.UiServiceManifest
import any.navigation.NavEvent
import any.navigation.Routes
import any.navigation.navPushEvent
import any.navigation.post
import any.navigation.search
import any.navigation.settings
import any.ui.common.awaitAnimations
import any.ui.common.lazy.LazyGridScrollableState
import any.ui.common.lazy.rememberLazyGridScrollableState
import any.ui.common.theme.themeColorOrPrimary
import any.ui.common.widget.EditDialog
import any.ui.common.widget.QuickReturnScreen
import any.ui.common.widget.ServiceDropdownButton
import any.ui.common.widget.TitleActionButton
import any.ui.common.widget.UiMessagePopup
import any.ui.common.widget.rememberQuickReturnScreenState
import any.ui.home.HomeScrollToTopManager
import any.ui.home.IconTintTheme
import any.ui.home.ScrollToTopResponder
import any.ui.home.SettingsButton
import any.ui.home.TitleBar
import any.ui.home.fresh.viewmodel.FreshUiState
import any.ui.home.fresh.viewmodel.FreshViewModel
import any.ui.jslogger.FloatingLoggerService
import any.ui.service.ServiceDialog
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
    statusBarColor: Color,
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

    var firstItemOffsetY by remember { mutableStateOf(0) }

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
        firstItemOffsetY = 0
        scope.launch {
            screenState.resetBars(animate = true)
            scrollableState.quickScrollToTop()
        }
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
        val handler = ScrollToTopResponder {
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
        fixedTopBar = LocalBenchmarkBuild.current,
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
                    modifier = Modifier.semantics { contentDescription = "ServiceSelector" },
                )
            }
        },
        bottomBar = { onBottomBarOffsetUpdate(it) },
    ) {
        var headerHeightPx by remember { mutableStateOf(0) }

        LaunchedEffect(scrollableState, statusBarColor) {
            val topBarAlpha = statusBarColor.alpha
            launch {
                snapshotFlow { scrollableState.visibleItemsInfo }
                    .mapNotNull { it.firstOrNull() }
                    .map { it.index == 0 }
                    .distinctUntilChanged()
                    .collect { isFirstItemVisible ->
                        if (!isFirstItemVisible) {
                            changeStatusBarColor(statusBarColor)
                            titleBarIconTintTheme = IconTintTheme.Auto
                            titleBarBackgroundColor = statusBarColor
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
                    if (headerHeightPx == 0) {
                        return@collect
                    }
                    val progress = (-it.toFloat() / headerHeightPx).coerceIn(0f, 1f)
                    val alpha = topBarAlpha * progress
                    changeStatusBarColor(statusBarColor.copy(alpha = alpha))
                    titleAlpha = alpha
                    titleBarIconTintTheme = if (alpha > 0.4f) {
                        IconTintTheme.Auto
                    } else {
                        IconTintTheme.Light
                    }
                    titleBarBackgroundColor = statusBarColor.copy(alpha = alpha)
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
                ServiceHeaderItem(
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
            label = { Text(stringResource(BaseR.string.enter_an_url_or_command)) },
            onConfirmClick = {
                if (text.isEmpty()) {
                    return@EditDialog
                }

                if (text == "jslogs") {
                    val activity = context as Activity
                    if (Permissions.checkOrRequestOverlayPermission(activity)) {
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
        ServiceDialog(
            onDismissRequest = { showConfigureServiceDialog = false },
            service = currentService,
            isAdded = true,
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
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

    val delayedIsRefreshing = remember { mutableStateOf(isRefreshing) }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = delayedIsRefreshing.value,
        onRefresh = { onFetchFirstPage(true) },
    )

    LaunchedEffect(isRefreshing) {
        pullRefreshState.awaitAnimations()
        delayedIsRefreshing.value = isRefreshing
    }

    Box(
        modifier = Modifier.pullRefresh(
            state = pullRefreshState,
            enabled = service != null,
        )
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

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            scale = true,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    val height = appBarHeight()
                    if (height >= statusBarHeight) {
                        val minOffY = height - statusBarHeight
                        val offY = appBarOffsetProvider().coerceIn(-minOffY, 0f)
                        translationY = height + offY
                    }
                },
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

@Composable
private fun ServiceHeaderItem(
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
    val overrideServiceHeaderImage = preferencesStore.overrideServiceHeaderImage.value
    val headerPicUrl = if (overrideServiceHeaderImage || serviceHeaderImage.isNullOrEmpty()) {
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
                modifier = Modifier
                    .align(Alignment.Bottom)
                    .semantics { contentDescription = "ServiceSelector" },
            )
        }
    }
}
