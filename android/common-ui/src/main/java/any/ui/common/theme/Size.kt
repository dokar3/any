package any.ui.common.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

interface ComponentSizes {
    val headerPicAspectRatio: Float

    val thumbElevation: Dp

    val thumbBorderStroke: Dp

    val titleBarHeight: Dp

    val bottomBarHeight: Dp

    val minAdaptiveGridCellsWidth: Dp
}

private object DefaultComponentSizes : ComponentSizes {
    override val headerPicAspectRatio = 5f / 3

    override val thumbElevation = 0.dp

    override val thumbBorderStroke = 0.6.dp

    override val titleBarHeight = 48.dp

    override val bottomBarHeight: Dp = 56.dp

    override val minAdaptiveGridCellsWidth = 180.dp
}

val LocalComponentSizes: CompositionLocal<ComponentSizes> = compositionLocalOf {
    DefaultComponentSizes
}

val MaterialTheme.sizes: ComponentSizes
    @Composable
    get() = LocalComponentSizes.current
