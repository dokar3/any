package any.ui.common.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import any.ui.common.theme.divider

@Composable
fun DoubleEndDashedDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.primary,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DashedDivider(
            modifier = Modifier
                .padding(end = 8.dp)
                .weight(weight = 1f, fill = false),
            color = color,
        )

        content()

        DashedDivider(
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(weight = 1f, fill = false),
            color = color,
        )
    }
}

@Composable
fun DashedDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.divider,
) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .drawWithCache {
                val strokeWidth = 2.dp.toPx()
                val dashPathEffect = PathEffect.dashPathEffect(
                    intervals = floatArrayOf(strokeWidth, strokeWidth),
                )
                onDrawBehind {
                    drawLine(
                        color = color,
                        start = Offset(0f, size.height / 2 - strokeWidth / 2),
                        end = Offset(size.width, size.height / 2 - strokeWidth / 2),
                        strokeWidth = strokeWidth,
                        pathEffect = dashPathEffect,
                    )
                }
            }
    )
}
