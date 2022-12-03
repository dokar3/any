package any.ui.common.post

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import any.ui.common.R
import any.ui.common.theme.secondaryText

@Composable
internal fun RepostHeader(
    onUserClick: () -> Unit,
    username: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(12.dp),
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(contentPadding),
    ) {
        val contentColor = MaterialTheme.colors.secondaryText
        Icon(
            painter = painterResource(R.drawable.ic_post_repost),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = contentColor,
        )

        val startPadding = contentPadding.calculateStartPadding(LocalLayoutDirection.current)
        Spacer(modifier = Modifier.width(max(startPadding, 8.dp)))

        Text(
            text = stringResource(any.base.R.string._username_reposted, username),
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onUserClick,
            ),
            fontSize = 14.sp,
            color = contentColor,
            fontWeight = FontWeight.Bold,
        )
    }
}