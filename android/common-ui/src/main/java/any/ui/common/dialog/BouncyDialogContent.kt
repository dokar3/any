package any.ui.common.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

private val slideSpring = spring<IntOffset>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

private val floatSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessLow,
)

private val enterTransition: EnterTransition = slideIn(animationSpec = slideSpring) {
    IntOffset(0, -it.height)
} + fadeIn(
    animationSpec = tween(
        durationMillis = 300,
        easing = FastOutSlowInEasing,
    )
) + scaleIn(
    initialScale = 0.9f,
    animationSpec = floatSpring,
)

private val exitTransition: ExitTransition = slideOut(
    animationSpec = tween(
        durationMillis = 255,
        easing = FastOutSlowInEasing,
    )
) {
    IntOffset(0, -it.height)
} + fadeOut(
    animationSpec = tween(
        durationMillis = 255,
        easing = FastOutSlowInEasing,
    )
) + scaleOut(
    targetScale = 0.9f,
    animationSpec = tween(
        durationMillis = 255,
        easing = FastOutSlowInEasing,
    )
)

@Composable
internal fun BouncyDialogContent(
    visibleState: MutableTransitionState<Boolean>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visibleState = visibleState,
        enter = enterTransition,
        exit = exitTransition,
    ) {
        DialogContent(modifier, content)
    }
}

@Composable
private fun DialogContent(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colors.surface,
        shape = MaterialTheme.shapes.medium,
        elevation = 6.dp,
    ) {
        content()
    }
}