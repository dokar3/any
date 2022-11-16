package com.dokar.any

import android.app.Activity
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.dialog
import any.base.prefs.LAUNCH_SCREEN_COLLECTIONS
import any.base.prefs.LAUNCH_SCREEN_DOWNLOADS
import any.base.prefs.LAUNCH_SCREEN_FOLLOWING
import any.base.prefs.appPassword
import any.base.prefs.currentService
import any.base.prefs.launchScreen
import any.base.prefs.preferencesStore
import any.base.util.clearLightStatusBar
import any.navigation.ImagePagerArgs
import any.navigation.NavEvent
import any.navigation.PostArgs
import any.navigation.ProfileArgs
import any.navigation.Routes
import any.navigation.SearchArgs
import any.navigation.SettingsArgs
import any.navigation.imagePagerPage
import any.navigation.imagePagerTitle
import any.navigation.initialElementIndex
import any.navigation.initialElementScrollOffset
import any.navigation.navPushEvent
import any.navigation.postUrl
import any.navigation.query
import any.navigation.search
import any.navigation.serviceId
import any.navigation.subSettings
import any.navigation.userId
import any.navigation.userUrl
import any.ui.common.BoxWithSystemBars
import any.ui.common.TintSystemBars
import any.ui.common.rememberBarsColorController
import any.ui.home.FADE_TRANS_DURATION
import any.ui.home.HomeScreen
import any.ui.imagepager.ImagePager
import any.ui.imagepager.ImagePagerPositionController
import any.ui.imagepager.ImagePagerViewModel
import any.ui.password.PasswordScreen
import any.ui.post.PostScreen
import any.ui.profile.ProfileScreen
import any.ui.runsql.RunSqlScreen
import any.ui.search.SearchScreen
import any.ui.settings.SettingsScreen
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable

@OptIn(ExperimentalComposeUiApi::class, ExperimentalAnimationApi::class)
@Composable
fun AnimatedAppNavGraph(
    onNavigate: (NavEvent) -> Unit,
    onClearSecondDestination: () -> Unit,
    navController: NavHostController,
    darkMode: Boolean,
    secondDestination: SecondDestination?,
    modifier: Modifier = Modifier,
    skipPassword: Boolean = false,
    imagePagerViewModel: ImagePagerViewModel = viewModel(),
) {
    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    val preferencesStore = context.preferencesStore()

    val appPassword by preferencesStore.appPassword
        .asStateFlow(scope)
        .collectAsState()
    val passwordEnabled = remember { appPassword?.toIntOrNull() != null }

    val initialHomeDest: String = remember {
        when (preferencesStore.launchScreen.value) {
            LAUNCH_SCREEN_COLLECTIONS -> Routes.Home.COLLECTIONS
            LAUNCH_SCREEN_FOLLOWING -> Routes.Home.FOLLOWING
            LAUNCH_SCREEN_DOWNLOADS -> Routes.Home.DOWNLOADS
            else -> Routes.Home.FRESH
        }
    }

    val startDestination = if (passwordEnabled && !skipPassword) {
        Routes.PASSWORD
    } else {
        Routes.HOME
    }

    AnimatedNavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        passwordScreen(
            onNavigate = onNavigate,
            darkMode = darkMode,
            appPassword = appPassword,
        )

        homeScreen(
            onNavigate = onNavigate,
            onClearSecondDestination = onClearSecondDestination,
            initialDestination = initialHomeDest,
            darkMode = darkMode,
            secondDestination = secondDestination,
        )

        searchScreen(
            onNavigate = onNavigate,
            darkMode = darkMode,
        )

        postScreen(
            onNavigate = onNavigate,
            darkMode = darkMode,
            imagePagerViewModel = imagePagerViewModel,
        )

        profileScreen(
            onNavigate = onNavigate,
            darkMode = darkMode,
        )

        settingsScreen(
            onNavigate = onNavigate,
            darkMode = darkMode,
        )

        runSqlScreen(darkMode = darkMode)

        imagePager(
            onNavigate = onNavigate,
            imagePagerViewModel = imagePagerViewModel,
        )
    }
}

@ExperimentalAnimationApi
private fun NavGraphBuilder.passwordScreen(
    onNavigate: (NavEvent) -> Unit,
    darkMode: Boolean,
    appPassword: String?,
) {
    composable(route = Routes.PASSWORD) {
        TintSystemBars(darkMode = darkMode)

        PasswordScreen(
            onConfirm = { password ->
                val isCorrect = when (password) {
                    appPassword -> {
                        true
                    }

                    appPassword?.reversed() -> {
                        true
                    }

                    else -> false
                }

                if (isCorrect) {
                    onNavigate(NavEvent.ReplaceWith(Routes.HOME))
                }

                isCorrect
            }
        )
    }
}

