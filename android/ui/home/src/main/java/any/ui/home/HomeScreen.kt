package any.ui.home

import android.app.Activity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import any.base.compose.LocalBenchmarkBuild
import any.base.util.applyLightStatusBar
import any.base.util.clearLightStatusBar
import any.navigation.NavEvent
import any.navigation.Routes
import any.navigation.navigateAndReplace
import any.ui.common.TintSystemBars
import any.ui.common.theme.compositedNavigationBarColor
import any.ui.common.theme.compositedStatusBarColor
import any.ui.common.theme.sizes
import any.ui.common.theme.topBarBackground
import any.ui.common.widget.BarsColorController
import any.ui.common.widget.BoxWithSystemBars
import any.ui.common.widget.rememberBarsColorController
import any.ui.home.collections.CollectionsScreen
import any.ui.home.downloads.DownloadsScreen
import any.ui.home.following.FollowingScreen
import any.ui.home.fresh.FreshScreen

const val FADE_TRANS_DURATION = 275

internal fun interface ScrollToTopResponder {
    fun onRequestScrollToTop()
}

@Stable
internal class HomeScrollToTopManager {
    private val handlers = mutableListOf<ScrollToTopResponder>()

    fun addHandler(handler: ScrollToTopResponder) {
        handlers.add(handler)
    }

    fun removeHandler(handler: ScrollToTopResponder) {
        handlers.remove(handler)
    }

