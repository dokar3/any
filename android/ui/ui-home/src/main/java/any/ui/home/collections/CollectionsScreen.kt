package any.ui.home.collections

import any.base.R as BaseR
import any.ui.common.R as CommonUiR
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import any.base.compose.StableHolder
import any.base.compose.rememberProvider
import any.base.prefs.FolderViewType
import any.base.prefs.PostSorting
import any.base.util.Intents
import any.base.util.urlDecode
import any.base.util.urlEncode
import any.data.entity.Folder
import any.domain.entity.UiPost
import any.navigation.NavEvent
import any.navigation.Routes
import any.navigation.navPushEvent
import any.navigation.navigateToMedia
import any.navigation.navigateToPost
import any.navigation.navigateToUser
import any.navigation.popBackStackUtil
import any.navigation.settings
import any.ui.comments.CommentsSheet
import any.ui.common.dialog.AddToCollectionsDialog
import any.ui.common.dialog.AddToFolderDialog
import any.ui.common.lazy.rememberLazyGridScrollableState
import any.ui.common.menu.FolderOptionMenu
import any.ui.common.menu.PostOptionMenu
import any.ui.common.quickScrollToTop
import any.ui.common.theme.topBarBackground
import any.ui.common.widget.EditDialog
import any.ui.common.widget.QuickReturnScreen
import any.ui.common.widget.RoundedTabIndicator
import any.ui.common.widget.SearchBar
import any.ui.common.widget.SimpleDialog
import any.ui.common.widget.TooltipBox
import any.ui.common.widget.rememberQuickReturnScreenState
import any.ui.home.HomeScrollToTopManager
import any.ui.home.ScrollToTopResponder
import any.ui.home.SettingsButton
import any.ui.home.TitleBar
import any.ui.home.collections.viewmodel.CollectionsViewModel
import any.ui.home.collections.viewmodel.SelectableTag
import com.dokar.sheets.BottomSheet
import com.dokar.sheets.PeekHeight
import com.dokar.sheets.rememberBottomSheetState
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import kotlinx.coroutines.launch

