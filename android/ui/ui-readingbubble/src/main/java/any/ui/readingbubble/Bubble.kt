package any.ui.readingbubble

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.ui.common.R
import any.ui.floatingbubble.BubbleFab
import any.ui.readingbubble.viewmodel.ReadingBubbleViewModel

@Composable
internal fun Bubble(
    viewModel: ReadingBubbleViewModel,
    modifier: Modifier = Modifier,
) {
    val postCount = viewModel.uiState.collectAsState().value.posts.size
    BubbleFab(
        modifier = modifier,
        backgroundColor = Color(0xFF0CCC72),
        contentColor = Color.White,
        badge = {
            if (postCount > 0) {
                CountBadge(count = postCount)
            }
        },
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_baseline_bookmark_border_24),
            contentDescription = null,
        )
    }
}

@Composable
private fun CountBadge(
    count: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .sizeIn(minWidth = 22.dp, minHeight = 22.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colors.secondary)
            .border(
                width = 1.dp,
                color = MaterialTheme.colors.background,
                shape = CircleShape,
            )
            .padding(1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$count",
            color = contentColorFor(MaterialTheme.colors.secondary),
            fontSize = 13.sp,
        )
    }
}