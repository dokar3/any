package any.ui.post.header

import any.base.R as BaseR
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.image.ImageRequest
import any.base.image.ImageState
import any.data.ThumbAspectRatio
import any.data.entity.Post
import any.domain.entity.UiPost
import any.domain.entity.UiServiceManifest
import any.ui.common.LocalFontScale
import any.ui.common.image.AsyncImage
import any.ui.common.theme.imagePlaceholder
import any.ui.common.theme.placeholder
import any.ui.common.theme.sizes
import any.ui.common.theme.thumb
import any.ui.common.theme.thumbBorder
import any.ui.common.widget.Avatar
import any.ui.common.widget.OptionsText
import any.ui.common.widget.TextOption
import any.ui.post.PostUiState
import any.ui.post.item.ItemsDefaults

@Composable
internal fun ComicHeader(
    post: UiPost,
    service: UiServiceManifest?,
    uiState: PostUiState,
    onContinueReadingClick: () -> Unit,
    onSearchTextRequest: (String) -> Unit,
    onUserClick: () -> Unit,
    modifier: Modifier = Modifier,
    defThumbAspectRatio: Float = 4f / 5,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = ItemsDefaults.ItemHorizontalSpacing,
                top = 0.dp,
                end = ItemsDefaults.ItemHorizontalSpacing,
                bottom = 16.dp
            )
    ) {
        val fontScale = LocalFontScale.current
        val res = LocalContext.current.resources
        val textOptions = remember {
            listOf(
                TextOption.Copy(res.getString(BaseR.string.copy)),
                TextOption.Search(res.getString(BaseR.string.search)),
            )
        }
        if (post.title.isNotEmpty() && post.title.isNotBlank()) {
            OptionsText(
                text = post.title,
                options = textOptions,
                onOptionSelected = {
                    if (it is TextOption.Search) {
                        onSearchTextRequest(post.title)
                    }
                },
                fontSize = 20.sp * fontScale,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            val thumbnail: String
            val thumbAspectRatio: Float
            val thumbMedia = post.media?.firstOrNull()
            if (thumbMedia != null && thumbMedia.type != Post.Media.Type.Video) {
                thumbnail = thumbMedia.thumbnail ?: thumbMedia.url
                val r = thumbMedia.aspectRatio
                    ?: service?.mediaAspectRatio
                    ?: defThumbAspectRatio
                thumbAspectRatio = r.coerceAtLeast(ThumbAspectRatio.MIN_THUMB_ASPECT_RATIO)
            } else {
                thumbnail = ""
                thumbAspectRatio = defThumbAspectRatio
            }
            var previousThumb: String? by remember { mutableStateOf(null) }
            AsyncImage(
                request = ImageRequest.Url(thumbnail),
                contentDescription = null,
                modifier = Modifier
                    .width(180.dp)
                    .aspectRatio(thumbAspectRatio)
                    .shadow(MaterialTheme.sizes.thumbElevation)
                    .clip(MaterialTheme.shapes.thumb)
                    .background(MaterialTheme.colors.imagePlaceholder)
                    .border(
                        width = MaterialTheme.sizes.thumbBorderStroke,
                        color = MaterialTheme.colors.thumbBorder,
                        shape = MaterialTheme.shapes.thumb,
                    ),
                contentScale = ContentScale.Crop,
                placeholder = Snapshot.withoutReadObservation {
                    previousThumb?.let { ImageRequest.Url(it) }
                },
                onState = {
                    if (it is ImageState.Success) {
                        previousThumb = thumbnail
                    }
                },
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                val avatar = post.avatar
                val author = post.author
                Row(
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            if (author != null) {
                                onUserClick()
                            }
                        },
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(
                        name = author ?: "?",
                        url = avatar,
                        size = 32.dp,
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    OptionsText(
                        text = author ?: stringResource(BaseR.string.unknown),
                        options = textOptions,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                        onOptionSelected = {
                            if (it is TextOption.Search) {
                                onSearchTextRequest(author ?: "")
                            }
                        },
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))


                if (service != null) {
                    LabeledItem(label = stringResource(BaseR.string.from), text = service.name)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (post.date != null) {
                    LabeledItem(label = stringResource(BaseR.string.date), text = post.date)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (post.category != null) {
                    LabeledItem(
                        label = stringResource(BaseR.string.category),
                        text = post.category,
                        textOptions = textOptions,
                        onSearchTextRequest = onSearchTextRequest,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (post.rating != null) {
                    LabeledItem(label = stringResource(BaseR.string.rating), text = post.rating)
                }

                if (post.readPosition > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = onContinueReadingClick,
                        elevation = ButtonDefaults.elevation(
                            defaultElevation = 0.dp,
                        ),
                        shape = CircleShape,
                        contentPadding = PaddingValues(
                            horizontal = 12.dp,
                            vertical = 4.dp
                        ),
                    ) {
                        Text(
                            text = stringResource(BaseR.string.continue_reading),
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val sectionCount: Int
        val pageCount: Int
        if (uiState.contentElements.isNotEmpty()) {
            sectionCount = uiState.sectionCount
            pageCount = uiState.images.size
        } else {
            sectionCount = 0
            pageCount = 0
        }
        SectionsAndPagesInfo(
            sectionCount = sectionCount,
            pageCount = pageCount,
        )
    }
}

@Composable
private fun LabeledItem(
    label: String,
    text: String?,
    modifier: Modifier = Modifier,
    textOptions: List<TextOption>? = null,
    onSearchTextRequest: ((String) -> Unit)? = null,
) {
    val fontScale = LocalFontScale.current
    Text(
        text = label,
        modifier = modifier,
        fontSize = 14.sp * fontScale,
        fontWeight = FontWeight.Medium,
    )

    Spacer(modifier = Modifier.height(4.dp))

    val fontSize = LocalTextStyle.current.fontSize * fontScale

    if (!text.isNullOrEmpty()) {
        if (!textOptions.isNullOrEmpty()) {
            OptionsText(
                text = text,
                fontSize = fontSize,
                options = textOptions,
                onOptionSelected = {
                    if (it is TextOption.Search) {
                        onSearchTextRequest?.invoke(text)
                    }
                },
            )
        } else {
            SelectionContainer {
                Text(text = text, fontSize = fontSize)
            }
        }
    } else {
        Text(text = "", fontSize = fontSize)
    }
}

@Composable
internal fun SectionsAndPagesInfo(
    sectionCount: Int,
    pageCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val res = LocalContext.current.resources
            val text = remember(sectionCount, pageCount) {
                buildAnnotatedString {
                    val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
                    if (sectionCount > 0) {
                        withStyle(boldStyle) {
                            append(sectionCount.toString())
                        }
                        append(' ')
                        append(res.getString(BaseR.string.sections))
                    }

                    append(' ')
                    withStyle(boldStyle) {
                        append(pageCount.toString())
                    }
                    append(' ')
                    append(res.getString(BaseR.string.pages))
                }
            }
            Text(text = text, fontSize = 14.sp * LocalFontScale.current)
        }

        Divider(
            modifier = Modifier.padding(top = 12.dp),
            color = MaterialTheme.colors.placeholder,
        )
    }
}
