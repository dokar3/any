package any.ui.common.modifier

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

fun Modifier.drawCheckMark(
    visible: Boolean = true,
    animated: Boolean = true,
    size: Dp = 24.dp,
    maskColor: Color = Color.Unspecified,
    iconBackgroundColor: Color = Color.Unspecified,
    iconColor: Color = Color.Unspecified,
    alignment: Alignment = Alignment.Center,
): Modifier = composed {
    if (!visible) {
        return@composed this
    }

    val anim = remember(animated) { Animatable(if (animated) 0f else 1f) }

    val layoutDirection = LocalLayoutDirection.current

    val painter = rememberVectorPainter(image = Icons.Default.CheckCircle)

    val realMaskColor = if (maskColor == Color.Unspecified) {
        MaterialTheme.colors.background.copy(0.6f)
    } else {
        maskColor
    }

    val realIconBackgroundColor = if (iconBackgroundColor == Color.Unspecified) {
        MaterialTheme.colors.primary
    } else {
        iconBackgroundColor
    }

    val realIconColor = if (iconColor == Color.Unspecified) {
        Color.White
    } else {
        iconColor
    }

    LaunchedEffect(anim, animated) {
        if (animated) {
            anim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 200),
            )
        }
    }

    clipToBounds().drawWithCache {
        val iconSizePx = size.toPx()
        val iconSize = Size(iconSizePx, iconSizePx)
        val padding = iconSizePx / 2
        val paddedIconSize = IntSize(
            iconSize.width.toInt() + padding.toInt() * 2,
            iconSize.height.toInt() + padding.toInt() * 2
        )
        val offset = alignment.align(
            size = paddedIconSize,
            space = IntSize(this.size.width.toInt(), this.size.height.toInt()),
            layoutDirection = layoutDirection,
        )
        val colorFilter = ColorFilter.tint(realIconColor)
        val iconOffset = Offset(padding, padding)
        onDrawWithContent {
            drawContent()
            drawRect(color = realMaskColor.copy(alpha = realMaskColor.alpha * anim.value))
            translate(
                offset.x.toFloat() + iconOffset.x,
                offset.y.toFloat() + iconOffset.y
            ) {
                scale(scale = anim.value, pivot = iconOffset) {
                    drawCircle(
                        color = realIconBackgroundColor,
                        radius = iconSizePx,
                        center = Offset(iconSizePx / 2, iconSizePx / 2),
                    )
                    with(painter) {
                        draw(size = iconSize, colorFilter = colorFilter)
                    }
                }
            }
        }
    }
}