package any.ui.common.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.image.ImageRequest
import any.ui.common.image.AsyncImage
import any.ui.common.image.rememberImageColorFilter
import any.ui.common.theme.imagePlaceholder
import any.ui.common.theme.primaryColors
import kotlin.random.Random

private val DefaultSize = 42.dp

@Composable
fun Avatar(
    name: String,
    url: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: Dp = DefaultSize,
    shape: Shape = CircleShape,
    placeholderColor: Color = MaterialTheme.colors.imagePlaceholder,
    memoryCacheEnabled: Boolean = true,
    diskCacheEnabled: Boolean = true,
) {
    if (!url.isNullOrEmpty()) {
        AsyncImage(
            request = ImageRequest.Url(
                url = url,
                memoryCacheEnabled = memoryCacheEnabled,
                diskCacheEnabled = diskCacheEnabled,
            ),
            contentDescription = contentDescription,
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(placeholderColor),
        )
    } else {
        val char = name.firstOrNull() ?: ' '
        val primaryColors = MaterialTheme.colors.primaryColors
        val backgroundColor = remember(char, primaryColors) {
            if (char != ' ') {
                val idx = Random(char.code).nextInt(primaryColors.size)
                primaryColors[idx]
            } else {
                placeholderColor
            }
        }
        val colorFilter = rememberImageColorFilter()
        BoxWithConstraints(
            modifier = modifier
                .size(size)
                .clip(shape)
                .drawWithContent {
                    drawRect(color = placeholderColor)
                    drawRect(
                        color = backgroundColor,
                        colorFilter = colorFilter,
                    )
                    drawContent()
                },
            contentAlignment = Alignment.Center,
        ) {
            val density = LocalDensity.current
            val scale = constraints.maxWidth / with(density) { DefaultSize.toPx() }
            Text(
                text = char.uppercase(),
                color = Color.White,
                fontSize = 24.sp * scale,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}