package any.ui.common.widget

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Long click to show the tooltip text
 */
@Composable
fun TooltipBox(
    text: String,
    modifier: Modifier = Modifier,
    position: TooltipPosition = TooltipPosition.BelowContent,
    offset: DpOffset = DpOffset.Zero,
    duration: Long = 1500L,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    val hapticFeedback = LocalHapticFeedback.current

    val density = LocalDensity.current

    var showTooltip by remember { mutableStateOf(false) }

    var height by remember { mutableStateOf(0) }

    Box(
        modifier = modifier
            .onSizeChanged { height = it.height }
            .onLongClick {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onLongClick?.invoke()
                coroutineScope.launch {
                    showTooltip = true
                    delay(duration)
                    showTooltip = false
                }
            }
    ) {
        content()

        val offsetY = remember(height, position) {
            when (position) {
                TooltipPosition.AboveContent -> with(density) { (-height).toDp() }
                TooltipPosition.OverlayContent -> 0.dp
                TooltipPosition.BelowContent -> with(density) { height.toDp() }
            }
        }
        MessagePopup(
            visible = showTooltip,
            enterTransition = slideInVertically {
                when (position) {
                    TooltipPosition.AboveContent -> it / 2
                    TooltipPosition.OverlayContent -> 0
                    TooltipPosition.BelowContent -> -it / 2
                }
            } + fadeIn(),
            exitTransition = slideOutVertically {
                when (position) {
                    TooltipPosition.AboveContent -> it / 2
                    TooltipPosition.OverlayContent -> 0
                    TooltipPosition.BelowContent -> -it / 2
                }
            } + fadeOut(),
            offset = offset.copy(y = offset.y + offsetY),
            elevation = 0.dp,
            backgroundColor = Color.Black.copy(alpha = 0.6f),
        ) {
            Text(text, color = Color.White, fontSize = 14.sp)
        }
    }
}

/// Copied from androidx.compose.foundation.gestures.TapGestureDetector.kt
/**
 * Consumes all pointer events until nothing is pressed and then returns. This method assumes
 * that something is currently pressed.
 */
private suspend fun AwaitPointerEventScope.consumeUntilUp() {
    do {
        val event = awaitPointerEvent()
        event.changes.fastForEach { it.consume() }
    } while (event.changes.fastAny { it.pressed })
}

private inline fun Modifier.onLongClick(
    crossinline onLongClick: () -> Unit
): Modifier {
    return pointerInput(Unit) {
        forEachGesture {
            awaitPointerEventScope {
                val down = awaitFirstDown(requireUnconsumed = false)
                down.consume()
                try {
                    withTimeout(viewConfiguration.longPressTimeoutMillis) {
                        waitForUpOrCancellation()
                    }
                } catch (_: PointerEventTimeoutCancellationException) {
                    onLongClick()
                    consumeUntilUp()
                }
            }
        }
    }
}

enum class TooltipPosition {
    AboveContent,
    OverlayContent,
    BelowContent,
}