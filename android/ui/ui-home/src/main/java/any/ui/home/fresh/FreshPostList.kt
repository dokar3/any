package any.ui.home.fresh

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.R
import any.base.compose.ImmutableHolder
import any.base.util.Intents
import any.data.entity.PostsViewType
import any.domain.entity.UiPost
import any.domain.entity.UiServiceManifest
import any.navigation.NavEvent
import any.navigation.Routes
import any.navigation.navPushEvent
import any.navigation.navigateToMedia
import any.navigation.navigateToPost
import any.navigation.navigateToUser
import any.navigation.settings
import any.ui.comments.CommentsSheet
import any.ui.common.dialog.AddToCollectionsDialog
import any.ui.common.dialog.PostFolderSelectionDialog
import any.ui.common.lazy.LazyGridScrollableState
import any.ui.common.menu.PostOptionMenu
import any.ui.common.modifier.verticalScrollBar
import any.ui.common.post.LoadingItem
import any.ui.common.post.PostList
import any.ui.common.post.RetryItem
import any.ui.common.widget.EmojiEmptyContent
import any.ui.common.widget.EndOfList
import any.ui.home.fresh.viewmodel.FreshUiState
import com.dokar.sheets.rememberBottomSheetState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

private enum class ItemsAnimState {
    Preparing,
    Started,
    Finished,
}

@ExperimentalFoundationApi
@Composable
internal fun FreshPostList(
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

    LaunchedEffect(service?.id) {
        currPosts = ImmutableHolder(emptyList())
        itemsAnimState = ItemsAnimState.Preparing
    }

    LaunchedEffect(currPosts, posts) {
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
            stopItemAnimations()
            initialItemAnimValue = 0f
            targetItemAnimValue = 1f
            itemsAnimState = ItemsAnimState.Started
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
        viewType = service?.postsViewType ?: PostsViewType.List,
        defThumbAspectRatio = service?.mediaAspectRatio,
        modifier = modifier
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
                    Text(stringResource(R.string.no_services))

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        stringResource(R.string.please_add_or_enable_services),
                        fontSize = 16.sp,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            val route = Routes.settings(subSettings = Routes.Settings.SERVICE_MGT)
                            onNavigate(navPushEvent(route))
                        },
                    ) {
                        Text(stringResource(R.string.service_management))
                    }
                }
            } else {
                EmojiEmptyContent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp),
                ) {
                    Text(stringResource(R.string.no_posts))

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { onFetchFirstPage(false) },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(stringResource(R.string.refresh))
                    }
                }
            }
        },
        loadMoreContent = {
            if (!uiState.hasMore && !uiState.isLoadingMorePosts) {
                EndOfList(
                    onClick = { onFetchMore(true) },
                ) {
                    Text(stringResource(R.string.no_more_posts))
                }
            } else if (!uiState.isLoading() && !uiState.isSuccess) {
                RetryItem(
                    message = stringResource(R.string.failed_to_load),
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
        PostFolderSelectionDialog(
            onDismissRequest = { postToAddToFolder = null },
            onFolderSelected = { onAddPostToFolder(post, it.path) },
            initiallySelectedFolder = post.folder,
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

        Text(stringResource(R.string.data_outdated_message))

        Spacer(modifier = Modifier.width(16.dp))

        OutlinedButton(onClick = onRefreshClick) {
            Text(stringResource(R.string.refresh))
        }
    }
}
