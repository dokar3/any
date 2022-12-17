package any.ui.home.following

import any.base.R as BaseR
import any.ui.common.R as CommonUiR
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import any.base.compose.ImmutableHolder
import any.base.prefs.fixedBottomBar
import any.base.prefs.fixedTopBar
import any.base.prefs.preferencesStore
import any.domain.entity.UiUser
import any.navigation.NavEvent
import any.navigation.Routes
import any.navigation.navPushEvent
import any.navigation.navigateToUser
import any.ui.common.lazy.rememberLazyListScrollableState
import any.ui.common.modifier.fabOffset
import any.ui.common.quickScrollToTop
import any.ui.common.theme.topBarBackground
import any.ui.common.widget.EmojiEmptyContent
import any.ui.common.widget.QuickReturnScreen
import any.ui.common.widget.QuickReturnScreenState
import any.ui.common.widget.SearchBar
import any.ui.common.widget.SimpleDialog
import any.ui.common.widget.rememberQuickReturnScreenState
import any.ui.home.HomeScrollToTopManager
import any.ui.home.ScrollToTopResponder
import any.ui.home.SettingsButton
import any.ui.home.TitleBar
import kotlinx.coroutines.launch

@Composable
internal fun FollowingScreen(
    onNavigate: (NavEvent) -> Unit,
    titleBarHeight: Dp,
    bottomBarHeight: Dp,
    onBottomBarOffsetUpdate: (Int) -> Unit,
    scrollToTopManager: HomeScrollToTopManager,
    modifier: Modifier = Modifier,
    viewModel: FollowingViewModel = viewModel(
        factory = FollowingViewModel.Factory(LocalContext.current),
    ),
) {
    val scope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsState()

    val scrollableState = rememberLazyListScrollableState()
    val screenState = rememberQuickReturnScreenState(scrollableState)

    LaunchedEffect(viewModel) {
        viewModel.fetchFollowingUsers()
    }

    DisposableEffect(scrollToTopManager) {
        val scrollToTopResponder = ScrollToTopResponder {
            scope.launch {
                screenState.resetBars()
                scrollableState.listState.quickScrollToTop()
            }
        }
        scrollToTopManager.addHandler(scrollToTopResponder)
        onDispose {
            scrollToTopManager.removeHandler(scrollToTopResponder)
        }
    }

    FollowingScreenContent(
        onNavigate = onNavigate,
        onFilterTextUpdate = viewModel::updateFilterText,
        onSelectUser = viewModel::selectUser,
        onUnselectUser = viewModel::unselectUser,
        onCancelUserSelection = viewModel::cancelUserSelection,
        onUnfollowSelectedUser = viewModel::unfollowSelectedUsers,
        onSelectAllUsers = viewModel::selectAllUsers,
        onSelectService = viewModel::selectService,
        onUnselectService = viewModel::unselectService,
        uiState = uiState,
        titleBarHeight = titleBarHeight,
        bottomBarHeight = bottomBarHeight,
        serviceSelectionHeight = 48.dp,
        onBottomBarOffsetUpdate = onBottomBarOffsetUpdate,
        screenState = screenState,
        listState = scrollableState.listState,
        modifier = modifier,
    )
}

