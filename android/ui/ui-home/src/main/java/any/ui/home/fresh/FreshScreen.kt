package any.ui.home.fresh

import any.base.R as BaseR
import any.ui.common.R as CommonUiR
import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
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
import any.ui.common.ServiceDropdownButton
import any.ui.common.lazy.LazyGridScrollableState
import any.ui.common.lazy.rememberLazyGridScrollableState
import any.ui.common.theme.statusBar
import any.ui.common.theme.themeColorOrPrimary
import any.ui.common.theme.topBarBackground
import any.ui.common.widget.EditDialog
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
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
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
            isAdded = true,
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
