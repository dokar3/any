package any.ui.common.widget

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RoundedProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    secondaryProgress: Float = 0f,
    barHeight: Dp = 8.dp,
    isVertical: Boolean = false,
    progressColor: Color = MaterialTheme.colors.primary,
    progressBrush: Brush? = null,
    secondaryProgressColor: Color = MaterialTheme.colors.onBackground.copy(alpha = 0.3f),
    secondaryProgressBrush: Brush? = null,
    backgroundColor: Color = MaterialTheme.colors.onBackground.copy(alpha = 0.2f),
    backgroundBrush: Brush? = null,
    animateProgress: Boolean = true,
    startLabel: String? = null,
    endLabel: String? = null
) {
    var previousProgress by remember { mutableFloatStateOf(0f) }
    val progressAnim = remember(progress, previousProgress, animateProgress) {
        Animatable(if (animateProgress) previousProgress else progress)
    }

    var previousSecondaryProgress by remember { mutableFloatStateOf(0f) }
    val secondaryProgressAnim = remember(
        secondaryProgress, previousSecondaryProgress, animateProgress
    ) {
        Animatable(if (animateProgress) previousSecondaryProgress else secondaryProgress)
    }

    LaunchedEffect(progress) {
        if (animateProgress) {
            progressAnim.animateTo(
                targetValue = progress,
                animationSpec = tween(durationMillis = 375)
            )
        }
        previousProgress = progress
    }

    LaunchedEffect(secondaryProgress) {
        if (animateProgress) {
            secondaryProgressAnim.animateTo(
                targetValue = secondaryProgress,
                animationSpec = tween(durationMillis = 375)

            )
        }
        previousSecondaryProgress = secondaryProgress
    }

    Column(modifier = modifier) {
        if (!isVertical && (startLabel != null || endLabel != null)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Start label
                if (startLabel != null) {
                    Text(startLabel, fontSize = 14.sp, maxLines = 1)
                    Spacer(modifier = Modifier.width(16.dp))
                }
                // End label
                if (endLabel != null) {
                    Text(endLabel, fontSize = 14.sp, maxLines = 1)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        Spacer(
            modifier = Modifier
                .let {
                    if (isVertical) {
                        it
                            .width(barHeight)
                            .fillMaxHeight()
                    } else {
                        it
                            .fillMaxWidth()
                            .height(barHeight)
                    }
                }
                .drawWithCache {
                    val radiusPx = if (isVertical) {
                        size.width / 2
                    } else {
                        size.height / 2
                    }
                    val radius = CornerRadius(radiusPx)
                    onDrawBehind {
                        // Draw background
                        drawBar(
                            progress = 1f,
                            isVertical = isVertical,
                            radius = radius,
                            color = backgroundColor,
                            brush = backgroundBrush
                        )
                        // Draw secondary progress
                        drawBar(
                            progress = secondaryProgressAnim.value,
                            isVertical = isVertical,
                            radius = radius,
                            color = secondaryProgressColor,
                            brush = secondaryProgressBrush,
                        )
                        // Draw progress
                        drawBar(
                            progress = progressAnim.value,
                            isVertical = isVertical,
                            radius = radius,
                            color = progressColor,
                            brush = progressBrush
                        )
                    }
                }
        )
    }
}

private fun DrawScope.drawBar(
    progress: Float,
    isVertical: Boolean,
    radius: CornerRadius,
    color: Color? = null,
    brush: Brush? = null
) {
    val p = progress.coerceIn(0f, 1f)
    if (p == 0f) {
        return
    }
    val progressSize = if (isVertical) {
        size.copy(height = size.height * p)
    } else {
        size.copy(width = size.width * p)
    }
    if (brush != null) {
        drawRoundRect(size = progressSize, cornerRadius = radius, brush = brush)
    } else if (color != null) {
        drawRoundRect(color = color, size = progressSize, cornerRadius = radius)
    }
}