@Composable
private fun FollowingScreenContent(
    onNavigate: (NavEvent) -> Unit,
    onFilterTextUpdate: (TextFieldValue) -> Unit,
    onSelectUser: (UiUser) -> Unit,
    onUnselectUser: (UiUser) -> Unit,
    onCancelUserSelection: () -> Unit,
    onUnfollowSelectedUser: () -> Unit,
    onSelectAllUsers: () -> Unit,
    onSelectService: (ServiceOfFollowingUsers) -> Unit,
    onUnselectService: (ServiceOfFollowingUsers) -> Unit,
    uiState: FollowingUiState,
    titleBarHeight: Dp,
    bottomBarHeight: Dp,
    serviceSelectionHeight: Dp,
    onBottomBarOffsetUpdate: (Int) -> Unit,
    screenState: QuickReturnScreenState,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val preferencesStore = LocalContext.current.preferencesStore()

    val listPadding = PaddingValues(
        top = titleBarHeight + serviceSelectionHeight + WindowInsets.statusBars
            .asPaddingValues()
            .calculateTopPadding(),
        bottom = bottomBarHeight + WindowInsets.navigationBars
            .asPaddingValues()
            .calculateBottomPadding(),
    )

    var bottomBarOffset by remember { mutableStateOf(0) }

    var showUnfollowSelectedDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = uiState.isSelectionEnabled) {
        onCancelUserSelection()
    }

    LaunchedEffect(screenState) {
        screenState.resetBars()
    }

    LaunchedEffect(uiState.isSelectionEnabled) {
        if (uiState.isSelectionEnabled) {
            screenState.resetTopBar()
        }
    }

    QuickReturnScreen(
        state = screenState,
        fixedTopBar = preferencesStore.fixedTopBar.value || uiState.isSelectionEnabled,
        fixedBottomBar = preferencesStore.fixedBottomBar.value,
        bottomBarHeight = listPadding.calculateBottomPadding(),
        topBar = {
            FollowingAppBar(
                onNavigate = onNavigate,
                filterText = uiState.filterText,
                onFilterTextUpdate = onFilterTextUpdate,
                titleBarHeight = titleBarHeight,
                service = ImmutableHolder(uiState.services),
                onSelectService = onSelectService,
                onUnselectService = onUnselectService,
                selectionHeight = serviceSelectionHeight,
                showUserSelection = uiState.isSelectionEnabled,
                selectedUserCount = uiState.selection.size,
                onCancelUserSelection = onCancelUserSelection,
                onSelectAllUserClick = onSelectAllUsers,
            )
        },
        bottomBar = {
            onBottomBarOffsetUpdate(it)
            bottomBarOffset = it
        },
        modifier = modifier,
    ) {
        FollowingList(
            onItemClick = {
                if (uiState.isSelectionEnabled) {
                    if (uiState.selection.contains(it.id)) {
                        onUnselectUser(it)
                    } else {
                        onSelectUser(it)
                    }
                } else {
                    navigateToUser(
                        handler = onNavigate,
                        serviceId = it.serviceId,
                        userId = it.id,
                    )
                }
            },
            onItemLongClick = {
                if (uiState.isSelectionEnabled) {
                    if (uiState.selection.contains(it.id)) {
                        onUnselectUser(it)
                    } else {
                        onSelectUser(it)
                    }
                } else {
                    onSelectUser(it)
                }
            },
            users = ImmutableHolder(uiState.users),
            selection = ImmutableHolder(uiState.selection),
            listState = listState,
            modifier = Modifier.testTag("followingList"),
            contentPadding = listPadding,
        )

        if (uiState.isSelectionEnabled) {
            val navBarHeightPx = WindowInsets.navigationBars
                .getBottom(LocalDensity.current)
            FloatingActionButton(
                onClick = { showUnfollowSelectedDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .fabOffset()
                    .offset {
                        val btmBarHeightPx = bottomBarHeight.roundToPx()
                        val btmOffset = bottomBarOffset.coerceAtMost(
                            btmBarHeightPx - navBarHeightPx
                        )
                        val fabOffset = -(btmBarHeightPx - btmOffset)
                        IntOffset(x = 0, y = fabOffset)
                    },
                backgroundColor = MaterialTheme.colors.error,
                contentColor = MaterialTheme.colors.onError,
            ) {
                Icon(
                    painter = painterResource(CommonUiR.drawable.ic_unfollow),
                    contentDescription = stringResource(BaseR.string.remove_selected),
                )
            }
        }

        if (uiState.showEmpty) {
            EmojiEmptyContent(
                modifier = Modifier.fillMaxSize()
            ) {
                Text(stringResource(BaseR.string.no_following_user))
            }
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }

    if (showUnfollowSelectedDialog) {
        SimpleDialog(
            onDismissRequest = { showUnfollowSelectedDialog = false },
            title = { Text(stringResource(BaseR.string.unfollow_selected)) },
            text = {
                Text(
                    stringResource(
                        BaseR.string._unfollow_selected_alert,
                        uiState.selection.size,
                    )
                )
            },
            cancelText = { Text(stringResource(android.R.string.cancel)) },
            confirmText = {
                Text(
                    text = stringResource(BaseR.string.confirm),
                    color = MaterialTheme.colors.error,
                )
            },
            onConfirmClick = { onUnfollowSelectedUser() },
        )
    }
}

@Composable
private fun FollowingAppBar(
    onNavigate: (NavEvent) -> Unit,
    filterText: TextFieldValue,
    onFilterTextUpdate: (TextFieldValue) -> Unit,
    titleBarHeight: Dp,
    service: ImmutableHolder<List<ServiceOfFollowingUsers>>,
    onSelectService: (ServiceOfFollowingUsers) -> Unit,
    onUnselectService: (ServiceOfFollowingUsers) -> Unit,
    selectionHeight: Dp,
    onCancelUserSelection: () -> Unit,
    onSelectAllUserClick: () -> Unit,
    showUserSelection: Boolean,
    selectedUserCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        TitleBar(
            height = titleBarHeight,
            startActionButton = {
                SettingsButton(
                    onClick = {
                        onNavigate(navPushEvent(Routes.SETTINGS))
                    },
                )
            },
        ) {
            CompositionLocalProvider(
                LocalTextStyle provides LocalTextStyle.current.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                )
            ) {
                SearchBar(
                    text = filterText,
                    onValueChange = onFilterTextUpdate,
                    placeholder = {
                        Text(
                            text = stringResource(BaseR.string.search_following_users),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }

        if (showUserSelection) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(selectionHeight)
                    .background(MaterialTheme.colors.topBarBackground)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onCancelUserSelection) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(BaseR.string.cancel_selection),
                        )
                    }

                    Text(stringResource(BaseR.string._selected, selectedUserCount))
                }

                IconButton(onClick = onSelectAllUserClick) {
                    Icon(
                        painter = painterResource(CommonUiR.drawable.ic_baseline_done_all_24),
                        contentDescription = stringResource(BaseR.string.select_all),
                    )
                }
            }
        } else {
            ServiceSelection(
                onItemClick = {
                    if (it.isSelected) {
                        onUnselectService(it)
                    } else {
                        onSelectService(it)
                    }
                },
                showAsRow = true,
                services = service,
                modifier = Modifier
                    .height(selectionHeight)
                    .background(MaterialTheme.colors.topBarBackground),
            )
        }
    }
}
