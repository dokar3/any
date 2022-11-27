package any.ui.post.item

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.image.ImageRequest
import any.base.image.ImageState
import any.ui.common.image.AsyncImage
import any.ui.common.theme.darkerImagePlaceholder
import any.ui.common.theme.imagePlaceholder

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FullWidthImageElementItem(
    url: String,
    imageIndex: Int,
    showIndex: Boolean,
    defaultImageRatio: Float,
    onDetectImageSize: (IntSize) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val evenColor = MaterialTheme.colors.imagePlaceholder
    val oddColor = MaterialTheme.colors.darkerImagePlaceholder

    val feedback = LocalHapticFeedback.current

    var reloadFactor by remember { mutableStateOf(0) }

    BoxWithConstraints(
        modifier = modifier,
    ) {
        var picSize by remember(url) { mutableStateOf(IntSize.Zero) }
        var isLoaded by remember(url) { mutableStateOf(false) }
        var error: Throwable? by remember(url) { mutableStateOf(null) }

        val w = constraints.maxWidth
        val ratio = if (ImageSizeCache.contains(url)) {
            val size = ImageSizeCache.get(url)!!
            size.width.toFloat() / size.height
        } else if (picSize.width != 0 && picSize.height != 0) {
            val size = IntSize(picSize.width, picSize.height)
            onDetectImageSize(size)
            ImageSizeCache.put(url, size)
            picSize.width.toFloat() / picSize.height
        } else {
            defaultImageRatio
        }
        val height = with(LocalDensity.current) { (w / ratio).toDp() }

        val isEvenItem = imageIndex % 2 == 0

        AsyncImage(
            request = ImageRequest.Downloadable(url = url),
            contentDescription = "",
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .background(if (isEvenItem) evenColor else oddColor)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                    onLongClick = {
                        feedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    },
                ),
            onState = { newState ->
                when (newState) {
                    is ImageState.Failure -> {
                        error = newState.error
                    }
                    is ImageState.Loading -> {
                        picSize = newState.size ?: IntSize.Zero
                        error = null
                    }
                    is ImageState.Success -> {
                        picSize = newState.size
                        error = null
                        isLoaded = true
                    }
                }
            },
            reloadFactor = reloadFactor,
            showProgressbar = true,
        )

        if (!isLoaded && showIndex) {
            // Image number
            val align = if (isEvenItem) {
                Alignment.BottomStart
            } else {
                Alignment.BottomEnd
            }
            val offset = if (isEvenItem) {
                DpOffset(x = (-16).dp, y = 16.dp)
            } else {
                DpOffset(x = 16.dp, y = 16.dp)
            }
            Text(
                text = (imageIndex + 1).toString(),
                modifier = Modifier
                    .align(align)
                    .offset(offset.x, offset.y),
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.4f),
                fontSize = 160.sp,
                fontWeight = FontWeight.Bold
            )
        }

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
