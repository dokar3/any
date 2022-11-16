package any.ui.common.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DefaultProgressBarSize = 40.dp

private val DefaultProgressStrokeWidth = 3.dp

@Composable
fun ProgressBar(
    modifier: Modifier = Modifier,
    size: Dp = DefaultProgressBarSize,
    color: Color = MaterialTheme.colors.primary,
) {
    CircularProgressIndicator(
        modifier = modifier.size(size),
        strokeWidth = size * (DefaultProgressStrokeWidth / DefaultProgressBarSize),
        color = color,
    )
}

@Composable
fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = DefaultProgressBarSize,
    color: Color = MaterialTheme.colors.primary,
) {
    CircularProgressIndicator(
        progress = progress,
        modifier = modifier.size(size),
        strokeWidth = size * (DefaultProgressStrokeWidth / DefaultProgressBarSize),
        color = color,
    )
}

@Composable
fun LinearProgressBar(
    modifier: Modifier = Modifier,
    height: Dp = DefaultProgressStrokeWidth,
    color: Color = MaterialTheme.colors.primary,
) {
    LinearProgressIndicator(
        modifier = modifier.height(height),
        color = color,
    )
}

@Composable
fun LinearProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = DefaultProgressStrokeWidth,
    color: Color = MaterialTheme.colors.primary,
) {
    LinearProgressIndicator(
        progress = progress,
        modifier = modifier.height(height),
        color = color,
    )
}

@Composable
fun LinearProgressLayout(
    current: Int,
    total: Int,
    modifier: Modifier = Modifier,
    progressTextColor: Color = MaterialTheme.colors.onBackground,
    progressTextSize: TextUnit = 14.sp,
    progressBarHeight: Dp = DefaultProgressStrokeWidth,
    progressColor: Color = MaterialTheme.colors.primary,
) {
    Column(modifier = modifier.width(IntrinsicSize.Max)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = current.toString(),
                color = progressTextColor,
                fontSize = progressTextSize,
            )
            Text(
                text = total.toString(),
                color = progressTextColor,
                fontSize = progressTextSize,
            )
        }

        val progress = (current.toFloat() / total).coerceIn(0f, 1f)
        LinearProgressBar(
            progress = progress,
            height = progressBarHeight,
            color = progressColor,
        )
    }
}
