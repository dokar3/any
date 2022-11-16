package any.ui.common.widget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

const val MSG_POPUP_DURATION = 2000L

private const val SwipeNone = 0
private const val SwipeLeft = 1
private const val SwipeRight = 2
private const val SwipeDown = 3

@Composable
fun MessagePopup(
    visible: Boolean,
    modifier: Modifier = Modifier,
    onDismissed: (() -> Unit)? = null,
    enterTransition: EnterTransition = slideInVertically(initialOffsetY = { it }) + fadeIn(),
    exitTransition: ExitTransition = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
    elevation: Dp = 4.dp,
    shape: Shape = RoundedCornerShape(8.dp),
    backgroundColor: Color = MaterialTheme.colors.primary,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    contentMargin: PaddingValues = PaddingValues(horizontal = 16.dp),
    swipeable: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val visibleState = remember { MutableTransitionState(false) }
    SideEffect {
        visibleState.targetState = visible
    }
    MessagePopup(
        visibleState = visibleState,
        modifier = modifier,
        onDismissed = onDismissed,
        enterTransition = enterTransition,
        exitTransition = exitTransition,
        elevation = elevation,
        shape = shape,
        backgroundColor = backgroundColor,
        offset = offset,
        contentMargin = contentMargin,
        swipeable = swipeable,
        content = content,
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MessagePopup(
    visibleState: MutableTransitionState<Boolean>,
    modifier: Modifier = Modifier,
    onDismissed: (() -> Unit)? = null,
    enterTransition: EnterTransition = slideInVertically(initialOffsetY = { it }) + fadeIn(),
    exitTransition: ExitTransition = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
    elevation: Dp = 4.dp,
    shape: Shape = RoundedCornerShape(8.dp),
    backgroundColor: Color = MaterialTheme.colors.primary,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    contentMargin: PaddingValues = PaddingValues(horizontal = 16.dp),
    swipeable: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    if (!visibleState.currentState && !visibleState.targetState) {
        return
    }

    val intOffset = with(LocalDensity.current) {
        IntOffset(offset.x.toPx().toInt(), offset.y.toPx().toInt())
    }

    LaunchedEffect(visibleState) {
        snapshotFlow { visibleState.currentState to visibleState.targetState }
            .distinctUntilChanged()
            .filter { !it.first && !it.second }
            .collect { onDismissed?.invoke() }
    }

    Popup(
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
        offset = intOffset,
        alignment = Alignment.BottomCenter,
        onDismissRequest = { visibleState.targetState = false },
    ) {
        val hSwipeableState = rememberSwipeableState(initialValue = SwipeNone)
        val vSwipeableState = rememberSwipeableState(initialValue = SwipeNone)

        LaunchedEffect(hSwipeableState) {
            snapshotFlow { hSwipeableState.currentValue to hSwipeableState.isAnimationRunning }
                .distinctUntilChanged()
                .collect { (state, animating) ->
                    if ((state == SwipeLeft || state == SwipeRight) && !animating) {
                        onDismissed?.invoke()
                    }
                }
        }

        LaunchedEffect(vSwipeableState) {
            snapshotFlow { vSwipeableState.currentValue to vSwipeableState.isAnimationRunning }
                .distinctUntilChanged()
                .collect { (state, animating) ->
                    if (state == SwipeDown && !animating) {
                        onDismissed?.invoke()
                    }
                }
        }

        BoxWithConstraints {
            val width = constraints.maxWidth.toFloat()
            val height = constraints.maxHeight.toFloat()
            AnimatedVisibility(
                visibleState = visibleState,
                modifier = Modifier
                    .padding(contentMargin)
                    .offset {
                        IntOffset(
                            hSwipeableState.offset.value.toInt(),
                            vSwipeableState.offset.value
                                .toInt()
                                .coerceAtLeast(0)
                        )
                    }
                    .swipeable(
                        state = hSwipeableState,
                        anchors = mapOf(
                            0f to SwipeNone,
                            -width to SwipeLeft,
                            width to SwipeRight,
                        ),
                        orientation = Orientation.Horizontal,
                        enabled = swipeable,
                    )
                    .swipeable(
                        state = vSwipeableState,
                        anchors = mapOf(
                            0f to SwipeNone,
                            height to SwipeDown,
                        ),
                        orientation = Orientation.Vertical,
                        enabled = swipeable,
                    ),
                enter = enterTransition,
                exit = exitTransition,
            ) {
                PopupContent(
                    content = content,
                    elevation = elevation,
                    shape = shape,
                    backgroundColor = backgroundColor,
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
private fun PopupContent(
    content: @Composable BoxScope.() -> Unit,
    elevation: Dp,
    shape: Shape,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        elevation = elevation,
        shape = shape,
        color = backgroundColor
    ) {
        Box(
            modifier = modifier.padding(12.dp, 8.dp),
            content = content,
        )
    }
}