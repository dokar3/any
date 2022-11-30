package any.ui.common.widget

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TabPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun RoundedTabIndicator(
    selectedTabIndex: Int,
    tabPositions: List<TabPosition>,
    modifier: Modifier = Modifier,
    indicatorHeight: Dp = 36.dp,
    indicatorColor: Color = MaterialTheme.colors.primary.copy(alpha = 0.16f),
) {
    val currentTabPosition = tabPositions[selectedTabIndex]
    val currentTabWidth by animateDpAsState(
        targetValue = currentTabPosition.width,
        animationSpec = tween(durationMillis = 250)
    )
    val indicatorOffset by animateDpAsState(
        targetValue = currentTabPosition.left,
        animationSpec = tween(durationMillis = 250)
    )
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.CenterStart)
            .offset(x = indicatorOffset)
            .width(currentTabWidth)
            .height(indicatorHeight)
            .clip(MaterialTheme.shapes.small)
            .background(indicatorColor),
    )
}