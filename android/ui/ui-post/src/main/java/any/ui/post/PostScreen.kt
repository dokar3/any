package any.ui.post

import any.base.R as BaseR
import any.ui.common.R as CommonUiR
import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.ripple.rememberRipple
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import any.base.ImmutableHolder
import any.base.StableHolder
import any.base.image.ImageLoader
import any.base.image.ImageRequest
import any.base.prefs.postFontScale
import any.base.prefs.postLineSpacingMultiplier
import any.base.prefs.preferencesStore
import any.base.prefs.showDevOptions
import any.base.util.Intents
import any.base.util.Permissions
import any.data.entity.Post
import any.domain.entity.UiContentElement
import any.domain.entity.UiPost
import any.domain.entity.UiServiceManifest
import any.navigation.NavEvent
import any.navigation.Routes
import any.navigation.imagePager
import any.navigation.navPushEvent
import any.navigation.post
import any.navigation.search
import any.navigation.userProfile
import any.ui.comments.CommentsSheet
import any.ui.common.LocalFontScale
import any.ui.common.dialog.AddToCollectionsDialog
import any.ui.common.itemRange
import any.ui.common.lazy.LazyListScrollableState
import any.ui.common.lazy.rememberLazyListScrollableState
import any.ui.common.modifier.verticalScrollBar
import any.ui.common.richtext.Html
import any.ui.common.richtext.RichTextStyle
import any.ui.common.theme.compositedNavigationBarColor
import any.ui.common.theme.compositedStatusBarColor
import any.ui.common.toDpOffset
import any.ui.common.visibleItemRange
import any.ui.common.widget.AnimatedPopup
import any.ui.common.widget.AnimatedPopupItem
import any.ui.common.widget.BoxWithSystemBars
import any.ui.common.widget.CommentCount
import any.ui.common.widget.EditDialog
import any.ui.common.widget.EmojiEmptyContent
import any.ui.common.widget.MessagePopup
import any.ui.common.widget.ProgressPullRefreshIndicator
import any.ui.common.widget.QuickReturnScreen
import any.ui.common.widget.rememberAnimatedPopupDismissRequester
import any.ui.common.widget.rememberBarsColorController
import any.ui.common.widget.rememberPullRefreshIndicatorOffset
import any.ui.common.widget.rememberQuickReturnScreenState
import any.ui.imagepager.ImagePagerPositionController
import any.ui.post.header.Header
import any.ui.post.item.contentElementItem
import any.ui.post.menu.BookmarksItem
import any.ui.post.menu.CommentsItem
import any.ui.post.menu.CopyUrlItem
import any.ui.post.menu.FloatingPostItem
import any.ui.post.menu.GoToTopItem
import any.ui.post.menu.JumpToPageItem
import any.ui.post.menu.OpenInBrowserItem
import any.ui.post.menu.PostImageOptionsPopup
import any.ui.post.menu.ReversePagesItem
import any.ui.post.menu.SectionsItem
import any.ui.post.menu.ShareItem
import any.ui.post.menu.TextStyleItem
import any.ui.post.menu.ThemeItem
import any.ui.post.sheet.AddBookmarkDialog
import any.ui.post.sheet.TextStyleSheet
import any.ui.post.sheet.ThemeSettingsSheet
import any.ui.post.viewmodel.BookmarkViewModel
import any.ui.post.viewmodel.PostUiState
import any.ui.post.viewmodel.PostViewModel
import any.ui.readingbubble.ReadingBubbleService
import any.ui.readingbubble.entity.ReadingPost
import com.dokar.sheets.detectPointerPositionChanges
import com.dokar.sheets.rememberBottomSheetState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

