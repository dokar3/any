package any.ui.common.widget

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import any.ui.common.lazy.LazyScrollableState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun rememberQuickReturnScreenState(
    lazyScrollableState: LazyScrollableState? = null,
): QuickReturnScreenState {
    return remember {
        QuickReturnScreenState(lazyScrollableState = lazyScrollableState)
    }.also {
        it.lazyScrollableState = lazyScrollableState
    }
}

@Stable
class QuickReturnScreenState(
    internal var lazyScrollableState: LazyScrollableState?,
) {
    internal var topBarOffsetYAnim = Animatable(0f)
    val topBarOffsetY: Float get() = topBarOffsetYAnim.value

    internal var btmBarOffsetYAnim = Animatable(0f)
    val btmBarOffsetY: Float get() = btmBarOffsetYAnim.value

    var topBarHeight: Int by mutableIntStateOf(0)
        internal set

    var btmBarHeight: Int by mutableIntStateOf(0)
        internal set

    /**
     * Calculates the number of scrollable pixels of the list. Returns a negative integer
     * if it cannot be detected at the current scroll position.
     */
    private fun calcScrollablePixels(): Int {
        val state = lazyScrollableState ?: return -1
        val last = state.visibleItemsInfo.lastOrNull() ?: return -1
        if (last.index != state.totalItemsCount - 1) {
            return -1
        }
        val bottomPadding = state.afterContentPadding
        return last.offset + last.size + bottomPadding - state.viewportEndOffset
    }

    suspend fun resetTopBar(animate: Boolean = false) {
        if (animate) {
            topBarOffsetYAnim.animateTo(0f)
        } else {
            topBarOffsetYAnim.snapTo(0f)
        }
    }

    suspend fun resetBottomBar(animate: Boolean = false) {
        if (animate) {
            btmBarOffsetYAnim.animateTo(0f)
        } else {
            btmBarOffsetYAnim.snapTo(0f)
        }
    }

    suspend fun resetBars(animate: Boolean = false) {
        resetTopBar(animate)
        resetBottomBar(animate)
    }

    internal suspend fun updateTopBarOffset(newOffset: Float) {
        topBarOffsetYAnim.stop()
        topBarOffsetYAnim.snapTo(newOffset)
    }

    internal suspend fun updateBottomBarOffset(
        bottomBarHeight: Int,
        newOffset: Float,
    ) {
        btmBarOffsetYAnim.stop()
        val pixelsToBottom = calcScrollablePixels()
        if (pixelsToBottom in 0 until bottomBarHeight) {
            if (pixelsToBottom < btmBarOffsetYAnim.value) {
                btmBarOffsetYAnim.snapTo(pixelsToBottom.toFloat())
            }
        } else {
            btmBarOffsetYAnim.snapTo(newOffset)
        }
    }
}

@Composable
fun rememberQuickReturnNestedScrollConnection(
    state: QuickReturnScreenState,
): QuickReturnNestedScrollConnection {
    val scope = rememberCoroutineScope()
    return remember(scope, state) {
        QuickReturnNestedScrollConnection(scope, state)
    }
}

class QuickReturnNestedScrollConnection(
    private val scope: CoroutineScope,
    private val state: QuickReturnScreenState,
) : NestedScrollConnection {
    internal var fixedTopBar: Boolean = false
    internal var fixedBottomBar: Boolean = false

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        updateOffset(consumed)
        return Offset.Zero
    }

    private fun updateOffset(offset: Offset) {
        if (!fixedTopBar) {
            val newOffset = (state.topBarOffsetYAnim.value + offset.y)
                .coerceIn(-state.topBarHeight.toFloat(), 0f)
            scope.launch {
                state.updateTopBarOffset(newOffset)
            }
        }

        if (!fixedBottomBar) {
            val newOffset = (state.btmBarOffsetYAnim.value - offset.y)
                .coerceIn(0f, state.btmBarHeight.toFloat())
            scope.launch {
                state.updateBottomBarOffset(
                    bottomBarHeight = state.btmBarHeight,
                    newOffset = newOffset,
                )
            }
        }
    }
}

@Composable
fun QuickReturnScreen(
    modifier: Modifier = Modifier,
    state: QuickReturnScreenState = rememberQuickReturnScreenState(),
    nestedScrollConnection: QuickReturnNestedScrollConnection =
        rememberQuickReturnNestedScrollConnection(state = state),
    fixedTopBar: Boolean = false,
    fixedBottomBar: Boolean = false,
    topBarHeight: Dp = 0.dp,
    bottomBarHeight: Dp = 0.dp,
    topBar: @Composable ColumnScope.(offsetY: Int) -> Unit,
    bottomBar: @Composable ColumnScope.(offsetY: Int) -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current

    SideEffect {
        nestedScrollConnection.fixedTopBar = fixedTopBar
        nestedScrollConnection.fixedBottomBar = fixedBottomBar
    }

    LaunchedEffect(topBarHeight) {
        if (topBarHeight > 0.dp) {
            state.topBarHeight = with(density) { topBarHeight.roundToPx() }
        }
    }

    LaunchedEffect(bottomBarHeight) {
        if (bottomBarHeight > 0.dp) {
            state.btmBarHeight = with(density) { bottomBarHeight.roundToPx() }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        content()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .onSizeChanged {
                    if (it.height != 0) {
                        state.topBarHeight = it.height
                    }
                }
                .offset { IntOffset(0, state.topBarOffsetYAnim.value.toInt()) }
        ) {
            topBar(state.topBarOffsetYAnim.value.toInt())
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .align(Alignment.BottomCenter)
                .onSizeChanged {
                    if (it.height != 0) {
                        state.btmBarHeight = it.height
                    }
                }
                .offset { IntOffset(0, state.btmBarOffsetYAnim.value.toInt()) }
        ) {
            bottomBar(state.btmBarOffsetYAnim.value.toInt())
        }
    }
}