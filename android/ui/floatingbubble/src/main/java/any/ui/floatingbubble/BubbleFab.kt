package any.ui.floatingbubble

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun BubbleFab(
    modifier: Modifier = Modifier,
    elevation: Dp = 6.dp,
    minSize: Dp = 56.dp,
    backgroundColor: Color = MaterialTheme.colors.secondary,
    contentColor: Color = contentColorFor(backgroundColor),
    badge: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalContentColor provides contentColor
    ) {
        Box(
            modifier = Modifier.padding(
                start = elevation * 0.5f,
                top = elevation * 0.8f,
                end = elevation * 0.5f,
                bottom = elevation * 1.2f,
            )
        ) {
            Surface(
                modifier = modifier.sizeIn(minWidth = minSize, minHeight = minSize),
                elevation = elevation,
                color = backgroundColor,
                shape = CircleShape,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    content()
                }
            }

            if (badge != null) {
                badge()
            }
        }
    }
}