@ExperimentalAnimationApi
private fun NavGraphBuilder.homeScreen(
    onNavigate: (NavEvent) -> Unit,
    onClearSecondDestination: () -> Unit,
    initialDestination: String,
    darkMode: Boolean,
    secondDestination: SecondDestination?,
) {
    composable(
        route = Routes.HOME,
        enterTransition = {
            fadeIn(animationSpec = tween(durationMillis = FADE_TRANS_DURATION))
        },
        exitTransition = {
            when (targetState.destination.route) {
                Routes.HOME -> {
                    fadeOut(animationSpec = tween(durationMillis = FADE_TRANS_DURATION))
                }

                else -> {
                    fadeOut() + slideOutHorizontally()
                }
            }
        },
        popEnterTransition = {
            fadeIn() + slideInHorizontally()
        },
        popExitTransition = {
            fadeOut() + slideOutHorizontally()
        },
    ) {
        val context = LocalContext.current

        val homeDest = remember {
            if (secondDestination == SecondDestination.Download) {
                Routes.Home.DOWNLOADS
            } else {
                initialDestination
            }
        }

        LaunchedEffect(Unit) {
            when (secondDestination) {
                SecondDestination.Search -> {
                    val route = Routes.search(
                        serviceId = context.preferencesStore().currentService.value,
                        query = null,
                    )
                    onNavigate(navPushEvent(route))
                }

                else -> {}
            }
            onClearSecondDestination()
        }

        HomeScreen(
            onNavigate = onNavigate,
            initialDestination = homeDest,
            darkMode = darkMode,
        )
    }
}

@ExperimentalAnimationApi
private fun NavGraphBuilder.postScreen(
    onNavigate: (NavEvent) -> Unit,
    darkMode: Boolean,
    imagePagerViewModel: ImagePagerViewModel,
) {
    composable(
        route = Routes.POST,
        arguments = PostArgs.navArgs,
        enterTransition = {
            // Slide from right
            fadeIn() + slideInHorizontally { it }
        },
        exitTransition = {
            val target = targetState.destination.route
            if (target == Routes.POST || target == Routes.PROFILE) {
                // Goto post or profile
                // Slide to left
                fadeOut() + slideOutHorizontally { -it }
            } else {
                // Slide to right
                fadeOut() + slideOutHorizontally { it }
            }
        },
        popEnterTransition = {
            // Slide from left
            fadeIn() + slideInHorizontally { -it }
        },
        popExitTransition = {
            // Slide to right
            fadeOut() + slideOutHorizontally { it }
        },
    ) { backStackEntry ->
        val postUrl = backStackEntry.postUrl
        val serviceId = backStackEntry.serviceId
        val initialElementIndex = backStackEntry.initialElementIndex ?: -1
        val initialElementScrollOffset = backStackEntry.initialElementScrollOffset ?: 0
        val isExiting = transition.targetState == EnterExitState.PostExit

        val positionController = remember { ImagePagerPositionController() }

        DisposableEffect(positionController, imagePagerViewModel) {
            imagePagerViewModel.attachUpdater(positionController)
            onDispose {
                imagePagerViewModel.detachUpdater(positionController)
            }
        }

        TintSystemBars(
            darkMode = darkMode,
            statusBarColor = Color.Transparent,
            navigationBarColor = Color.Transparent,
        )

        BoxWithSystemBars {
            PostScreen(
                onNavigate = onNavigate,
                positionController = positionController,
                postUrl = postUrl,
                serviceId = serviceId,
                initialElementIndex = initialElementIndex,
                initialElementScrollOffset = initialElementScrollOffset,
                isRunningExitTransition = isExiting,
            )
        }
    }
}

