package any.ui.common.widget

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun StatusBarSpacer(modifier: Modifier = Modifier) {
    Spacer(modifier = modifier.windowInsetsTopHeight(WindowInsets.statusBars))
}

@Composable
fun NavigationBarSpacer(modifier: Modifier = Modifier) {
    Spacer(modifier = modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
}

@Composable
fun ShadowDividerSpacer(
    modifier: Modifier = Modifier,
    height: Dp = 4.dp,
    elevation: Dp = 1.5.dp,
) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(height = height)
            .alpha(0.3f)
            .shadow(elevation = elevation)
    )
}
