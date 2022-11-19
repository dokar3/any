package any.ui.profile

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import any.base.ImmutableHolder
import any.base.R
import any.base.util.Intents
import any.data.entity.ServiceViewType
import any.domain.entity.UiPost
import any.navigation.NavEvent
import any.navigation.navigateToMedia
import any.navigation.navigateToPost
import any.navigation.navigateToUser
import any.ui.comments.CommentsSheet
import any.ui.common.dialog.AddToCollectionsDialog
import any.ui.common.dialog.AddToFolderDialog
import any.ui.common.lazy.ScrollToTopFab
import any.ui.common.lazy.rememberLazyGridScrollableState
import any.ui.common.menu.PostOptionMenu
import any.ui.common.modifier.fabOffset
import any.ui.common.modifier.verticalScrollBar
import any.ui.common.post.LoadingItem
import any.ui.common.post.PostList
import any.ui.common.post.RetryItem
import any.ui.common.theme.compositedNavigationBarColor
import any.ui.common.theme.statusBar
import any.ui.common.theme.themeColorOrPrimary
import any.ui.common.widget.BoxWithSystemBars
import any.ui.common.widget.EndOfList
import any.ui.common.widget.ProgressPullRefreshIndicator
import any.ui.common.widget.UiMessagePopup
import any.ui.common.widget.rememberBarsColorController
import any.ui.common.widget.rememberPullRefreshIndicatorOffset
import any.ui.profile.viewmodel.ProfileUiState
import any.ui.profile.viewmodel.ProfileViewModel
import com.dokar.sheets.rememberBottomSheetState
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    onNavigate: (NavEvent) -> Unit,
    serviceId: String,
    userId: String,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.Factory(context = LocalContext.current)
    ),
) {
    LaunchedEffect(serviceId, userId) {
        viewModel.fetchProfile(serviceId = serviceId, userId = userId)
    }

    BoxWithSystemBars(
        barsColorController = rememberBarsColorController(
            statusBarColor = MaterialTheme.colors.statusBar,
            navigationBarColor = MaterialTheme.colors.compositedNavigationBarColor,
        ),
    ) {
        ProfileScreenContent(
            uiState = viewModel.uiState.collectAsState().value,
            onNavigate = onNavigate,
            onFollow = viewModel::followUser,
            onUnfollow = viewModel::unfollowUser,
            onRefresh = {
                viewModel.fetchProfile(
                    serviceId = serviceId,
                    userId = userId,
                    remoteOnly = true,
                )
            },
            onFetchMore = viewModel::fetchMorePosts,
            onRetryPostsFetch = viewModel::retryPostsFetch,
            onClearMessage = viewModel::clearMessage,
            onCollectPost = viewModel::collectPost,
            onDiscardPost = viewModel::discardPost,
            onAddPostToFolder = viewModel::addToFolder,
            modifier = modifier,
        )
    }
}