@Composable
fun PostScreen(
    onNavigate: (NavEvent) -> Unit,
    postUrl: String?,
    serviceId: String?,
    isRunningExitTransition: Boolean,
    initialElementIndex: Int = -1,
    initialElementScrollOffset: Int = 0,
    viewModel: PostViewModel = viewModel(
        factory = PostViewModel.Factory(LocalContext.current)
    ),
    bookmarkViewModel: BookmarkViewModel = viewModel(
        factory = BookmarkViewModel.Factory(LocalContext.current),
    ),
    positionController: ImagePagerPositionController,
) {
    val scope = rememberCoroutineScope()

    val postDrawerState = rememberPostDrawerState()

    val scrollableState = rememberLazyListScrollableState()

    BoxWithSystemBars(
        barsColorController = rememberBarsColorController(
            statusBarColor = MaterialTheme.colors.compositedStatusBarColor,
            navigationBarColor = MaterialTheme.colors.compositedNavigationBarColor,
        ),
    ) {
        PostScreenContent(
            onNavigate = onNavigate,
            viewModel = viewModel,
            bookmarkViewModel = bookmarkViewModel,
            positionController = positionController,
            postUrl = postUrl,
            serviceId = serviceId,
            initialElementIndex = initialElementIndex,
            initialElementScrollOffset = initialElementScrollOffset,
            scrollableState = scrollableState,
            drawerState = postDrawerState,
            isRunningExitTransition = isRunningExitTransition,
        )
    }

    val uiState by viewModel.postUiState.collectAsState()
    val bookmarkUiState by bookmarkViewModel.bookmarkUiState.collectAsState()
    PostDrawer(
        state = postDrawerState,
        bookmarkUiState = bookmarkUiState,
        sections = ImmutableHolder(uiState.sections),
        onSectionClick = { _, section ->
            scope.launch {
                // Scroll to target section
                val offset = calculateContentOffset(uiState.post)
                scrollableState.listState.scrollToItem(offset + section.targetElementIndex)
                postDrawerState.close()
            }
        },
        onLoadBookmarkRequest = {
            if (serviceId != null && postUrl != null) {
                bookmarkViewModel.loadBookmarks(serviceId, postUrl)
            }
        },
        onBookmarkClick = { bookmark ->
            scope.launch {
                val pos = if (uiState.reversedPages) {
                    uiState.images.size - bookmark.elementIndex
                } else {
                    bookmark.elementIndex
                } + calculateContentOffset(uiState.post)
                scrollableState.listState.scrollToItem(pos)
                postDrawerState.close()
            }
        },
        onRemoveBookmark = {
            bookmarkViewModel.removeBookmark(it)
        },
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun PostScreenContent(
    onNavigate: (NavEvent) -> Unit,
    viewModel: PostViewModel,
    bookmarkViewModel: BookmarkViewModel,
    positionController: ImagePagerPositionController,
    postUrl: String?,
    serviceId: String?,
    initialElementIndex: Int,
    initialElementScrollOffset: Int,
    scrollableState: LazyListScrollableState,
    drawerState: PostDrawerState,
    isRunningExitTransition: Boolean,
) {
    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    var jumpToPageDialogVisible by remember { mutableStateOf(false) }

    val density = LocalDensity.current

    var titleBarHeight by remember { mutableStateOf(0.dp) }
    var bottomBarHeight by remember { mutableStateOf(0.dp) }

    val uiState by viewModel.postUiState.collectAsState()

    val quickReturnScreenState = rememberQuickReturnScreenState(
        lazyScrollableState = scrollableState,
    )

    val textStyleSheetState = rememberBottomSheetState()
    val themeSettingsSheetState = rememberBottomSheetState()
    val commentsSheetState = rememberBottomSheetState()

    LaunchedEffect(postUrl) {
        viewModel.fetchPost(serviceId, postUrl)
    }

    QuickReturnScreen(
        state = quickReturnScreenState,
        topBar = {
            var showDebugMenu by remember { mutableStateOf(false) }

            TitleBar(
                post = uiState.post,
                onBackClick = { onNavigate(NavEvent.Back) },
                onBackLongClick = {
                    if (context.preferencesStore().showDevOptions.value) {
                        showDebugMenu = true
                    }
                },
                onCollectRequest = viewModel::collectPost,
                onDiscardRequest = viewModel::discardPost,
                modifier = Modifier.onSizeChanged {
                    titleBarHeight = with(density) { it.height.toDp() }
                },
            )

            if (showDebugMenu) {
                val popupDismissRequester = rememberAnimatedPopupDismissRequester()
                AnimatedPopup(
                    dismissRequester = popupDismissRequester,
                    onDismissed = { showDebugMenu = false },
                    offset = DpOffset(12.dp, titleBarHeight),
                    scaleAnimOrigin = TransformOrigin(0f, 0f),
                ) {
                    AnimatedPopupItem(
                        index = 0,
                        onClick = {
                            popupDismissRequester.dismiss()
                            if (uiState.contentElements.isEmpty()) {
                                return@AnimatedPopupItem
                            }
                            uiState.post?.let {
                                viewModel.removePostContentCache(it)
                                Toast
                                    .makeText(
                                        context,
                                        "Removed post content cache",
                                        Toast.LENGTH_SHORT
                                    )
                                    .show()
                            }
                        },
                    ) {
                        Text("Clear post content")
                    }

                    AnimatedPopupItem(
                        index = 1,
                        onClick = {
                            popupDismissRequester.dismiss()
                            val images = uiState.images.toMutableList()
                            uiState.post?.media?.forEach {
                                if (it.type != Post.Media.Type.Video) {
                                    images.add(it.url)
                                }
                                it.thumbnail?.let { thumb ->
                                    images.add(thumb)
                                }
                            }
                            if (images.isEmpty()) {
                                return@AnimatedPopupItem
                            }
                            viewModel.viewModelScope.launch(Dispatchers.IO) {
                                images.forEach {
                                    ImageLoader.evictFromCache(ImageRequest.Url(it))
                                }
                                withContext(Dispatchers.Main) {
                                    Toast
                                        .makeText(
                                            context,
                                            "Cleared cached images",
                                            Toast.LENGTH_SHORT
                                        )
                                        .show()
                                }
                            }
                        },
                    ) {
                        Text("Clear cached images")
                    }
                }
            }
        },
        bottomBar = {
            val items = remember(uiState.post?.type, uiState.hasComments) {
                val basicItems = when (uiState.post?.type) {
                    Post.Type.Comic -> {
                        mutableListOf(
                            JumpToPageItem,
                            ThemeItem,
                            BookmarksItem,
                        )
                    }

                    else -> {
                        mutableListOf(
                            TextStyleItem,
                            ThemeItem,
                            SectionsItem,
                        )
                    }
                }
                if (uiState.hasComments) {
                    basicItems.add(CommentsItem)
                }
                basicItems.toList().onEach { it.isSelected = false }
            }
            val moreItems = remember(uiState.post?.type) {
                val list = mutableListOf(
                    ShareItem,
                    CopyUrlItem,
                    OpenInBrowserItem,
                )
                if (uiState.post?.type == Post.Type.Comic) {
                    list.add(TextStyleItem)
                    list.add(ReversePagesItem)
                }
                if (uiState.post != null) {
                    list.add(FloatingPostItem)
                }
                list.add(GoToTopItem)
                list.onEach { it.isSelected = false }
            }
            val clipboardManager = LocalClipboardManager.current
            BottomBar(
                showDownload = uiState.images.isNotEmpty(),
                post = uiState.post,
                items = ImmutableHolder(items),
                moreItems = ImmutableHolder(moreItems),
                onItemClick = { item ->
                    when (item) {
                        GoToTopItem -> {
                            scope.launch {
                                scrollableState.listState.scrollToItem(0)
                            }
                        }

                        JumpToPageItem -> {
                            if (uiState.contentElements.isNotEmpty()) {
                                jumpToPageDialogVisible = true
                            }
                        }

                        TextStyleItem -> {
                            scope.launch {
                                textStyleSheetState.expand()
                            }
                        }

                        ThemeItem -> {
                            scope.launch {
                                themeSettingsSheetState.expand()
                            }
                        }

                        BookmarksItem -> {
                            scope.launch {
                                drawerState.open(ROUTE_BOOKMARKS)
                            }
                        }

                        SectionsItem -> {
                            scope.launch {
                                drawerState.open(ROUTE_SECTIONS)
                            }
                        }

                        CommentsItem -> {
                            scope.launch {
                                commentsSheetState.peek()
                            }
                        }

                        ShareItem -> {
                            val post = uiState.post
                            if (post != null) {
                                Intents.shareText(
                                    context = context,
                                    text = "${post.title} (${post.url})",
                                )
                            }
                        }

                        OpenInBrowserItem -> {
                            if (postUrl != null) {
                                Intents.openInBrowser(context, postUrl)
                            }
                        }

                        CopyUrlItem -> {
                            if (postUrl != null) {
                                clipboardManager.setText(AnnotatedString(postUrl))
                            }
                        }

                        ReversePagesItem -> {
                            val reversed = !uiState.reversedPages
                            viewModel.reversePages(reversed)
                            item.isSelected = reversed
                        }

                        FloatingPostItem -> {
                            val post = uiState.post
                            val listState = scrollableState.listState
                            val index = listState.firstVisibleItemIndex -
                                    calculateContentOffset(post)
                            val scrollOffset = listState.firstVisibleItemScrollOffset
                            if (post != null) {
                                val readingPost = ReadingPost.fromPost(
                                    post = post.raw,
                                    source = uiState.service?.name,
                                    elementIndex = index,
                                    elementScrollOffset = scrollOffset,
                                )
                                val activity = context as Activity
                                if (Permissions.checkOrRequestFloatingPermission(activity)) {
                                    ReadingBubbleService.addPost(context, readingPost)
                                    onNavigate(NavEvent.Back)
                                }
                            }
                        }

                        else -> {}
                    }
                },
                modifier = Modifier.onSizeChanged {
                    bottomBarHeight = with(density) { it.height.toDp() }
                },
                moreItemsEnabled = !postUrl.isNullOrEmpty(),
                menuIconDecoration = { item ->
                    val commentCount = uiState.post?.commentCount ?: 0
                    if (item == CommentsItem && commentCount > 0) {
                        CommentCount(
                            count = commentCount,
                            modifier = Modifier
                                .widthIn(min = 24.dp)
                                .offset(6.dp, (-6).dp)
                                .align(Alignment.TopEnd),
                        )
                    }
                },
            )
        }
    ) {
        val pullRefreshState = rememberPullRefreshState(
            refreshing = uiState.isLoading,
            onRefresh = {
                viewModel.fetchPost(
                    serviceId = serviceId,
                    postUrl = postUrl,
                    networkPostOnly = true,
                )
            },
        )
        val indicatorOffset = rememberPullRefreshIndicatorOffset(state = pullRefreshState)

        Box(
            modifier = Modifier.pullRefresh(
                state = pullRefreshState,
                enabled = uiState.service != null,
            ),
        ) {
            PostContent(
                onNavigate = onNavigate,
                viewModel = viewModel,
                bookmarkViewModel = bookmarkViewModel,
                positionController = positionController,
                uiState = uiState,
                serviceId = serviceId,
                postUrl = postUrl,
                initialElementIndex = initialElementIndex,
                initialElementScrollOffset = initialElementScrollOffset,
                titleBarHeight = titleBarHeight,
                bottomBarHeight = bottomBarHeight,
                scrollableState = scrollableState,
                drawerState = drawerState,
                isRunningExitTransition = isRunningExitTransition,
                modifier = Modifier.offset {
                    IntOffset(0, indicatorOffset)
                },
            )

            ProgressPullRefreshIndicator(
                state = pullRefreshState,
                indicatorOffsetProvider = { indicatorOffset },
                isRefreshing = uiState.isLoading,
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
                loadingProgress = uiState.loadingProgress?.value,
            )
        }
    }

    TextStyleSheet(state = textStyleSheetState)

    ThemeSettingsSheet(state = themeSettingsSheetState)

    if (jumpToPageDialogVisible) {
        var page by remember { mutableStateOf("1") }
        EditDialog(
            onDismissRequest = { jumpToPageDialogVisible = false },
            value = page,
            onValueChange = { text -> page = text.filter { it.isDigit() } },
            title = { Text(stringResource(BaseR.string.jump_to_page) + " (1 - ${uiState.images.size})") },
            onConfirmClick = {
                val index = page.toInt() - 1
                if (index < 0 || index >= uiState.images.size) {
                    return@EditDialog
                }
                positionController.update(index)
            },
            keyboardType = KeyboardType.Number,
        )
    }

    val service = uiState.service
    val post = uiState.post
    if (service != null && post != null) {
        CommentsSheet(
            onNavigate = onNavigate,
            state = commentsSheetState,
            service = service,
            post = post,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun PostContent(
    onNavigate: (NavEvent) -> Unit,
    viewModel: PostViewModel,
    bookmarkViewModel: BookmarkViewModel,
    positionController: ImagePagerPositionController,
    uiState: PostUiState,
    serviceId: String?,
    postUrl: String?,
    initialElementIndex: Int,
    initialElementScrollOffset: Int,
    titleBarHeight: Dp,
    bottomBarHeight: Dp,
    scrollableState: LazyListScrollableState,
    drawerState: PostDrawerState,
    isRunningExitTransition: Boolean,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    val preferencesStore = context.preferencesStore()

    val fontScale by preferencesStore.postFontScale
        .asStateFlow(scope)
        .collectAsState()

    val lineSpacing by preferencesStore.postLineSpacingMultiplier
        .asStateFlow(scope)
        .collectAsState()

    var selectedImage: UiContentElement? by remember { mutableStateOf(null) }

    var indexToAddBookmark by remember { mutableStateOf(-1) }

    var longClickOffset by remember { mutableStateOf(Offset.Zero) }

    var isPreparingToShare by remember { mutableStateOf(false) }

    var shareImageJob by remember { mutableStateOf<Job?>(null) }

    val post = uiState.post

    var postToCollect by remember { mutableStateOf<UiPost?>(null) }

    var contentImages by remember(uiState.images) { mutableStateOf(uiState.images) }

    if (!uiState.isLoading && uiState.service == null) {
        NoServiceMatched(url = postUrl)
        return
    }

    LaunchedEffect(positionController, uiState.post?.url, uiState.sections) {
        positionController.position
            .collect { imageIndex ->
                if (imageIndex < 0) {
                    return@collect
                }

                val elements = uiState.contentElements
                var idx = -1
                // Find target element
                val elementIndex = elements.indexOfFirst { element ->
                    when (element) {
                        is UiContentElement.Image,
                        is UiContentElement.FullWidthImage -> {
                            idx++
                        }

                        is UiContentElement.Carousel -> {
                            idx += element.items.count { it.image != null }
                        }

                        else -> {}
                    }
                    imageIndex <= idx
                }
                if (elementIndex == -1) {
                    return@collect
                }

                val listState = scrollableState.listState
                val offset = calculateContentOffset(uiState.post)
                val targetIndex = offset + elementIndex
                if (targetIndex !in listState.itemRange) {
                    return@collect
                }
                if (targetIndex in listState.visibleItemRange) {
                    return@collect
                }
                listState.scrollToItem(targetIndex)
            }
    }

    LaunchedEffect(isRunningExitTransition, uiState.post, uiState.sections) {
        if (!isRunningExitTransition) {
            return@LaunchedEffect
        }
        // Save the read position
        uiState.post?.run {
            val first = scrollableState.listState.firstVisibleItemIndex
            val offset = calculateContentOffset(this)
            val readPos = max(-1, first - offset)
            viewModel.savePost(
                serviceId = this.serviceId,
                url = this.url,
                update = { post ->
                    if (readPos > 0) {
                        post.copy(
                            lastReadAt = System.currentTimeMillis(),
                            readPosition = readPos,
                        )
                    } else {
                        post.copy(
                            lastReadAt = System.currentTimeMillis(),
                        )
                    }
                },
            )
        }
        // Reset list position
        positionController.update(-1)
    }

    LaunchedEffect(post?.url, post?.serviceId) {
        if (uiState.post != null) {
            val p = ReadingPost.fromPost(uiState.post.raw)
            ReadingBubbleService.removePost(p)
        }
    }

    LaunchedEffect(
        uiState.contentElements.isNotEmpty(),
        initialElementIndex,
        initialElementScrollOffset,
    ) {
        if (uiState.contentElements.isEmpty() || initialElementIndex < 0) {
            return@LaunchedEffect
        }
        snapshotFlow { scrollableState.listState.layoutInfo }
            .filter { it.totalItemsCount > initialElementIndex }
            .first()
        scrollableState.listState.scrollToItem(
            index = initialElementIndex + calculateContentOffset(uiState.post),
            scrollOffset = initialElementScrollOffset,
        )
    }

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.clearError()
            isPreparingToShare = false
        }
    }

    val currentTextStyle = LocalTextStyle.current

    val contentTextStyle = remember(currentTextStyle, lineSpacing) {
        currentTextStyle.copy(lineHeight = lineSpacing.em)
    }

    CompositionLocalProvider(
        LocalTextStyle provides contentTextStyle,
        LocalFontScale provides fontScale,
    ) {
        LazyColumn(
            state = scrollableState.listState,
            modifier = modifier
                .fillMaxSize()
                .verticalScrollBar(
                    state = scrollableState,
                    padding = PaddingValues(
                        top = WindowInsets.statusBars
                            .asPaddingValues()
                            .calculateTopPadding(),
                        bottom = bottomBarHeight,
                    ),
                )
                .detectPointerPositionChanges(
                    key = postUrl,
                    onPositionChanged = { longClickOffset = it },
                    onDown = null,
                    onGestureEnd = null,
                ),
            contentPadding = PaddingValues(top = titleBarHeight, bottom = bottomBarHeight),
        ) {
            postHeader(
                onNavigate = onNavigate,
                positionController = positionController,
                uiState = uiState,
                service = uiState.service,
            )

            // Elements
            uiState.contentElements.forEachIndexed { index, element ->
                val imageUrl: String? = when (element) {
                    is UiContentElement.Image -> element.url
                    is UiContentElement.FullWidthImage -> element.url
                    is UiContentElement.Carousel -> element.items.firstOrNull()?.image
                    else -> null
                }
                val imageIndex = if (imageUrl == null) {
                    -1
                } else if (uiState.reversedPages) {
                    uiState.images.size - uiState.images.indexOf(imageUrl)
                } else {
                    uiState.images.indexOf(imageUrl)
                }

                contentElementItem(
                    onDetectedPicSize = {},
                    onClick = {
                        when (element) {
                            is UiContentElement.Section -> {
                                scope.launch {
                                    drawerState.open(destination = ROUTE_SECTIONS)
                                }
                            }

                            else -> {}
                        }
                    },
                    onLinkClick = {
                        scope.launch {
                            if (viewModel.canHandleUrlInApp(it)) {
                                onNavigate(navPushEvent(Routes.post(url = it, serviceId = null)))
                            } else {
                                Intents.openInBrowser(context, it)
                            }
                        }
                    },
                    onImageClick = { images, url ->
                        val currImages = images ?: contentImages
                        val imageIdx = currImages.indexOf(url)
                        val navEvent = NavEvent.PushImagePager(
                            route = Routes.imagePager(
                                title = post!!.title,
                                currPage = imageIdx.coerceIn(0, currImages.size),
                            ),
                            images = currImages,
                        )
                        onNavigate(navEvent)
                    },
                    onImageLongClick = { images, _ ->
                        contentImages = images ?: uiState.images
                        selectedImage = element
                    },
                    onAddToBookmarkClick = { indexToAddBookmark = index },
                    imageIndex = imageIndex,
                    showIndexOnFullWidthImage = post?.type == Post.Type.Comic,
                    element = element,
                )
            }
        }
    }

    if (indexToAddBookmark != -1 && post != null) {
        AddBookmarkDialog(
            onDismissRequest = { indexToAddBookmark = -1 },
            onAskToCollect = { postToCollect = post },
            post = post,
            elementIndex = Snapshot.withoutReadObservation { indexToAddBookmark },
            currentBookmarks = bookmarkViewModel.bookmarkUiState.collectAsState().value.bookmarks,
            onLoadRequest = { bookmarkViewModel.loadBookmarks(post.serviceId, post.url) },
            onAddBookmark = { bookmarkViewModel.addBookmark(it) },
            onRemoveBookmark = { bookmarkViewModel.removeBookmark(it) },
        )
    }

    if (postToCollect != null) {
        val p = postToCollect!!
        AddToCollectionsDialog(
            onCollect = { viewModel.collectPost(p) },
            onDismissRequest = { postToCollect = null },
            post = p,
        )
    }

    val selectedImageUrl = when (val image = selectedImage) {
        is UiContentElement.Image -> image.url
        is UiContentElement.FullWidthImage -> image.url
        else -> null
    }
    if (selectedImageUrl != null) {
        val density = LocalDensity.current
        PostImageOptionsPopup(
            onDismissRequest = { selectedImage = null },
            onAddToBookmarkClick = {
                indexToAddBookmark = uiState.contentElements.indexOf(selectedImage)
            },
            onShareStarted = {
                shareImageJob = it
                isPreparingToShare = true
            },
            onShareFinished = {
                shareImageJob = null
                isPreparingToShare = false
            },
            postTitle = post?.title,
            selectedImage = selectedImageUrl,
            contentImages = StableHolder(contentImages),
            isReversed = uiState.reversedPages,
            isShareEnabled = !Snapshot.withoutReadObservation { isPreparingToShare },
            offset = Snapshot.withoutReadObservation { longClickOffset }.toDpOffset(density),
        )
    }

    MessagePopup(
        visible = isPreparingToShare,
        offset = DpOffset(0.dp, -bottomBarHeight),
        onDismissed = {},
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val iconTint = MaterialTheme.colors.onPrimary.copy(alpha = LocalContentAlpha.current)
            Icon(
                painter = painterResource(CommonUiR.drawable.ic_baseline_share_24),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = iconTint,
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = stringResource(BaseR.string.preparing_to_share),
                modifier = Modifier.weight(weight = 1f, fill = false),
                color = iconTint,
            )

            Spacer(modifier = Modifier.size(8.dp))

            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(bounded = false),
                        onClick = {
                            shareImageJob?.cancel()
                            isPreparingToShare = false
                        }
                    ),
                tint = iconTint,
            )
        }
    }

    var error by remember { mutableStateOf(Throwable()) }
    var showError by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            error = uiState.error
            showError = true
        } else {
            showError = false
        }
    }

    MessagePopup(
        visible = showError,
        offset = DpOffset(0.dp, -bottomBarHeight),
        backgroundColor = MaterialTheme.colors.error,
        swipeable = true,
        onDismissed = { viewModel.clearError() },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Html(
                html = stringResource(
                    BaseR.string._load_failed_with_message,
                    error.message ?: "Unknown error: $error"
                ),
                onLinkClick = { Intents.openInBrowser(context, it) },
                modifier = Modifier.weight(1f),
                style = RichTextStyle.Default.copy(linkColor = MaterialTheme.colors.secondary),
                color = MaterialTheme.colors.onError,
                fontSize = 14.sp,
                maxBlocks = 2,
                blockMaxLines = 3,
                blockTextOverflow = TextOverflow.Ellipsis,
            )

            TextButton(
                onClick = {
                    viewModel.fetchPost(
                        serviceId = serviceId,
                        postUrl = postUrl,
                        networkPostOnly = true
                    )
                },
            ) {
                Text(
                    text = stringResource(BaseR.string.reload),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onError,
                )
            }
        }
    }
}

