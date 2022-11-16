package any.ui.post

import any.base.R as BaseR
import any.ui.common.R as CommonUiR
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import any.base.ImmutableHolder
import any.data.entity.Bookmark
import any.domain.post.ContentSection
import any.ui.common.widget.BottomSheetTitle
import any.ui.post.viewmodel.BookmarkUiState
import com.dokar.sheets.BottomSheet
import com.dokar.sheets.BottomSheetDefaults
import com.dokar.sheets.BottomSheetState
import com.dokar.sheets.rememberBottomSheetState
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController

internal const val ROUTE_SECTIONS = "post/sections"

internal const val ROUTE_BOOKMARKS = "post/bookmarks"

@Composable
internal fun rememberPostDrawerState(
    drawerState: BottomSheetState = rememberBottomSheetState(),
): PostDrawerState {
    return remember(drawerState) {
        PostDrawerState(sheetState = drawerState)
    }
}

private fun NavController.replace(route: String) {
    navigate(route) {
        popUpTo(currentDestination!!.id) {
            inclusive = true
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

internal class PostDrawerState(
    internal val sheetState: BottomSheetState,
) {
    private var navController: NavController? = null

    private var currentDestination: String? = null

    internal fun attachNavController(navController: NavController) {
        this.navController = navController
        currentDestination?.let { setNavDestination(it) }
    }

    internal fun detachNavController() {
        this.navController = null
    }

    suspend fun open(destination: String) {
        if (navController != null) {
            currentDestination = null
            setNavDestination(destination)
        } else {
            currentDestination = destination
        }
        sheetState.expand()
    }

    suspend fun close() {
        currentDestination = null
        sheetState.collapse()
    }

    private fun setNavDestination(destination: String) {
        val currentRoute = navController?.currentDestination?.route
        if (currentRoute != destination) {
            navController?.replace(destination)
        }
    }
}

@Composable
internal fun PostDrawer(
    state: PostDrawerState,
    bookmarkUiState: BookmarkUiState,
    sections: ImmutableHolder<List<ContentSection>>,
    onSectionClick: (index: Int, section: ContentSection) -> Unit,
    onLoadBookmarkRequest: () -> Unit,
    onBookmarkClick: (Bookmark) -> Unit,
    onRemoveBookmark: (Bookmark) -> Unit,
    modifier: Modifier = Modifier,
    startDestination: String = ROUTE_SECTIONS,
) {
    BottomSheet(
        state = state.sheetState,
        modifier = modifier.heightIn(max = 600.dp),
        skipPeek = true,
        behaviors = BottomSheetDefaults.dialogSheetBehaviors(
            extendsIntoNavigationBar = true,
        ),
    ) {
        PostDrawerContent(
            state = state,
            bookmarkUiState = bookmarkUiState,
            sections = sections,
            onSectionClick = onSectionClick,
            onLoadBookmarkRequest = onLoadBookmarkRequest,
            onBookmarkClick = onBookmarkClick,
            onRemoveBookmark = onRemoveBookmark,
            startDestination = startDestination,
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun PostDrawerContent(
    state: PostDrawerState,
    bookmarkUiState: BookmarkUiState,
    sections: ImmutableHolder<List<ContentSection>>,
    onSectionClick: (index: Int, section: ContentSection) -> Unit,
    onLoadBookmarkRequest: () -> Unit,
    onBookmarkClick: (Bookmark) -> Unit,
    onRemoveBookmark: (Bookmark) -> Unit,
    modifier: Modifier = Modifier,
    startDestination: String = ROUTE_SECTIONS,
    navigationBarBackground: Color = MaterialTheme.colors.background,
) {
    val navController = rememberAnimatedNavController()

    DisposableEffect(state, navController) {
        state.attachNavController(navController)
        onDispose { state.detachNavController() }
    }

    Column(modifier = modifier.fillMaxSize()) {
        AnimatedNavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            composable(
                route = ROUTE_SECTIONS,
                enterTransition = {
                    slideInHorizontally() + fadeIn()
                },
                exitTransition = {
                    slideOutHorizontally() + fadeOut()
                },
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    BottomSheetTitle(text = stringResource(BaseR.string.sections))

                    Sections(
                        sections = sections,
                        onSectionClick = onSectionClick,
                    )
                }
            }

            composable(
                route = ROUTE_BOOKMARKS,
                enterTransition = {
                    slideInHorizontally { it / 2 } + fadeIn()
                },
                exitTransition = {
                    slideOutHorizontally { it / 2 } + fadeOut()
                },
            ) {
                LaunchedEffect(Unit) {
                    onLoadBookmarkRequest()
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    BottomSheetTitle(text = stringResource(BaseR.string.bookmarks))

                    Bookmarks(
                        bookmarks = ImmutableHolder(bookmarkUiState.bookmarks),
                        onBookmarkClick = onBookmarkClick,
                        onRemoveBookmark = onRemoveBookmark,
                    )
                }
            }
        }

        BottomNavigation(
            modifier = Modifier
                .background(navigationBarBackground)
                .shadow(4.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            backgroundColor = navigationBarBackground,
            elevation = 0.dp,
        ) {
            val res = LocalContext.current.resources
            val items = remember {
                arrayOf(
                    PostDrawerNavItem(
                        title = res.getString(BaseR.string.sections),
                        iconRes = CommonUiR.drawable.ic_baseline_format_list_numbered_24,
                        route = ROUTE_SECTIONS,
                    ),
                    PostDrawerNavItem(
                        title = res.getString(BaseR.string.bookmarks),
                        iconRes = CommonUiR.drawable.ic_baseline_bookmark_border_24,
                        route = ROUTE_BOOKMARKS,
                    )
                )
            }
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            for (item in items) {
                val isSelected = currentRoute == item.route
                BottomNavigationItem(
                    selected = isSelected,
                    onClick = {
                        if (!isSelected) {
                            navController.replace(item.route)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    icon = {
                        Icon(
                            painter = painterResource(item.iconRes),
                            contentDescription = item.title,
                        )
                    },
                    label = {
                        Text(item.title)
                    },
                    selectedContentColor = MaterialTheme.colors.primary,
                    unselectedContentColor = MaterialTheme.colors.onBackground,
                )
            }
        }
    }
}

private data class PostDrawerNavItem(
    val title: String,
    val iconRes: Int,
    val route: String,
)