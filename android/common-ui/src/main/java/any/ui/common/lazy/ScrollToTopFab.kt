package any.ui.common.lazy

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.size
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import any.base.R
import kotlinx.coroutines.launch
import java.util.TreeMap
import kotlin.math.abs

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ScrollToTopFab(
    scrollableState: LazyScrollableState,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    toggleScrollThreshold: Dp = 180.dp,
    enterTransition: EnterTransition = scaleIn() + slideInVertically { it * 2 },
    exitTransition: ExitTransition = scaleOut() + slideOutVertically { it * 2 },
    icon: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = stringResource(R.string.go_to_top),
            tint = Color.White,
        )
    }
) {
    val scope = rememberCoroutineScope()

    val density = LocalDensity.current

    val currThreshold = rememberUpdatedState(toggleScrollThreshold)

    val itemHeights = rememberSaveable(inputs = emptyArray()) { TreeMap<Int, Int>() }

    val showScrollToTopButton by remember(scrollableState, density) {
        val threshold = with(density) { -(currThreshold.value.roundToPx()) }
        derivedStateOf {
            scrollableState.visibleItemsInfo.let { items ->
                if (items.isEmpty()) {
                    return@let false
                }
                if (items.first().offset < threshold) {
                    return@let true
                }
                for (item in items) {
                    itemHeights[item.index] = item.size
                }
                val firstIndex = items.first().index
                var invisibleItemsHeight = 0
                for ((index, height) in itemHeights) {
                    if (index < firstIndex) {
                        invisibleItemsHeight += height
                        if (invisibleItemsHeight > abs(threshold)) {
                            return@let true
                        }
                    } else {
                        // Safely break the loop because the map is a TreeMap
                        break
                    }
                }
                -invisibleItemsHeight + items.first().offset < threshold
            }
        }
    }

    AnimatedVisibility(
        visible = showScrollToTopButton,
        modifier = modifier,
        enter = enterTransition,
        exit = exitTransition,
    ) {
        FloatingActionButton(
            onClick = { scope.launch { scrollableState.quickScrollToTop() } },
            modifier = Modifier.size(size),
        ) {
            icon()
        }
    }
}