private const val ROUTE = "collections?folder={folder}"

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
internal fun CollectionsScreen(
    onNavigate: (NavEvent) -> Unit,
    titleBarHeight: Dp,
    bottomBarHeight: Dp,
    onBottomBarOffsetUpdate: (Int) -> Unit,
    scrollToTopManager: HomeScrollToTopManager,
    modifier: Modifier = Modifier,
    viewModel: CollectionsViewModel = viewModel(
        factory = CollectionsViewModel.Factory(LocalContext.current),
    ),
    tagSelectionHeight: Dp = 48.dp,
) {
    val topInset = WindowInsets.statusBars
        .asPaddingValues()
        .calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()


    val listPadding = PaddingValues(
        top = topInset + titleBarHeight + tagSelectionHeight,
        bottom = bottomBarHeight + bottomInset,
    )

    val uiState by viewModel.collectionsUiState.collectAsState()

    val scope = rememberCoroutineScope()

    val moreMenuSheet = rememberBottomSheetState()

    val gridStateProvider = rememberProvider { LazyGridState() }

    val scrollableState = rememberLazyGridScrollableState(gridStateProvider)

    val screenState = rememberQuickReturnScreenState(scrollableState)

    val isSelectionEnabled = uiState.isMultiSelectionEnabled()

    val currentFolder = uiState.currentFolderUiState.folder
    val handleBack = !currentFolder.isRoot() || uiState.isMultiSelectionEnabled()
    BackHandler(enabled = handleBack) {
        if (!currentFolder.isRoot()) {
            viewModel.gotoParentFolder()
        } else if (uiState.isMultiSelectionEnabled()) {
            viewModel.finishMultiSelection()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.loadCollectedPosts()
    }

    LaunchedEffect(uiState.isMultiSelectionEnabled()) {
        if (uiState.isMultiSelectionEnabled()) {
            screenState.resetBars()
        }
    }

    DisposableEffect(scrollToTopManager) {
        val handler = ScrollToTopResponder {
            scope.launch {
                screenState.resetBars()
                gridStateProvider.get().quickScrollToTop()
            }
        }
        scrollToTopManager.addHandler(handler)
        onDispose {
            scrollToTopManager.removeHandler(handler)
        }
    }

    QuickReturnScreen(
        state = screenState,
        bottomBarHeight = listPadding.calculateBottomPadding(),
        topBar = {
            TitleBar(
                height = titleBarHeight,
                startActionButton = {
                    SettingsButton(
                        onClick = { onNavigate(navPushEvent(Routes.settings())) }
                    )
                },
                backgroundColor = MaterialTheme.colors.topBarBackground,
            ) {
                CompositionLocalProvider(
                    LocalTextStyle provides LocalTextStyle.current.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                    )
                ) {
                    SearchBar(
                        text = uiState.filterText,
                        onValueChange = {
                            viewModel.updateSearchFilter(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = stringResource(
                                    BaseR.string.search_in_your_collections
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }

            Box(modifier = Modifier.height(IntrinsicSize.Min)) {
                TagSelection(
                    tags = StableHolder(uiState.currentFolderUiState.tags),
                    onShowAllTagsClick = {
                        scope.launch {
                            moreMenuSheet.expand()
                        }
                    },
                    onUpdateTags = {
                        viewModel.updateTags(it)
                    },
                    onRemoveTagFromPosts = {
                        viewModel.removeTagFromCurrentPosts(it)
                    },
                    modifier = Modifier
                        .height(tagSelectionHeight)
                        .background(MaterialTheme.colors.topBarBackground)
                        .alpha(if (isSelectionEnabled) 0f else 1f),
                )
                if (uiState.isMultiSelectionEnabled()) {
                    PostSelectionPanel(
                        selectedPosts = StableHolder(uiState.selectedPosts),
                        onFinishSelectionRequest = {
                            viewModel.finishMultiSelection()
                        },
                        onRemoveSelected = {
                            viewModel.removeSelectedFromCollections()
                        },
                        onAddSelectedToFolder = {
                            viewModel.addSelectedToFolder(it)
                        },
                        modifier = Modifier
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {},
                            ),
                    )
                }
            }
        },
        bottomBar = { onBottomBarOffsetUpdate(it) },
        fixedTopBar = isSelectionEnabled,
        modifier = modifier,
    ) {
        var selectedPost: UiPost? by remember { mutableStateOf(null) }

        var selectedFolder: Folder? by remember { mutableStateOf(null) }

        var folderToRename: Folder? by remember { mutableStateOf(null) }

        var folderToUnfold: Folder? by remember { mutableStateOf(null) }

        var postToAddToFolder: UiPost? by remember { mutableStateOf(null) }

        var postToAddToCollections: UiPost? by remember { mutableStateOf(null) }

        var postToLoadComments: UiPost? by remember { mutableStateOf(null) }

        val commentsSheetState = rememberBottomSheetState()

        val subNavController = rememberAnimatedNavController()

        val previousFolder = uiState.previousFolderUiState.folder

        LaunchedEffect(subNavController) {
            // Fix a weird behavior that the nav controller may keep blocking back press events
            // even if there are no back entries to pop back, this happened when reentering
            // from another screen.
            subNavController.enableOnBackPressed(false)
        }

        LaunchedEffect(previousFolder.path, currentFolder.path) {
            if (previousFolder.path == currentFolder.path) {
                // Not a navigate event, pass it.
                return@LaunchedEffect
            }

            val currentEntry = subNavController.currentBackStackEntry
            val currentNavFolder = currentEntry?.arguments?.getString("folder")
            if (currentFolder.path == currentNavFolder) {
                // Already in the current folder, but this effect will get executed after
                // reentering from another screen, so we just pass it.
                return@LaunchedEffect
            }

            // Current folder has changed and the data is ready, navigate to next folder
            screenState.resetBars()
            val route = ROUTE.replace("{folder}", currentFolder.path.urlEncode())
            if (currentFolder.isTheSameOrSubFolder(previousFolder)) {
                subNavController.popBackStackUtil { entry ->
                    val entryFolder = entry.arguments?.getString("folder")?.urlDecode()
                    val isRoot = entryFolder == null && currentFolder.isRoot()
                    val isTarget = entryFolder == currentFolder.path
                    isRoot || isTarget
                }
            } else {
                subNavController.navigate(route)
            }
        }

        val animSpec = remember { tween<Float>(easing = FastOutSlowInEasing) }

        AnimatedNavHost(
            navController = subNavController,
            startDestination = ROUTE,
        ) {
            composable(
                route = ROUTE,
                arguments = listOf(
                    navArgument(name = "folder") {
                        nullable = true
                        type = NavType.StringType
                    },
                ),
                enterTransition = {
                    scaleIn(
                        initialScale = 0.8f,
                        animationSpec = animSpec
                    ) + fadeIn(
                        animationSpec = animSpec
                    )
                },
                exitTransition = {
                    scaleOut(
                        targetScale = 1.2f,
                        animationSpec = animSpec
                    ) + fadeOut(
                        animationSpec = animSpec
                    )
                },
                popEnterTransition = {
                    scaleIn(
                        initialScale = 1.2f,
                        animationSpec = animSpec
                    ) + fadeIn(
                        animationSpec = animSpec
                    )
                },
                popExitTransition = {
                    scaleOut(
                        targetScale = 0.8f,
                        animationSpec = animSpec
                    ) + fadeOut(
                        animationSpec = animSpec
                    )
                },
            ) { backStackEntry ->
                val context = LocalContext.current

                val folderPath = backStackEntry.arguments
                    ?.getString("folder")
                    ?.urlDecode() ?: Folder.ROOT.path

                val currentPath = uiState.currentFolderUiState.folder.path
                val isCurrentList = folderPath == currentPath
                val folderUiState = if (isCurrentList) {
                    uiState.currentFolderUiState
                } else {
                    uiState.previousFolderUiState
                }

                // Create a new LazyGridState for each composable to remember
                // the scroll position of folder
                val gridState = rememberLazyGridState()
                gridStateProvider.provide(gridState)

                CollectionList(
                    state = gridState,
                    folder = folderUiState.folder,
                    viewType = folderUiState.viewType,
                    folders = StableHolder(folderUiState.folders),
                    posts = StableHolder(folderUiState.posts),
                    selectedPosts = StableHolder(uiState.selectedPosts),
                    isLoading = uiState.previousFolderUiState.isLoading ||
                            uiState.currentFolderUiState.isLoading,
                    contentPadding = listPadding,
                    onMediaClick = { post, index ->
                        navigateToMedia(onNavigate, context, post.raw, index)
                    },
                    onUserClick = { serviceId, userId ->
                        navigateToUser(onNavigate, serviceId, userId)
                    },
                    onCommentsClick = {
                        postToLoadComments = it
                        scope.launch { commentsSheetState.peek() }
                    },
                    onLinkClick = { Intents.openInBrowser(context, it) },
                    onPostClick = {
                        val isSelected = uiState.selectedPosts.contains(it)
                        if (uiState.isMultiSelectionEnabled()) {
                            if (isSelected) {
                                viewModel.removeFromSelection(it)
                            } else {
                                viewModel.addToSelection(it)
                            }
                        } else {
                            navigateToPost(onNavigate, context, it.raw)
                        }
                    },
                    onPostLongClick = {
                        val isSelected = uiState.selectedPosts.contains(it)
                        if (uiState.isMultiSelectionEnabled()) {
                            if (isSelected) {
                                viewModel.removeFromSelection(it)
                            } else {
                                viewModel.addToSelection(it)
                            }
                        } else {
                            selectedPost = it
                        }
                    },
                    onFolderClick = { viewModel.loadPostsForNextFolder(it) },
                    onFolderLongClick = {
                        if (!uiState.isMultiSelectionEnabled()) {
                            selectedFolder = it
                        }
                    },
                    modifier = Modifier.testTag("collectionList"),
                )
            }
        }

        if (selectedPost != null) {
            val post = selectedPost!!
            PostOptionMenu(
                post = post,
                showMultiSelectionItem = true,
                onDiscardRequest = { viewModel.discardPost(post) },
                onAddToCollectionsClick = { postToAddToCollections = post },
                onDismiss = { selectedPost = null },
                onAddToFolderClick = { postToAddToFolder = post },
                onMultiSelectionClick = { viewModel.startMultiSelection(post) },
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

        if (postToAddToCollections != null) {
            val post = postToAddToCollections!!
            AddToCollectionsDialog(
                onDismissRequest = { postToAddToCollections = null },
                onCollect = { viewModel.collectPost(it) },
                post = post,
            )
        }

        if (selectedFolder != null) {
            val tempFolder = selectedFolder!!
            FolderOptionMenu(
                folderName = tempFolder.path,
                onDismissRequest = { selectedFolder = null },
                onUnfoldClick = { folderToUnfold = tempFolder },
                onRenameClick = { folderToRename = tempFolder },
            )
        }

        if (folderToRename != null) {
            val tempFolder = folderToRename!!
            var name by remember { mutableStateOf(tempFolder.path) }
            EditDialog(
                onDismissRequest = { folderToRename = null },
                value = name,
                onValueChange = { name = it },
                title = { Text(stringResource(BaseR.string.rename_folder)) },
                onConfirmClick = { viewModel.renameFolder(tempFolder, name) },
                acceptEmpty = false,
            )
        }

        if (folderToUnfold != null) {
            val tempFolder = folderToUnfold!!
            SimpleDialog(
                onDismissRequest = {
                    folderToUnfold = null
                },
                title = {
                    Text(stringResource(BaseR.string._unfold, tempFolder.path))
                },
                text = {
                    Text(stringResource(BaseR.string.unfold_folder_alert))
                },
                confirmText = {
                    Text(stringResource(android.R.string.ok))
                },
                cancelText = {
                    Text(stringResource(android.R.string.cancel))
                },
                onConfirmClick = {
                    viewModel.unfoldFolder(tempFolder)
                },
            )
        }

        if (postToLoadComments != null) {
            CommentsSheet(
                onNavigate = onNavigate,
                state = commentsSheetState,
                service = null,
                post = postToLoadComments!!
            )
        }
    }

    BottomSheet(
        state = moreMenuSheet,
        modifier = Modifier
            .heightIn(max = 480.dp)
            .fillMaxHeight(),
        peekHeight = PeekHeight.fraction(1f),
    ) {
        MoreMenuContent(
            tags = StableHolder(uiState.currentFolderUiState.tags),
            currentSorting = uiState.sorting,
            currentViewType = uiState.currentFolderUiState.viewType,
            onUpdateTags = { viewModel.updateTags(it) },
            onRemoveTagFromPosts = { viewModel.removeTagFromCurrentPosts(it) },
            onSelectSorting = { viewModel.setSorting(it) },
            onSelectViewType = { viewType, applyToAllFolders ->
                viewModel.setFolderViewType(
                    folder = uiState.currentFolderUiState.folder,
                    viewType = viewType,
                    applyToAllFolders = applyToAllFolders,
                )
            },
        )
    }
}

@Composable
private fun MoreMenuContent(
    tags: StableHolder<List<SelectableTag>>,
    currentSorting: PostSorting,
    currentViewType: FolderViewType,
    onUpdateTags: (List<SelectableTag>) -> Unit,
    onRemoveTagFromPosts: (SelectableTag) -> Unit,
    onSelectSorting: (PostSorting) -> Unit,
    onSelectViewType: (viewType: FolderViewType, applyToAllFolders: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(
                horizontal = 22.dp,
                vertical = 16.dp
            )
    ) {
        val context = LocalContext.current
        val tabs = remember(tags.value.size) {
            listOf(
                context.resources.getString(BaseR.string._tag_with_count, tags.value.size),
                context.resources.getString(BaseR.string.view_type),
                context.resources.getString(BaseR.string.sorting),
            )
        }

        var selectedTabIndex by remember { mutableStateOf(0) }
        var previousTabIndex by remember { mutableStateOf(0) }

        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color.Transparent,
            indicator = {
                RoundedTabIndicator(
                    selectedTabIndex = selectedTabIndex,
                    tabPositions = it,
                    indicatorHeight = 36.dp,
                )
            },
            divider = {},
        ) {
            for (i in tabs.indices) {
                val tab = tabs[i]
                Tab(
                    selected = i == selectedTabIndex,
                    onClick = {
                        previousTabIndex = selectedTabIndex
                        selectedTabIndex = i
                    },
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .clip(MaterialTheme.shapes.small),
                ) {
                    Text(
                        text = tab,
                        modifier = Modifier.padding(8.dp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        var firstIntro by remember { mutableStateOf(true) }

        when (selectedTabIndex) {
            0 -> {
                val state = remember {
                    MutableTransitionState(firstIntro).apply {
                        targetState = true
                        firstIntro = false
                    }
                }
                AnimatedVisibility(
                    visibleState = state,
                    enter = slideInHorizontally() + fadeIn(),
                    exit = slideOutHorizontally() + fadeOut(),
                ) {
                    TagSelection(
                        tags = tags,
                        onShowAllTagsClick = {},
                        onUpdateTags = onUpdateTags,
                        onRemoveTagFromPosts = onRemoveTagFromPosts,
                        modifier = Modifier.heightIn(max = 380.dp),
                        tagsLayout = TagsLayout.Flow,
                    )
                }
            }

            1 -> {
                val state = remember {
                    MutableTransitionState(false).apply {
                        targetState = true
                    }
                }
                AnimatedVisibility(
                    visibleState = state,
                    enter = if (previousTabIndex == 0) {
                        slideInHorizontally { it / 2 } + fadeIn()
                    } else {
                        slideInHorizontally { -it / 2 } + fadeIn()
                    },
                    exit = slideOutHorizontally { it / 2 } + fadeOut(),
                ) {
                    FolderViewTypes(
                        currentViewType = currentViewType,
                        onSelectViewType = onSelectViewType,
                    )
                }
            }

            else -> {
                val state = remember {
                    MutableTransitionState(false).apply {
                        targetState = true
                    }
                }
                AnimatedVisibility(
                    visibleState = state,
                    enter = slideInHorizontally { it / 2 } + fadeIn(),
                    exit = slideOutHorizontally { it / 2 } + fadeOut(),
                ) {
                    CollectionsSorting(
                        currentSorting = currentSorting,
                        onSelectSorting = onSelectSorting,
                    )
                }
            }
        }
    }
}

@Composable
private fun PostSelectionPanel(
    selectedPosts: StableHolder<Set<UiPost>>,
    onFinishSelectionRequest: () -> Unit,
    onRemoveSelected: () -> Unit,
    onAddSelectedToFolder: (Folder) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showRemovalDialog by remember { mutableStateOf(false) }
    var showAddToFolderDialog by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { onFinishSelectionRequest() }
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(BaseR.string.cancel_selection),
            )
        }

        Text(
            text = stringResource(BaseR.string._selected, selectedPosts.value.size),
            modifier = Modifier.weight(1f)
        )

        Row {
            TooltipBox(text = stringResource(BaseR.string.remove_from_collections)) {
                IconButton(
                    onClick = { showRemovalDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(BaseR.string.remove_from_collections),
                        tint = MaterialTheme.colors.error,
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            TooltipBox(text = stringResource(BaseR.string.add_to_folder)) {
                IconButton(
                    onClick = { showAddToFolderDialog = true }
                ) {
                    Icon(
                        painter = painterResource(
                            CommonUiR.drawable.ic_outline_create_new_folder_24
                        ),
                        contentDescription = stringResource(BaseR.string.add_to_folder),
                    )
                }
            }
        }
    }

    if (showRemovalDialog) {
        SimpleDialog(
            onDismissRequest = { showRemovalDialog = false },
            title = { Text(stringResource(BaseR.string.remove_selected)) },
            text = {
                Text(stringResource(BaseR.string.remove_selected_posts_from_collections_alert))
            },
            confirmText = {
                Text(
                    text = stringResource(BaseR.string.remove),
                    color = MaterialTheme.colors.error,
                )
            },
            cancelText = { Text(stringResource(android.R.string.cancel)) },
            onConfirmClick = onRemoveSelected,
        )
    }

    if (showAddToFolderDialog) {
        AddToFolderDialog(
            onDismissRequest = { showAddToFolderDialog = false },
            onFolderConfirm = { onAddSelectedToFolder(it) },
            post = null,
        )
    }
}
