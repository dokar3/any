package any.ui.post.item

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import any.base.image.ImageRequest
import any.domain.entity.UiContentElement
import any.ui.common.R
import any.ui.common.image.AsyncImage
import any.ui.common.theme.imagePlaceholder
import any.ui.common.theme.thumb

// TODO: Implement the in-app video playback
@Composable
internal fun VideoItem(
    video: UiContentElement.Video,
    modifier: Modifier = Modifier,
    onPlayClick: () -> Unit,
    defaultAspectRatio: Float = 16f / 9,
) {
    Box(
        modifier = modifier
            .padding(
                horizontal = ItemsDefaults.ItemHorizontalSpacing,
                vertical = ItemsDefaults.ItemVerticalSpacing,
            )
            .fillMaxWidth()
            .aspectRatio(video.aspectRatio ?: defaultAspectRatio)
            .clip(MaterialTheme.shapes.thumb)
            .background(MaterialTheme.colors.imagePlaceholder),
    ) {
        val thumb = video.thumbnail
        if (thumb != null) {
            AsyncImage(
                request = ImageRequest.Url(thumb),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }

        VideoPlaybackButton(
            onPlayClick = onPlayClick,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
internal fun VideoPlaybackButton(
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = false),
                onClick = onPlayClick,
            ),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_outline_play_arrow_24),
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = Color.White,
        )
    }
}