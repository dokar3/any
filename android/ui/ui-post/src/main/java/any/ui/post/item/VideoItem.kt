package any.ui.post.item

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import any.base.image.ImageRequest
import any.domain.entity.UiContentElement
import any.ui.common.image.AsyncImage
import any.ui.common.theme.imagePlaceholder
import any.ui.common.theme.thumb
import any.ui.common.video.VideoView
import any.ui.common.video.rememberVideoPlaybackState

@Composable
internal fun VideoItem(
    video: UiContentElement.Video,
    modifier: Modifier = Modifier,
    onPlayClick: () -> Unit,
    defaultAspectRatio: Float = 16f / 9,
) {
    var aspectRatio by remember {
        mutableStateOf(
            video.aspectRatio
                ?: VideoAspectRatioCache[video.url]
                ?: defaultAspectRatio
        )
    }

    Box(
        modifier = modifier
            .padding(
                horizontal = ItemsDefaults.ItemHorizontalSpacing,
                vertical = ItemsDefaults.ItemVerticalSpacing,
            )
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(MaterialTheme.shapes.thumb)
            .background(MaterialTheme.colors.imagePlaceholder),
    ) {
        val thumb = video.thumbnail
        if (video.url.isNotEmpty()) {
            VideoView(
                state = rememberVideoPlaybackState(url = video.url),
                modifier = Modifier.fillMaxSize(),
                onVideoAspectRatioAvailable = {
                    val resizeSurface = aspectRatio == it
                    aspectRatio = it
                    VideoAspectRatioCache[video.url] = it
                    resizeSurface
                },
            )
        } else if (!thumb.isNullOrEmpty()) {
            AsyncImage(
                request = ImageRequest.Url(thumb),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
