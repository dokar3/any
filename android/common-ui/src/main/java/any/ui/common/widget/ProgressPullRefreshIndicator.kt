package any.ui.common.widget

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshDefaults
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.R
import any.ui.common.theme.secondaryText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun rememberPullRefreshIndicatorOffset(
    state: PullRefreshState,
    refreshThreshold: Dp = PullRefreshDefaults.RefreshThreshold,
): Int {
    val density = LocalDensity.current

    val thresholdPx = with(density) { refreshThreshold.roundToPx() }

    val prevProgress = remember { mutableFloatStateOf(0f) }

    val indicatorOffset = remember { mutableIntStateOf(0) }

    LaunchedEffect(state, thresholdPx) {
        snapshotFlow { state.progress }
            .collect { progress ->
                if (progress == 0f) {
                    launch {
                        animate(
                            initialValue = prevProgress.floatValue,
                            targetValue = progress,
                        ) { value, _ ->
                            indicatorOffset.intValue = (thresholdPx * value).toInt()
                        }
                        prevProgress.floatValue = progress
                    }
                } else {
                    prevProgress.floatValue = progress
                    indicatorOffset.intValue = (thresholdPx * progress).toInt()
                }
            }
    }

    return indicatorOffset.intValue
}

/**
 * A pull refresh indicator can also show the loading progress bar.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ProgressPullRefreshIndicator(
    state: PullRefreshState,
    isRefreshing: Boolean,
    indicatorOffsetProvider: () -> Int,
    modifier: Modifier = Modifier,
    loadingProgress: Float? = null,
    progressColor: Color = MaterialTheme.colors.primary,
    textColor: Color = MaterialTheme.colors.secondaryText.copy(alpha = 0.7f),
) {
    Box(modifier = modifier.fillMaxWidth()) {
        if (isRefreshing) {
            if (loadingProgress != null && loadingProgress > 0f) {
                LinearProgressIndicator(
                    progress = loadingProgress,
                    modifier = Modifier.fillMaxWidth(),
                    color = progressColor,
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = progressColor,
                )
            }
        } else {
            val refreshProgress = state.progress.coerceIn(0f, 1f)
            Row(
                modifier = Modifier
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val width = placeable.width
                        val height = placeable.height
                        layout(width, height) {
                            val indicatorOffset = indicatorOffsetProvider()
                            val h = height - 16.dp.toPx()
                            val offsetY = if (indicatorOffset > h) {
                                ((indicatorOffset - h) / 2).toInt()
                            } else {
                                0
                            }
                            placeable.placeRelative(0, offsetY)
                        }
                    }
                    .graphicsLayer { alpha = refreshProgress }
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                ProgressBar(
                    size = 18.dp,
                    progress = refreshProgress,
                    color = progressColor,
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = if (refreshProgress == 1f) {
                        stringResource(R.string.release_to_refresh)
                    } else {
                        stringResource(R.string.pull_to_refresh)
                    },
                    modifier = Modifier.animateContentSize(),
                    color = textColor,
                    fontSize = 14.sp,
                )
            }
        }
    }
}