@ExperimentalAnimationApi
private fun NavGraphBuilder.profileScreen(
    onNavigate: (NavEvent) -> Unit,
    darkMode: Boolean,
) {
    composable(
        route = Routes.PROFILE,
        arguments = ProfileArgs.navArgs,
        enterTransition = {
            // Slide from right
            fadeIn() + slideInHorizontally { it }
        },
        exitTransition = {
            val target = targetState.destination.route
            if (target == Routes.POST || target == Routes.PROFILE) {
                // Goto post or profile
                // Slide to left
                fadeOut() + slideOutHorizontally { -it }
            } else {
                // Slide to right
                fadeOut() + slideOutHorizontally { it }
            }
        },
        popEnterTransition = {
            // Slide from left
            fadeIn() + slideInHorizontally { -it }
        },
        popExitTransition = {
            // Slide to right
            fadeOut() + slideOutHorizontally { it }
        },
    ) { backStackEntry ->
        val serviceId = backStackEntry.serviceId
        val userId = backStackEntry.userId
        val userUrl = backStackEntry.userUrl

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

        val barsColorController = rememberBarsColorController(
            statusBarColor = Color.Transparent,
        )
        BoxWithSystemBars(barsColorController = barsColorController) {
            if (userUrl != null) {
                ProfileScreen(
                    onNavigate = onNavigate,
                    userUrl = userUrl,
                )
            } else if (serviceId != null && userId != null) {
                ProfileScreen(
                    onNavigate = onNavigate,
                    serviceId = serviceId,
                    userId = userId,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                ) {
                    val message = """
                        Illegal navigation parameters
                        Require 'userUrl' != null or ('serviceId' != null && 'userId' != null)
                    """.trimIndent()
                    Text(message)
                }
            }
        }
    }
}

@ExperimentalAnimationApi
private fun NavGraphBuilder.searchScreen(
    onNavigate: (NavEvent) -> Unit,
    darkMode: Boolean,
) {
    composable(
        route = Routes.SEARCH,
        arguments = SearchArgs.navArgs,
        enterTransition = {
            // Slide from right
            fadeIn() + slideInHorizontally { it }
        },
        exitTransition = {
            // Slide to left
            fadeOut() + slideOutHorizontally { -it }
        },
        popEnterTransition = {
            // Slide from left
            fadeIn() + slideInHorizontally { -it }
        },
        popExitTransition = {
            // Slide to right
            fadeOut() + slideOutHorizontally { it }
        },
    ) { backStackEntry ->
        val serviceId = backStackEntry.serviceId
        val query = backStackEntry.query
        TintSystemBars(darkMode = darkMode)
        SearchScreen(
            onNavigate = onNavigate,
            serviceId = serviceId,
            query = query,
        )
    }
}

@ExperimentalAnimationApi
private fun NavGraphBuilder.settingsScreen(
    onNavigate: (NavEvent) -> Unit,
    darkMode: Boolean,
) {
    composable(
        route = Routes.SETTINGS,
        arguments = SettingsArgs.navArgs,
        enterTransition = {
            // Slide from right
            fadeIn() + slideInHorizontally { it }
        },
        exitTransition = {
            when (targetState.destination.route) {
                Routes.RUN_SQL -> {
                    // Slide to left
                    fadeOut() + slideOutHorizontally()
                }

                else -> {
                    // Slide to right
                    fadeOut() + slideOutHorizontally { it }
                }
            }
        },
        popEnterTransition = { fadeIn() + slideInHorizontally() },
    ) { backStackEntry ->
        val subSettings = backStackEntry.subSettings
        TintSystemBars(darkMode = darkMode)
        SettingsScreen(
            onNavigate = onNavigate,
            subSettings = subSettings,
        )
    }
}

@ExperimentalAnimationApi
private fun NavGraphBuilder.runSqlScreen(
    darkMode: Boolean,
) {
    composable(
        route = Routes.RUN_SQL,
        enterTransition = {
            // Slide from right
            fadeIn() + slideInHorizontally { it }
        },
        exitTransition = {
            // Slide to right
            fadeOut() + slideOutHorizontally { it }
        },
    ) {
        TintSystemBars(darkMode = darkMode)
        RunSqlScreen()
    }
}

@ExperimentalComposeUiApi
@ExperimentalAnimationApi
private fun NavGraphBuilder.imagePager(
    onNavigate: (NavEvent) -> Unit,
    imagePagerViewModel: ImagePagerViewModel,
) {
    dialog(
        route = Routes.IMAGE_PAGER,
        arguments = ImagePagerArgs.navArgs,
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
    ) { backStackEntry ->
        val images = imagePagerViewModel.images ?: emptyList()
        ImagePager(
            onBack = { onNavigate(NavEvent.Back) },
            currentIndexUpdater = { imagePagerViewModel.updateScrollPosition(it) },
            title = backStackEntry.imagePagerTitle,
            images = images,
            initialPage = backStackEntry.imagePagerPage,
        )
    }
}
