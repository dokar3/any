package any.ui.common.widget

import androidx.compose.animation.animateContentSize
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.R
import any.ui.common.theme.secondaryText

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun rememberPullRefreshIndicatorOffset(
    state: PullRefreshState,
    refreshThreshold: Dp = PullRefreshDefaults.RefreshThreshold,
): Int {
    val density = LocalDensity.current
    val indicatorOffset by remember(state, density) {
        derivedStateOf {
            val dp = refreshThreshold * state.progress
            with(density) { dp.roundToPx() }
        }
    }
    return indicatorOffset
}

/**
 * A pull refresh indicator can also show the loading progress bar.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ProgressPullRefreshIndicator(
    state: PullRefreshState,
    isRefreshing: Boolean,
    indicatorOffset: Int,
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
        } else if (indicatorOffset > 0) {
            val refreshProgress = state.progress
            Row(
                modifier = Modifier
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val width = placeable.width
                        val height = placeable.height
                        layout(width, height) {
                            val h = height - 16.dp.toPx()
                            val offsetY = if (indicatorOffset > h) {
                                ((indicatorOffset - h) / 2).toInt()
                            } else {
                                0
                            }
                            placeable.placeRelative(0, offsetY)
                        }
                    }
                    .fillMaxWidth()
                    .padding(16.dp)
                    .alpha(refreshProgress),
                horizontalArrangement = Arrangement.Center,
            ) {
                ProgressBar(
                    size = 18.dp,
                    progress = refreshProgress,
                    color = progressColor,
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = if (refreshProgress.coerceIn(0f, 1f) == 1f) {
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