    fun requestScrollToTop() {
        for (handler in handlers) {
            handler.onRequestScrollToTop()
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(
    onNavigate: (NavEvent) -> Unit,
    initialDestination: String,
    darkMode: Boolean,
    modifier: Modifier = Modifier,
    titleBarHeight: Dp = MaterialTheme.sizes.titleBarHeight,
    bottomBarHeight: Dp = MaterialTheme.sizes.bottomBarHeight,
) {
    var bottomBarOffset by remember { mutableIntStateOf(0) }

    val scrollToTopManager = remember { HomeScrollToTopManager() }

    val compositedStatusBarColor = MaterialTheme.colors.compositedStatusBarColor
    val compositedNavigationBarColor = MaterialTheme.colors.compositedNavigationBarColor

    val barsColorController = rememberBarsColorController(
        statusBarColor = compositedStatusBarColor,
        navigationBarColor = compositedNavigationBarColor,
    )

    BoxWithSystemBars(
        barsColorController = barsColorController,
        modifier = modifier.fillMaxSize(),
    ) {
        val subNavController = rememberNavController()
        NavHost(
            navController = subNavController,
            startDestination = initialDestination,
        ) {
            freshScreen(
                onNavigate = onNavigate,
                barsColorController = barsColorController,
                statusBarColor = compositedStatusBarColor,
                darkMode = darkMode,
                titleBarHeight = { titleBarHeight },
                bottomBarHeight = { bottomBarHeight },
                onBottomBarOffsetUpdate = { bottomBarOffset = it },
                scrollToTopManager = scrollToTopManager,
            )

            followingScreen(
                onNavigate = onNavigate,
                darkMode = darkMode,
                titleBarHeight = { titleBarHeight },
                bottomBarHeight = { bottomBarHeight },
                onBottomBarOffsetUpdate = { bottomBarOffset = it },
                scrollToTopManager = scrollToTopManager,
                barsColorController = barsColorController,
            )

            collectionScreen(
                onNavigate = onNavigate,
                darkMode = darkMode,
                titleBarHeight = { titleBarHeight },
                bottomBarHeight = { bottomBarHeight },
                onBottomBarOffsetUpdate = { bottomBarOffset = it },
                scrollToTopManager = scrollToTopManager,
                barsColorController = barsColorController,
            )

            downloadScreen(
                onNavigate = onNavigate,
                darkMode = darkMode,
                titleBarHeight = { titleBarHeight },
                bottomBarHeight = { bottomBarHeight },
                onBottomBarOffsetUpdate = { bottomBarOffset = it },
                scrollToTopManager = scrollToTopManager,
                barsColorController = barsColorController,
            )
        }

        val currentRoute = subNavController.currentBackStackEntryAsState().value
            ?.destination
            ?.route ?: ""
        BottomBar(
            currentRoute = currentRoute,
            offsetProvider = if (LocalBenchmarkBuild.current) {
                { 0 }
            } else {
                { bottomBarOffset }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
            height = bottomBarHeight,
            onItemClick = { navItem, _ ->
                if (navItem.route != currentRoute) {
                    subNavController.navigateAndReplace(navItem.route)
                } else {
                    scrollToTopManager.requestScrollToTop()
                }
            }
        )
    }
}

@ExperimentalAnimationApi
private fun NavGraphBuilder.freshScreen(
    onNavigate: (NavEvent) -> Unit,
    barsColorController: BarsColorController,
    statusBarColor: Color,
    darkMode: Boolean,
    titleBarHeight: () -> Dp,
    bottomBarHeight: () -> Dp,
    onBottomBarOffsetUpdate: (Int) -> Unit,
    scrollToTopManager: HomeScrollToTopManager,
) {
    composable(
        route = Routes.Home.FRESH,
        enterTransition = {
            fadeIn(animationSpec = tween(durationMillis = FADE_TRANS_DURATION))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(durationMillis = FADE_TRANS_DURATION))
        },
    ) {
        val window = (LocalContext.current as Activity).window
        val view = LocalView.current
        LaunchedEffect(view) {
            // Override default xml attribute
            view.clearLightStatusBar(window)
        }

        TintSystemBars(
            darkMode = darkMode,
            statusBarColor = Color.Transparent,
            navigationBarColor = Color.Transparent,
            applyLightStatusBarAutomatically = false,
        )

        var lightStatusBar = remember { false }
        FreshScreen(
            onNavigate = onNavigate,
            titleBarHeight = titleBarHeight(),
            bottomBarHeight = bottomBarHeight(),
            onBottomBarOffsetUpdate = onBottomBarOffsetUpdate,
            statusBarColor = statusBarColor,
            changeStatusBarColor = {
                barsColorController.statusBarColor = it
                if (!darkMode) {
                    val light = it.alpha >= 0.4f
                    if (lightStatusBar != light) {
                        lightStatusBar = light
                        if (light) {
                            view.applyLightStatusBar(window)
                        } else {
                            view.clearLightStatusBar(window)
                        }
                    }
                }
            },
            isRunningTransitions = transition.isRunning,
            scrollToTopManager = scrollToTopManager,
        )
    }
}

@ExperimentalAnimationApi
private fun NavGraphBuilder.commonScreen(
    route: String,
    darkMode: Boolean,
    barsColorController: BarsColorController,
    content: @Composable (AnimatedVisibilityScope.(NavBackStackEntry) -> Unit)
) {
    composable(
        route = route,
        enterTransition = {
            fadeIn(animationSpec = tween(durationMillis = FADE_TRANS_DURATION))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(durationMillis = FADE_TRANS_DURATION))
        },
    ) {
        TintSystemBars(
            darkMode = darkMode,
            statusBarColor = Color.Transparent,
            navigationBarColor = Color.Transparent,
        )

        val statusBarColor = MaterialTheme.colors.topBarBackground
        LaunchedEffect(barsColorController, statusBarColor) {
            animate(
                typeConverter = Color.VectorConverter(statusBarColor.colorSpace),
                initialValue = barsColorController.statusBarColor,
                targetValue = statusBarColor,
            ) { color, _ ->
                barsColorController.statusBarColor = color
            }
        }

        content(it)
    }
}

@ExperimentalAnimationApi
private fun NavGraphBuilder.followingScreen(
    onNavigate: (NavEvent) -> Unit,
    darkMode: Boolean,
    titleBarHeight: () -> Dp,
    bottomBarHeight: () -> Dp,
    onBottomBarOffsetUpdate: (Int) -> Unit,
    scrollToTopManager: HomeScrollToTopManager,
    barsColorController: BarsColorController,
) {
    commonScreen(
        route = Routes.Home.FOLLOWING,
        darkMode = darkMode,
        barsColorController = barsColorController,
    ) {
        FollowingScreen(
            onNavigate = onNavigate,
            titleBarHeight = titleBarHeight(),
            bottomBarHeight = bottomBarHeight(),
            onBottomBarOffsetUpdate = onBottomBarOffsetUpdate,
            scrollToTopManager = scrollToTopManager,
        )
    }
}

@ExperimentalAnimationApi
private fun NavGraphBuilder.collectionScreen(
    onNavigate: (NavEvent) -> Unit,
    darkMode: Boolean,
    titleBarHeight: () -> Dp,
    bottomBarHeight: () -> Dp,
    onBottomBarOffsetUpdate: (Int) -> Unit,
    scrollToTopManager: HomeScrollToTopManager,
    barsColorController: BarsColorController,
) {
    commonScreen(
        route = Routes.Home.COLLECTIONS,
        darkMode = darkMode,
        barsColorController = barsColorController,
    ) {
        CollectionsScreen(
            onNavigate = onNavigate,
            titleBarHeight = titleBarHeight(),
            bottomBarHeight = bottomBarHeight(),
            onBottomBarOffsetUpdate = onBottomBarOffsetUpdate,
            scrollToTopManager = scrollToTopManager,
        )
    }
}

@ExperimentalAnimationApi
private fun NavGraphBuilder.downloadScreen(
    onNavigate: (NavEvent) -> Unit,
    darkMode: Boolean,
    titleBarHeight: () -> Dp,
    bottomBarHeight: () -> Dp,
    onBottomBarOffsetUpdate: (Int) -> Unit,
    scrollToTopManager: HomeScrollToTopManager,
    barsColorController: BarsColorController,
) {
    commonScreen(
        route = Routes.Home.DOWNLOADS,
        darkMode = darkMode,
        barsColorController = barsColorController,
    ) {
        DownloadsScreen(
            onNavigate = onNavigate,
            titleBarHeight = titleBarHeight(),
            bottomBarHeight = bottomBarHeight(),
            onBottomBarOffsetUpdate = onBottomBarOffsetUpdate,
            scrollToTopManager = scrollToTopManager,
        )
    }
}
