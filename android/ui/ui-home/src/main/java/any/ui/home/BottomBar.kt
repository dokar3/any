package any.ui.home

import any.base.R as BaseR
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.prefs.LAUNCH_SCREEN_COLLECTIONS
import any.base.prefs.LAUNCH_SCREEN_DOWNLOADS
import any.base.prefs.LAUNCH_SCREEN_FOLLOWING
import any.base.prefs.LAUNCH_SCREEN_FRESH
import any.base.prefs.launchScreen
import any.base.prefs.preferencesStore
import any.navigation.Routes
import any.ui.common.ViewConfiguration
import any.ui.common.theme.bottomBarBackground
import any.ui.common.widget.MSG_POPUP_DURATION
import any.ui.common.widget.MessagePopup
import any.ui.common.widget.NavigationBarSpacer
import any.ui.common.widget.ShadowDividerSpacer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BottomBar(
    currentRoute: String,
    offsetProvider: () -> Int,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
    backgroundColor: Color = MaterialTheme.colors.bottomBarBackground,
    onHeightChanged: ((Int) -> Unit)? = null,
    onItemClick: ((item: HomeNavItem, isSelected: Boolean) -> Unit)? = null,
    onItemLongClick: ((item: HomeNavItem, isSelected: Boolean) -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()

    val preferencesStore = LocalContext.current.preferencesStore()

    val hapticFeedback = LocalHapticFeedback.current

    var showMessagePopup by remember { mutableStateOf(false) }

    var launchScreen by remember {
        mutableStateOf("")
    }

    val res = LocalContext.current.resources
    val items = remember {
        listOf(
            HomeNavItem.Fresh(res.getString(BaseR.string.fresh)),
            HomeNavItem.Following(res.getString(BaseR.string.following)),
            HomeNavItem.Collections(res.getString(BaseR.string.collections)),
            HomeNavItem.Downloads(res.getString(BaseR.string.downloads)),
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(0, offsetProvider()) },
    ) {
        ShadowDividerSpacer()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .background(backgroundColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .clipToBounds()
                .onSizeChanged { onHeightChanged?.invoke(it.height) },
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val selectedColor = MaterialTheme.colors.primary
            val unselectedColor = MaterialTheme.colors.onSurface
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                NavigationItem(
                    selected = isSelected,
                    modifier = Modifier
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(bounded = false, color = selectedColor),
                            onClick = { onItemClick?.invoke(item, isSelected) },
                            onLongClick = {
                                onItemLongClick?.invoke(item, isSelected)
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                when (item.route) {
                                    Routes.Home.FRESH -> {
                                        preferencesStore.launchScreen.value = LAUNCH_SCREEN_FRESH
                                    }

                                    Routes.Home.FOLLOWING -> {
                                        preferencesStore.launchScreen.value =
                                            LAUNCH_SCREEN_FOLLOWING
                                    }

                                    Routes.Home.COLLECTIONS -> {
                                        preferencesStore.launchScreen.value =
                                            LAUNCH_SCREEN_COLLECTIONS
                                    }

                                    Routes.Home.DOWNLOADS -> {
                                        preferencesStore.launchScreen.value =
                                            LAUNCH_SCREEN_DOWNLOADS
                                    }
                                }
                                launchScreen = item.name
                                scope.launch {
                                    showMessagePopup = true
                                    delay(MSG_POPUP_DURATION)
                                    showMessagePopup = false
                                }
                            },
                        ),
                    icon = {
                        Icon(
                            painter = painterResource(item.icon),
                            contentDescription = ""
                        )
                    },
                    label = {
                        Text(text = item.name, fontSize = 12.sp)
                    },
                    selectedContentColor = selectedColor,
                    unselectedContentColor = unselectedColor,
                )
            }
        }

        NavigationBarSpacer()
    }

    val density = LocalDensity.current
    val bottomInset = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
    MessagePopup(
        visible = showMessagePopup,
        offset = DpOffset(0.dp, (-64).dp - bottomInset),
    ) {
        Text(
            text = stringResource(BaseR.string._is_the_launch_screen_now, launchScreen),
            color = MaterialTheme.colors.onPrimary,
        )
    }
}

@Composable
private fun RowScope.NavigationItem(
    selected: Boolean,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    label: @Composable () -> Unit,
    selectedContentColor: Color = MaterialTheme.colors.primary,
    unselectedContentColor: Color = MaterialTheme.colors.onSurface,
) {
    val color = remember(selected, selectedContentColor, unselectedContentColor) {
        if (selected) selectedContentColor else unselectedContentColor
    }
    val context = LocalContext.current
    val viewConfiguration = remember {
        ViewConfiguration(context = context, doubleTapTimeoutMillis = 125)
    }
    CompositionLocalProvider(LocalViewConfiguration provides viewConfiguration) {
        Column(
            modifier = modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CompositionLocalProvider(
                LocalContentColor provides color.copy(alpha = 1f),
                LocalContentAlpha provides color.alpha,
            ) {
                icon()
                label()
            }
        }
    }
}
