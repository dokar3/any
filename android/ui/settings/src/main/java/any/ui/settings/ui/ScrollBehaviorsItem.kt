package any.ui.settings.ui

import any.base.R as BaseR
import any.ui.common.R as CommonUiR
import androidx.compose.animation.core.EaseInOutQuart
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import any.base.prefs.fixedBottomBar
import any.base.prefs.fixedTopBar
import any.base.prefs.preferencesStore
import any.ui.common.theme.darkerImagePlaceholder
import any.ui.common.theme.imagePlaceholder
import any.ui.common.theme.placeholder
import any.ui.common.theme.thumbBorder
import any.ui.common.widget.QuickReturnScreen
import any.ui.common.widget.rememberQuickReturnNestedScrollConnection
import any.ui.common.widget.rememberQuickReturnScreenState
import any.ui.settings.SettingsItem
import any.ui.settings.SettingsItemIcon
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun ScrollBehaviorsItem(
    modifier: Modifier = Modifier,
) {
    SettingsItem(
        modifier = modifier,
        icon = {
            SettingsItemIcon(
                painter = painterResource(CommonUiR.drawable.ic_baseline_touch_app_24),
            )
        },
        iconAlignment = Alignment.Top,
    ) {
        val scope = rememberCoroutineScope()
        val preferencesStore = LocalContext.current.preferencesStore()
        val fixedTopBar by preferencesStore.fixedTopBar
            .asStateFlow(scope)
            .collectAsState()
        val fixedBottomBar by preferencesStore.fixedBottomBar
            .asStateFlow(scope)
            .collectAsState()

        Column {
            Text(stringResource(BaseR.string.scroll_behaviors))

            Spacer(modifier = Modifier.height(12.dp))

            DummyScreen(
                fixedTopBar = fixedTopBar,
                fixedBottomBar = fixedBottomBar,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(5f / 2)
                    .clip(MaterialTheme.shapes.medium)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colors.thumbBorder,
                        shape = MaterialTheme.shapes.medium,
                    ),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {

                CheckableButton(
                    title = stringResource(BaseR.string.fixed_top_bar),
                    checked = fixedTopBar,
                    onCheckedChange = { preferencesStore.fixedTopBar.value = it },
                    modifier = Modifier.weight(1f),
                )

                Spacer(modifier = Modifier.width(12.dp))

                CheckableButton(
                    title = stringResource(BaseR.string.fixed_bottom_bar),
                    checked = fixedBottomBar,
                    onCheckedChange = { preferencesStore.fixedBottomBar.value = it },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DummyScreen(
    fixedTopBar: Boolean,
    fixedBottomBar: Boolean,
    modifier: Modifier = Modifier,
    topBarHeight: Dp = 24.dp,
    bottomBarHeight: Dp = 24.dp,
    listItemCount: Int = 10,
    listItemHeight: Dp = 48.dp,
) {
    val density = LocalDensity.current

    val screenState = rememberQuickReturnScreenState()

    val nestedScrollConnection = rememberQuickReturnNestedScrollConnection(state = screenState)

    val scrollState = rememberScrollState()

    val bottomBarHeightPx = with(density) { bottomBarHeight.roundToPx() }

    LaunchedEffect(fixedTopBar) {
        if (fixedTopBar) {
            screenState.resetTopBar(animate = true)
        }
    }

    LaunchedEffect(fixedBottomBar) {
        if (fixedBottomBar) {
            screenState.resetBottomBar(animate = true)
        }
    }

    LaunchedEffect(scrollState, screenState, bottomBarHeightPx) {
        var prevValue = scrollState.value
        snapshotFlow { scrollState.value }
            .distinctUntilChanged()
            .collect {
                val offset = prevValue - it.toFloat()
                prevValue = it
                nestedScrollConnection.onPostScroll(
                    consumed = Offset(0f, offset),
                    available = Offset.Zero,
                    source = NestedScrollSource.Drag,
                )
            }
    }

    LaunchedEffect(scrollState) {
        val scrollAnimSpec = tween<Float>(
            durationMillis = 2_000,
            easing = EaseInOutQuart,
        )

        delay(1000)

        while (true) {
            scrollState.animateScrollTo(
                value = scrollState.maxValue,
                animationSpec = scrollAnimSpec,
            )

            delay(1_000)

            scrollState.animateScrollTo(
                value = 0,
                animationSpec = scrollAnimSpec,
            )

            delay(1_000)
        }
    }

    QuickReturnScreen(
        modifier = modifier,
        state = screenState,
        nestedScrollConnection = nestedScrollConnection,
        fixedTopBar = fixedTopBar,
        fixedBottomBar = fixedBottomBar,
        topBar = {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(topBarHeight)
                    .background(MaterialTheme.colors.darkerImagePlaceholder),
            )
        },
        bottomBar = {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bottomBarHeight)
                    .background(MaterialTheme.colors.darkerImagePlaceholder),
            )
        },
    ) {
        Column(
            modifier = Modifier.verticalScroll(
                state = scrollState,
                enabled = false,
            ),
        ) {
            Spacer(modifier = Modifier.height(topBarHeight))

            repeat(listItemCount) {
                DummyListItem(listItemHeight = listItemHeight)
            }

            Spacer(modifier = Modifier.height(bottomBarHeight))
        }
    }
}

@Composable
private fun DummyListItem(
    listItemHeight: Dp,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(listItemHeight)
            .fillMaxWidth()
            .padding(
                vertical = 8.dp,
                horizontal = 16.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(
            modifier = Modifier
                .size(listItemHeight - 16.dp)
                .background(
                    color = MaterialTheme.colors.imagePlaceholder,
                    shape = MaterialTheme.shapes.small,
                )
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Spacer(
                modifier = Modifier
                    .height(12.dp)
                    .fillMaxWidth(1f)
                    .background(MaterialTheme.colors.imagePlaceholder)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Spacer(
                modifier = Modifier
                    .height(12.dp)
                    .fillMaxWidth(0.7f)
                    .background(MaterialTheme.colors.placeholder)
            )
        }
    }
}
