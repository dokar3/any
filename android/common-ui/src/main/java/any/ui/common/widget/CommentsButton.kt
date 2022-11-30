package any.ui.common.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.ui.common.R

@Composable
fun CommentsButton(
    onClick: () -> Unit,
    commentCount: Int,
    modifier: Modifier = Modifier,
    iconAlpha: Float = 1f,
) {
    Box(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = false),
                onClick = onClick,
            )
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_outline_comment_24),
            contentDescription = stringResource(any.base.R.string.comments),
            modifier = Modifier
                .align(Alignment.Center)
                .size(22.dp)
                .alpha(iconAlpha),
        )
        if (commentCount > 0) {
            CommentCount(
                count = commentCount,
                modifier = Modifier
                    .widthIn(min = 24.dp)
                    .align(Alignment.TopEnd),
            )
        }
    }
}

@Composable
fun CommentCount(
    count: Int,
    modifier: Modifier = Modifier,
) {
    val displayCount = if (count <= 999) {
        count.toString()
    } else {
        "• • •"
    }
    Text(
        text = displayCount,
        modifier = modifier
            .background(
                color = MaterialTheme.colors.primary,
                shape = CircleShape,
            )
            .border(
                width = 2.dp, color = MaterialTheme.colors.background, shape = CircleShape
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
        fontSize = 11.sp,
        color = MaterialTheme.colors.onPrimary,
        textAlign = TextAlign.Center,
    )
}
