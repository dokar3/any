package any.ui.jslogger

import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import any.ui.common.R
import any.ui.floatingbubble.BubbleFab

@Composable
internal fun Bubble(
    modifier: Modifier = Modifier,
) {
    BubbleFab(
        backgroundColor = Color(0xFF8500B6),
        contentColor = Color.White,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_baseline_text_snippet_24),
            contentDescription = null
        )
    }
}