private fun LazyListScope.postHeader(
    onNavigate: (NavEvent) -> Unit,
    positionController: ImagePagerPositionController,
    uiState: PostUiState,
    service: UiServiceManifest?,
) {
    val post = uiState.post ?: return
    // Details header
    item(
        key = "header",
        contentType = PostContentItemType.Header,
    ) {
        Header(
            post = post,
            service = service,
            uiState = uiState,
            onContinueReadingClick = {
                val readPos = uiState.post.readPosition
                if (readPos > 0 && readPos < uiState.images.size) {
                    positionController.update(readPos)
                }
            },
            onSearchTextRequest = {
                if (service != null) {
                    onNavigate(navPushEvent(Routes.search(serviceId = service.id, query = it)))
                }
            },
            onUserClick = {
                val userId = post.authorId
                if (userId != null) {
                    val route = Routes.userProfile(serviceId = post.serviceId, userId = userId)
                    onNavigate(navPushEvent(route))
                }
            },
        )
    }
}

private fun calculateContentOffset(post: UiPost?): Int {
    return if (post != null) 1 else 0
}

@Composable
private fun NoServiceMatched(
    url: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    EmojiEmptyContent(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            SelectionContainer {
                Text(
                    text = stringResource(BaseR.string._no_service_found_for_url, url ?: ""),
                    fontSize = 18.sp,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (url != null) {
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { Intents.openInBrowser(context, url) },
                ) {
                    Text(stringResource(BaseR.string.open_in_browser))
                }
            }
        }
    }
}
