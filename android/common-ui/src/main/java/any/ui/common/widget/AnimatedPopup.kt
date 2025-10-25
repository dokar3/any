package any.ui.common.widget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun rememberAnimatedPopupDismissRequester(): AnimatedPopupDismissRequester {
    return remember { AnimatedPopupDismissRequester() }
}

@Stable
class AnimatedPopupDismissRequester {
    internal val visibleState = MutableTransitionState(false)

    fun dismiss() {
        visibleState.targetState = false
    }
}

@Composable
fun AnimatedPopup(
    dismissRequester: AnimatedPopupDismissRequester,
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
    scaleAnimOrigin: TransformOrigin = TransformOrigin(1f, 1f),
    elevation: Dp = 6.dp,
    shape: Shape = MaterialTheme.shapes.small,
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentAlignmentToAnchor: Alignment = Alignment.TopStart,
    offset: DpOffset = DpOffset.Zero,
    minWidth: Dp = 200.dp,
    contentPadding: PaddingValues = PaddingValues(vertical = 8.dp),
    properties: PopupProperties = PopupProperties(),
    content: @Composable ColumnScope.() -> Unit,
) {
    AnimatedPopup(
        dismissRequester = dismissRequester,
        onDismissed = onDismissed,
        modifier = modifier,
        enterTransition = scaleIn(
            animationSpec = spring(),
            transformOrigin = scaleAnimOrigin,
        ),
        exitTransition = scaleOut(
            animationSpec = tween(durationMillis = 200),
            transformOrigin = scaleAnimOrigin,
        ),
        elevation = elevation,
        shape = shape,
        backgroundColor = backgroundColor,
        contentAlignmentToAnchor = contentAlignmentToAnchor,
        offset = offset,
        minWidth = minWidth,
        contentPadding = contentPadding,
        properties = properties,
        content = content,
    )
}

@Composable
fun AnimatedPopup(
    dismissRequester: AnimatedPopupDismissRequester,
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
    enterTransition: EnterTransition = fadeIn(),
    exitTransition: ExitTransition = fadeOut(),
    elevation: Dp = 6.dp,
    shape: Shape = MaterialTheme.shapes.small,
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentAlignmentToAnchor: Alignment = Alignment.TopStart,
    offset: DpOffset = DpOffset.Zero,
    minWidth: Dp = 200.dp,
    contentPadding: PaddingValues = PaddingValues(vertical = 8.dp),
    properties: PopupProperties = PopupProperties(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val density = LocalDensity.current

    val intOffset = with(density) {
        IntOffset(offset.x.roundToPx(), offset.y.roundToPx())
    }

    LaunchedEffect(dismissRequester.visibleState) {
        dismissRequester.visibleState.targetState = true
        snapshotFlow {
            dismissRequester.visibleState.currentState to
                    dismissRequester.visibleState.targetState
        }
            .distinctUntilChanged()
            .filter { !it.first && !it.second }
            .collect { onDismissed() }
    }

    Popup(
        onDismissRequest = { dismissRequester.visibleState.targetState = false },
        alignment = contentAlignmentToAnchor,
        offset = intOffset,
        properties = properties,
    ) {
        AnimatedVisibility(
            visibleState = dismissRequester.visibleState,
            enter = enterTransition,
            exit = exitTransition,
        ) {
            Card(
                elevation = elevation,
                shape = shape,
                backgroundColor = backgroundColor,
            ) {
                Column(
                    modifier = modifier
                        .width(IntrinsicSize.Max)
                        .widthIn(min = minWidth)
                        .padding(contentPadding),
                ) {
                    content()
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnimatedPopupItem(
    index: Int,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = 16.dp,
        vertical = 10.dp,
    ),
    onLongClick: (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val itemVisibleState = remember {
        MutableTransitionState(false).also {
            it.targetState = true
        }
    }
    val animDelay = 75 + 20 * index
    AnimatedVisibility(
        visibleState = itemVisibleState,
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                enabled = onClick != null || onLongClick != null,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = { onClick?.invoke() },
                onLongClick = { onLongClick?.invoke() },
            )
            .padding(contentPadding),
        enter = fadeIn(
            animationSpec = tween(durationMillis = 255, delayMillis = animDelay)
        ) + slideInVertically(
            animationSpec = tween(durationMillis = 255, delayMillis = animDelay)
        ) { it / 2 },
        exit = ExitTransition.None,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                icon()

                Spacer(modifier = Modifier.width(8.dp))
            }

            content()
        }
    }
}
