package any.ui.search

import any.base.R as BaseR
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import any.base.ImmutableHolder
import any.base.util.Intents
import any.data.entity.PostsViewType
import any.domain.entity.UiPost
import any.navigation.NavEvent
import any.navigation.Routes
import any.navigation.navPushEvent
import any.navigation.navigateToMedia
import any.navigation.navigateToPost
import any.navigation.navigateToUser
import any.navigation.settings
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
import any.ui.common.widget.EmojiEmptyContent
import any.ui.common.widget.EndOfList
import any.ui.common.widget.ProgressBar
import any.ui.common.widget.StatusBarSpacer
import any.ui.common.widget.UiMessagePopup
import any.ui.common.widget.rememberSearchBarState
import any.ui.search.viewmodel.SearchViewModel
import com.dokar.sheets.rememberBottomSheetState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onNavigate: (NavEvent) -> Unit,
    serviceId: String?,
    query: String?,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel(
        factory = SearchViewModel.Factory(LocalContext.current)
    ),
) {
    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    val uiState by viewModel.searchUiState.collectAsState()

    var selectedPost: UiPost? by remember { mutableStateOf(null) }

    var postToAddToFolder: UiPost? by remember { mutableStateOf(null) }

    var postToAddToCollection: UiPost? by remember { mutableStateOf(null) }

    var postToLoadComments: UiPost? by remember { mutableStateOf(null) }

    val searchBarState = rememberSearchBarState()

    val commentsSheetState = rememberBottomSheetState()

    var showKeyboardAfterEntering by rememberSaveable(
        inputs = arrayOf(viewModel, uiState.currentService),
    ) {
        mutableStateOf(true)
    }

    LaunchedEffect(serviceId) {
        viewModel.selectService(serviceId)
    }

    LaunchedEffect(query) {
        if (!uiState.hasSetQuery && !query.isNullOrEmpty()) {
            viewModel.updateQuery(TextFieldValue(query))
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.updatePosts()
    }

    val isImeVisible = rememberUpdatedState(WindowInsets.isImeVisible)
    LaunchedEffect(
        uiState.currentService?.id,
        uiState.isSearchable,
        showKeyboardAfterEntering,
    ) {
        if (uiState.currentService != null &&
            uiState.isSearchable &&
            showKeyboardAfterEntering
        ) {
            val maxRetries = 20
            for (i in 1..maxRetries) {
                if (!isImeVisible.value) {
                    searchBarState.showKeyboard()
                    delay(50)
                } else {
                    showKeyboardAfterEntering = false
                    break
                }
            }
        }
    }

    DisposableEffect(viewModel) {
        onDispose {
            searchBarState.hideKeyboard()
            viewModel.clearMessage()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        StatusBarSpacer()

        TitleBar(
            uiState = uiState,
            onBack = { onNavigate(NavEvent.Back) },
            onUpdateQuery = viewModel::updateQuery,
            onSearch = viewModel::search,
            onManagementClick = {
                val route = Routes.settings(subSettings = Routes.Settings.SERVICE_MGT)
                onNavigate(navPushEvent(route))
            },
            onSelectService = viewModel::selectService,
            searchBarState = searchBarState,
        )

        Divider()

        val scrollableState = rememberLazyGridScrollableState()

        if (uiState.isSearchable) {
            Box {
                PostList(
                    state = scrollableState.gridState,
                    posts = ImmutableHolder(uiState.posts),
                    pageKey = ImmutableHolder(uiState.pageKey),
                    viewType = uiState.currentService?.postsViewType ?: PostsViewType.List,
                    defThumbAspectRatio = uiState.currentService?.mediaAspectRatio,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .verticalScrollBar(state = scrollableState),
                    onCommentsClick = {
                        postToLoadComments = it
                        scope.launch { commentsSheetState.peek() }
                    },
                    onCollectClick = {
                        if (it.isCollected()) {
                            viewModel.discardPost(it)
                        } else {
                            viewModel.collectPost(it)
                        }
                    },
                    onMoreClick = { selectedPost = it },
                    onMediaClick = { post, index ->
                        navigateToMedia(onNavigate, context, post.raw, index)
                    },
                    onUserClick = { serviceId, userId ->
                        navigateToUser(onNavigate, serviceId, userId)
                    },
                    onLinkClick = { Intents.openInBrowser(context, it) },
                    onItemClick = { navigateToPost(onNavigate, context, it.raw) },
                    onItemLongClick = { selectedPost = it },
                    headerContent = {
                        if (uiState.isLoading && !uiState.isLoadingMore) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                ProgressBar()

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(stringResource(BaseR.string.searching))
                            }
                        }
                    },
                    emptyContent = {
                        if (uiState.searchedCount > 0 && !uiState.isLoading) {
                            EmojiEmptyContent(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(BaseR.string.nothing_found))
                            }
                        }
                    },
                    loadMoreContent = {
                        if (!uiState.hasMore && !uiState.isLoadingMore) {
                            EndOfList(
                                onClick = {
                                    viewModel.fetchNextPage()
                                },
                            ) {
                                Text(stringResource(BaseR.string.no_more_results))
                            }
                        } else if (!uiState.isLoadingMore && !uiState.isSuccess) {
                            RetryItem(
                                message = stringResource(BaseR.string.load_failed),
                                onClick = { viewModel.fetchNextPage() },
                            )
                        } else {
                            LoadingItem()
                        }
                    },
                    onLoadMore = {
                        if (uiState.hasMore) {
                            viewModel.fetchNextPage()
                        }
                    },
                    isLoadingMore = uiState.isLoadingMore,
                )

                ScrollToTopFab(
                    scrollableState = scrollableState,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .fabOffset(),
                )
            }
        } else {
            EmojiEmptyContent(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
            ) {
                Text(
                    text = stringResource(BaseR.string.search_not_available_for_service),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    UiMessagePopup(
        message = uiState.message,
        onClearMessage = { viewModel.clearMessage() },
        onRetry = { viewModel.fetchPreviousRequest() },
    )

    if (selectedPost != null) {
        val post = selectedPost!!
        PostOptionMenu(
            post = post,
            showMultiSelectionItem = false,
            onDiscardRequest = { viewModel.discardPost(post) },
            onAddToCollectionsClick = { postToAddToCollection = post },
            onDismiss = { selectedPost = null },
            onAddToFolderClick = { postToAddToFolder = selectedPost },
        )
    }

    if (postToAddToFolder != null) {
        val post = postToAddToFolder!!
        AddToFolderDialog(
            onDismissRequest = { postToAddToFolder = null },
            onFolderConfirm = { viewModel.addToFolder(post, it.path) },
            post = post,
        )
    }

    if (postToAddToCollection != null) {
        val post = postToAddToCollection!!
        AddToCollectionsDialog(
            onDismissRequest = { postToAddToCollection = null },
            onCollect = { viewModel.collectPost(it) },
            post = post,
        )
    }

    val service = uiState.currentService
    if (postToLoadComments != null && service != null) {
        CommentsSheet(
            onNavigate = onNavigate,
            state = commentsSheetState,
            service = service,
            post = postToLoadComments!!
        )
    }
}
