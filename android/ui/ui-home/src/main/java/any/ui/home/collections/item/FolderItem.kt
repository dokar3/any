package any.ui.home.collections.item

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.StableHolder
import any.base.image.ImageRequest
import any.data.ThumbAspectRatio
import any.data.entity.Folder
import any.data.entity.ServiceViewType
import any.domain.entity.UiPost
import any.domain.post.containsRaw
import any.ui.common.image.AsyncImage
import any.ui.common.modifier.drawCheckMark
import any.ui.common.rememberScale
import any.ui.common.theme.imagePlaceholder
import any.ui.common.theme.placeholder
import any.ui.common.theme.sizes
import any.ui.common.theme.thumb
import any.ui.common.theme.thumbBorder

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FolderItem(
    folder: Folder,
    selectedPosts: StableHolder<Set<UiPost>>,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current

    val posts = folder.posts!!

    Column(
        modifier = modifier
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberScale(),
                onClick = { onClick?.invoke() },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick?.invoke()
                }
            ),
    ) {
        val ratio = ThumbAspectRatio.defaultAspectRatio(ServiceViewType.Grid)
        val cols = 2
        val rows = 2
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .clip(MaterialTheme.shapes.thumb)
                .background(MaterialTheme.colors.placeholder)
                .border(
                    width = MaterialTheme.sizes.thumbBorderStroke,
                    color = MaterialTheme.colors.thumbBorder,
                    shape = MaterialTheme.shapes.thumb,
                )
                .padding(4.dp)
        ) {
            for (i in 0 until rows) {
                Row(modifier = Modifier.weight(1f)) {
                    for (j in 0 until cols) {
                        val idx = i * cols + j
                        if (idx < posts.size) {
                            val post = posts[idx]
                            val isSelected = selectedPosts.value.containsRaw(post)
                            val thumbnail = post.media?.firstOrNull()?.thumbnailOrNull() ?: ""
                            AsyncImage(
                                request = ImageRequest.Url(thumbnail),
                                contentDescription = "",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f)
                                    .padding(4.dp)
                                    .shadow(MaterialTheme.sizes.thumbElevation)
                                    .clip(MaterialTheme.shapes.thumb)
                                    .background(MaterialTheme.colors.imagePlaceholder)
                                    .border(
                                        width = MaterialTheme.sizes.thumbBorderStroke,
                                        color = MaterialTheme.colors.thumbBorder,
                                        shape = MaterialTheme.shapes.thumb,
                                    )
                                    .drawCheckMark(
                                        visible = isSelected,
                                        animated = false,
                                        size = 12.dp,
                                    ),
                                fadeIn = true,
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            val textColor = MaterialTheme.colors.onBackground
            val text = remember(folder, textColor) {
                buildAnnotatedString {
                    append(folder.name)
                    append(' ')
                    withStyle(style = SpanStyle(color = textColor.copy(alpha = 0.42f))) {
                        append("(${posts.size})")
                    }
                }
            }
            Text(
                text = text,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            )
        }
    }
}
