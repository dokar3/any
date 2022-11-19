package any.ui.post.item

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.image.ImageRequest
import any.base.image.ImageState
import any.base.util.compose.performLongPress
import any.ui.common.image.AsyncImage
import any.ui.common.theme.imagePlaceholder

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ImageElementItem(
    url: String,
    defaultImageRatio: Float,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onDetectImageSize: ((IntSize) -> Unit)? = null,
    verticalSpacing: Dp = ItemsDefaults.ItemVerticalSpacing,
    horizontalSpacing: Dp = ItemsDefaults.ItemHorizontalSpacing,
) {
    val hapticFeedback = LocalHapticFeedback.current

    BoxWithConstraints(
        modifier = modifier.padding(
            horizontal = horizontalSpacing,
            vertical = verticalSpacing,
        ),
    ) {
        var reloadFactor by remember { mutableStateOf(0) }

        var picSize by remember(url) { mutableStateOf(IntSize.Zero) }
        var error: Throwable? by remember(url) { mutableStateOf(null) }

        val ratio = if (ImageSizeCache.contains(url)) {
            val size = ImageSizeCache.get(url)!!
            size.width.toFloat() / size.height
        } else if (picSize.width != 0 && picSize.height != 0) {
            onDetectImageSize?.invoke(picSize)
            ImageSizeCache.put(url, picSize)
            picSize.width.toFloat() / picSize.height
        } else {
            defaultImageRatio
        }

        AsyncImage(
            request = ImageRequest.Downloadable(url = url),
            contentDescription = "Image",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colors.imagePlaceholder)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.small,
                )
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onClick?.invoke() },
                    onLongClick = {
                        hapticFeedback.performLongPress()
                        onLongClick?.invoke()
                    },
                ),
            contentScale = ContentScale.Crop,
            onState = { newState ->
                when (newState) {
                    is ImageState.Failure -> {
                        error = newState.error
                    }
                    is ImageState.Loading -> {
                        error = null
                        picSize = newState.size ?: IntSize.Zero
                    }
                    is ImageState.Success -> {
                        error = null
                        picSize = newState.size
                    }
                }
            },
            reloadFactor = reloadFactor,
            showProgressbar = true,
        )

        if (error != null) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Image load failed, error: \n ${error!!.message}",
                    textAlign = TextAlign.Center,
                    fontSize = 15.sp
                )
                OutlinedButton(onClick = { reloadFactor++ }) {
                    Text("Reload")
                }
            }
        }
    }
}
