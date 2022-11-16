package any.ui.common.modifier

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

fun Modifier.verticalScrollBar(
    state: ScrollState,
    color: Color = Color.Unspecified,
    width: Dp = 4.dp,
    padding: PaddingValues = PaddingValues(0.dp),
    autoFadeOutDelay: Long = 300L,
): Modifier = composed {
    val scope = rememberCoroutineScope()

    var height by remember { mutableStateOf(0) }

    val alpha = remember { Animatable(0f) }

    val barColor = if (color == Color.Unspecified) {
        MaterialTheme.colors.onBackground.copy(alpha = 0.3f)
    } else {
        color
    }

    fun updateScrollBarAlpha(isScrollInProgress: Boolean): Job = scope.launch {
        if (isScrollInProgress) {
            alpha.animateTo(1f)
        } else {
            delay(autoFadeOutDelay)
            alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 500),
            )
        }
    }

    LaunchedEffect(state) {
        snapshotFlow { state.maxValue to height }
            .filter { it.first != Int.MAX_VALUE && it.second != 0 }
            .first()
        alpha.animateTo(1f)

        var updateJob: Job? = null
        snapshotFlow { state.isScrollInProgress }
            .distinctUntilChanged()
            .collect {
                updateJob?.cancel()
                updateJob = updateScrollBarAlpha(it)
            }
    }

    onSizeChanged { height = it.height }
        .drawWithCache {
            val barWidthPx = width.toPx()
            val topPadding = padding
                .calculateTopPadding()
                .roundToPx()
            val btmPadding = padding
                .calculateBottomPadding()
                .roundToPx()
            onDrawWithContent {
                drawContent()
                if (height == 0) {
                    return@onDrawWithContent
                }

                if (alpha.value <= 0f) {
                    return@onDrawWithContent
                }

                if (state.maxValue == Int.MAX_VALUE) {
                    return@onDrawWithContent
                }

                // h is the visible area height
                val h = (height - state.maxValue) - topPadding - btmPadding
                val barHeightPx = h / (h + state.maxValue.toFloat()) * h
                if (barHeightPx <= 0f) {
                    return@onDrawWithContent
                }

                val progress = state.value.toFloat() / state.maxValue
                val top = topPadding + state.value + (h - barHeightPx) * progress
                drawRect(
                    color = barColor,
                    topLeft = Offset(
                        x = size.width - barWidthPx,
                        y = top,
                    ),
                    size = Size(barWidthPx, barHeightPx),
                    alpha = alpha.value,
                )
            }
        }
}