@Composable
fun ProfileScreen(
    onNavigate: (NavEvent) -> Unit,
    userUrl: String,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.Factory(context = LocalContext.current)
    ),
) {
    LaunchedEffect(userUrl) {
        viewModel.fetchProfile(userUrl = userUrl)
    }

    ProfileScreenContent(
        uiState = viewModel.uiState.collectAsState().value,
        onNavigate = onNavigate,
        onFollow = viewModel::followUser,
        onUnfollow = viewModel::unfollowUser,
        onRefresh = {
            viewModel.fetchProfile(
                userUrl = userUrl,
                remoteOnly = true
            )
        },
        onFetchMore = viewModel::fetchMorePosts,
        onRetryPostsFetch = viewModel::retryPostsFetch,
        onClearMessage = viewModel::clearMessage,
        onCollectPost = viewModel::collectPost,
        onDiscardPost = viewModel::discardPost,
        onAddPostToFolder = viewModel::addToFolder,
        modifier = modifier,
    )
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterialApi::class)
@Composable
private fun ProfileScreenContent(
    uiState: ProfileUiState,
    onNavigate: (NavEvent) -> Unit,
    onFollow: () -> Unit,
    onUnfollow: () -> Unit,
    onRefresh: () -> Unit,
    onFetchMore: () -> Unit,
    onRetryPostsFetch: () -> Unit,
    onClearMessage: () -> Unit,
    onCollectPost: (UiPost) -> Unit,
    onDiscardPost: (UiPost) -> Unit,
    onAddPostToFolder: (UiPost, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    val scrollableState = rememberLazyGridScrollableState()

    val service = uiState.service

    var selectedPost: UiPost? by remember { mutableStateOf(null) }

    var postToAddToFolder: UiPost? by remember { mutableStateOf(null) }

    var postToAddToCollection: UiPost? by remember { mutableStateOf(null) }

    var postToLoadComments: UiPost? by remember { mutableStateOf(null) }

    val isRefreshing = uiState.isLoadingUser || uiState.isLoadingPosts

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = onRefresh,
    )

    val indicatorOffset = rememberPullRefreshIndicatorOffset(state = pullRefreshState)

    val commentsSheetState = rememberBottomSheetState()

    val themeColor = themeColorOrPrimary(
        themeColor = Color(uiState.user?.serviceThemeColor ?: 0),
        darkThemeColor = Color(uiState.user?.serviceDarkThemeColor ?: 0),
    )

    Box(modifier = modifier.pullRefresh(state = pullRefreshState)) {
        var bannerHeight by remember { mutableStateOf(0) }

        var listScrollY: Int? by remember { mutableStateOf(0) }

        LaunchedEffect(scrollableState) {
            snapshotFlow { scrollableState.visibleItemsInfo }
                .mapNotNull { it.firstOrNull() }
                .collect { first ->
                    listScrollY = if (first.index == 0) {
                        first.offset
                    } else {
                        null
                    }
                }
        }

        ProfileHeaderBanner(
            listScrollYProvider = { listScrollY },
            contentOffsetYProvider = { indicatorOffset.toFloat() },
            heightUpdater = { bannerHeight = it },
            url = uiState.user?.banner,
        )

        val listPadding = WindowInsets.navigationBars.asPaddingValues()

        PostList(
            state = scrollableState.gridState,
            posts = ImmutableHolder(uiState.posts),
            pageKey = ImmutableHolder(null),
            viewType = service?.viewType ?: ServiceViewType.List,
            defThumbAspectRatio = service?.mediaAspectRatio,
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
            onUserClick = { serviceId, userId ->
                if (userId != uiState.user?.id) {
                    navigateToUser(onNavigate, serviceId, userId)
                }
            },
            onLinkClick = { Intents.openInBrowser(context, it) },
            onItemClick = { navigateToPost(onNavigate, context, it.raw) },
            onItemLongClick = { selectedPost = it },
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, indicatorOffset) }
                .verticalScrollBar(
                    state = scrollableState,
                    padding = PaddingValues(
                        top = WindowInsets.statusBars
                            .asPaddingValues()
                            .calculateTopPadding(),
                        bottom = listPadding.calculateBottomPadding(),
                    ),
                ),
            headerContent = {
                ProfileHeader(
                    onFollowClick = {
                        if (uiState.user != null) {
                            if (uiState.user.isFollowed()) {
                                onUnfollow()
                            } else {
                                onFollow()
                            }
                        }
                    },
                    user = uiState.user,
                    bannerHeight = bannerHeight,
                    themeColor = themeColor,
                )
            },
            onLoadMore = {
                if (uiState.hasMore) {
                    onFetchMore()
                }
            },
            loadMoreContent = {
                if (!uiState.hasMore && !uiState.isLoadingMorePosts) {
                    EndOfList(
                        onClick = onRetryPostsFetch,
                    ) {
                        Text(stringResource(R.string.no_more_posts))
                    }
                } else if (!uiState.isLoadingMorePosts && uiState.isFailedToFetchPosts) {
                    RetryItem(
                        message = stringResource(R.string.load_failed),
                        onClick = onRetryPostsFetch,
                    )
                } else {
                    LoadingItem()
                }
            },
            isLoadingMore = uiState.isLoadingMorePosts,
            contentPadding = listPadding,
        )

        ScrollToTopFab(
            scrollableState = scrollableState,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .fabOffset(),
        )

        TitleBar(
            scrollProvider = { listScrollY ?: -bannerHeight },
            onBack = { onNavigate(NavEvent.Back) },
            title = uiState.user?.name,
            fullyVisibleScrollY = -bannerHeight,
        )

        ProgressPullRefreshIndicator(
            state = pullRefreshState,
            isRefreshing = isRefreshing,
            indicatorOffsetProvider = { indicatorOffset },
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
            loadingProgress = null,
            textColor = Color.White,
            progressColor = themeColor,
        )
    }

    UiMessagePopup(
        message = uiState.message,
        onClearMessage = onClearMessage,
        onRetry = {
            if (uiState.isFailedToFetchPosts) {
                onRetryPostsFetch()
            } else {
                onRefresh()
            }
        },
    )

    if (selectedPost != null) {
        val post = selectedPost!!
        PostOptionMenu(
            post = post,
            showMultiSelectionItem = false,
            onDiscardRequest = { onDiscardPost(post) },
            onAddToCollectionsClick = { postToAddToCollection = post },
            onDismiss = { selectedPost = null },
            onAddToFolderClick = { postToAddToFolder = selectedPost },
        )
    }

    if (postToAddToFolder != null) {
        val post = postToAddToFolder!!
        AddToFolderDialog(
            onDismissRequest = { postToAddToFolder = null },
            onFolderConfirm = { onAddPostToFolder(post, it.path) },
            post = post,
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