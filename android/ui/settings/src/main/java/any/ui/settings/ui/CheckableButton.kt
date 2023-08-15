package any.ui.settings.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun CheckableButton(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    minWidth: Dp = 56.dp,
) {
    val borderColor = if (checked) {
        MaterialTheme.colors.primary
    } else {
        MaterialTheme.colors.onBackground
    }
    Box(
        modifier = modifier
            .widthIn(min = minWidth)
            .clip(MaterialTheme.shapes.small)
            .clickable { onCheckedChange(!checked) }
            .drawWithCache {
                val borderStrokeWidth = 2.dp.toPx()
                val cornerRadius = CornerRadius(6.dp.toPx())
                onDrawBehind {
                    drawRoundRect(
                        color = borderColor,
                        cornerRadius = cornerRadius,
                        style = Stroke(width = borderStrokeWidth),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(8.dp)
        )

        AnimatedVisibility(
            visible = checked,
            modifier = Modifier.align(Alignment.BottomEnd),
            enter = scaleIn(),
            exit = scaleOut(),
        ) {
            // dot
            Spacer(
                modifier = Modifier
                    .size(16.dp)
                    .offset(2.dp, 2.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colors.background)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colors.primary)
            )
        }
    }
}
