package any.ui.common.modifier

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified

fun Modifier.horizontalWindowInsetsSpacers(
    insets: WindowInsets,
    spacerBackgroundColor: Color,
): Modifier {
    return this
        .drawWithCache {
            val left = insets.getLeft(density = this, layoutDirection = layoutDirection)
            val right = insets.getRight(density = this, layoutDirection = layoutDirection)
            onDrawWithContent {
                drawContent()
                if (spacerBackgroundColor.isUnspecified || spacerBackgroundColor.alpha == 0f) {
                    return@onDrawWithContent
                }
                if (left > 0) {
                    drawRect(
                        color = spacerBackgroundColor,
                        topLeft = Offset.Zero,
                        size = size.copy(width = left.toFloat()),
                    )
                }
                if (right > 0) {
                    drawRect(
                        color = spacerBackgroundColor,
                        topLeft = Offset(size.width - right, 0f),
                        size = size.copy(width = right.toFloat()),
                    )
                }
            }
        }
        .windowInsetsPadding(insets.only(WindowInsetsSides.Horizontal))
}