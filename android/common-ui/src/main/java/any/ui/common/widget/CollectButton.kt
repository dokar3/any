package any.ui.common.widget

import any.base.R as BaseR
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import any.ui.common.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CollectButton(
    isCollected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    size: Dp = 48.dp,
    animationDuration: Int = 255,
    collectedIconAlpha: Float = 1f,
    uncollectedIconAlpha: Float = 1f,
) {
    val currIcon: Int
    val targetIcon: Int
    val currTint: Color
    val targetTint: Color
    if (isCollected) {
        currIcon = R.drawable.ic_baseline_star_24
        targetIcon = R.drawable.ic_baseline_star_outline_24
        currTint = MaterialTheme.colors.primary
        targetTint = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
    } else {
        currIcon = R.drawable.ic_baseline_star_outline_24
        targetIcon = R.drawable.ic_baseline_star_24
        currTint = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
        targetTint = MaterialTheme.colors.primary
    }

    val res = LocalContext.current.resources
    val description = remember(isCollected) {
        if (isCollected) {
            res.getString(BaseR.string.discard)
        } else {
            res.getString(BaseR.string.collect)
        }
    }

    val scope = rememberCoroutineScope()

    val transition = remember(isCollected) { Animatable(0f) }
    Box(
        modifier = modifier
            .size(size)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = false),
                onClick = {
                    if (transition.isRunning) {
                        return@combinedClickable
                    }
                    scope.launch {
                        transition.stop()
                        transition.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = animationDuration),
                        )
                        onClick()
                    }
                },
                onLongClick = { onLongClick?.invoke() },
            )
            .graphicsLayer {
                val progress = transition.value
                val p = (if (progress < 0.5f) progress else 1f - progress) / 0.5f
                val scale = 1f + p * 0.7f
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center,
    ) {
        val targetAlpha = if (isCollected) collectedIconAlpha else uncollectedIconAlpha
        Icon(
            painter = painterResource(currIcon),
            contentDescription = description,
            modifier = Modifier.alpha((1f - transition.value) * targetAlpha),
            tint = currTint,
        )

        if (transition.value != 0f) {
            Icon(
                painter = painterResource(targetIcon),
                contentDescription = null,
                modifier = Modifier.alpha(transition.value * targetAlpha),
                tint = targetTint,
            )
        }
    }
}