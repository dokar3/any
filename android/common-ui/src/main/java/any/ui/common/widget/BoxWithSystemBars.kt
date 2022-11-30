package any.ui.common.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import any.ui.common.theme.navigationBar
import any.ui.common.theme.statusBar

@Composable
fun rememberBarsColorController(
    statusBarColor: Color = MaterialTheme.colors.statusBar,
    navigationBarColor: Color = MaterialTheme.colors.navigationBar,
): BarsColorController {
    return remember(statusBarColor, navigationBarColor) {
        BarsColorController(statusBarColor, navigationBarColor)
    }
}

@Stable
class BarsColorController(
    statusBarColor: Color,
    navigationBarColor: Color,
) {
    var statusBarColor by mutableStateOf(statusBarColor)

    var navigationBarColor by mutableStateOf(navigationBarColor)
}

/**
 * Box also draws status and navigation bar on top of the content. Provide a BarsColorController to
 * change the system bar color instead of passing the color values directly, which will avoid
 * recomposition during color changes.
 */
@Composable
fun BoxWithSystemBars(
    modifier: Modifier = Modifier,
    barsColorController: BarsColorController = rememberBarsColorController(),
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.getTop(density)
    val navigationBarHeight = WindowInsets.navigationBars.getBottom(density)
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                onDrawWithContent {
                    drawContent()
                    // Draw status bar
                    drawRect(
                        color = barsColorController.statusBarColor,
                        size = size.copy(height = statusBarHeight.toFloat()),
                    )
                    // Draw navigation bar
                    drawRect(
                        color = barsColorController.navigationBarColor,
                        topLeft = Offset(0f, size.height - navigationBarHeight),
                        size = size.copy(height = navigationBarHeight.toFloat())
                    )
                }
            },
        content = content
    )
}