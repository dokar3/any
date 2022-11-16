package any.ui.post.header

import any.base.R as BaseR
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.domain.entity.UiPost
import any.domain.entity.UiServiceManifest
import any.ui.common.LocalFontScale
import any.ui.common.theme.placeholder
import any.ui.common.widget.Avatar
import any.ui.common.widget.OptionsText
import any.ui.common.widget.TextOption
import any.ui.post.item.ItemsDefaults

private const val INFO_ANNOTATION_AUTHOR = "author"

@Composable
internal fun ArticleHeader(
    post: UiPost,
    service: UiServiceManifest?,
    onSearchTextRequest: (String) -> Unit,
    onUserClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.padding(
            start = ItemsDefaults.ItemHorizontalSpacing,
            top = 0.dp,
            end = ItemsDefaults.ItemHorizontalSpacing,
            bottom = 16.dp - ItemsDefaults.ItemVerticalSpacing
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

        Spacer(modifier = Modifier.height(8.dp))

        val info = remember(post, service) {
            buildAnnotatedString {
                val author = post.author ?: res.getString(BaseR.string.unknown)
                if (post.author != null) {
                    addStringAnnotation(
                        tag = INFO_ANNOTATION_AUTHOR,
                        annotation = "",
                        start = length,
                        end = length + author.length,
                    )
                }
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(author)
                }
                append('\n')

                val serviceName = service?.name
                if (!serviceName.isNullOrEmpty()) {
                    append(serviceName)
                }

                val category = post.category
                if (!category.isNullOrEmpty()) {
                    append(" | ")

                    append(category)
                }

                val date = post.date
                if (!date.isNullOrEmpty()) {
                    append(" | ")

                    append(date)
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val author = post.author
            Avatar(
                name = post.author ?: "",
                url = post.avatar,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (author != null) {
                            onUserClick()
                        }
                    },
                ),
            )

            Spacer(modifier = Modifier.width(8.dp))

            SelectionContainer(modifier = Modifier.weight(1f)) {
                var infoTextLayoutResult by remember {
                    mutableStateOf<TextLayoutResult?>(null)
                }
                Text(
                    text = info,
                    fontSize = 14.sp * fontScale,
                    modifier = Modifier.pointerInput(info) {
                        detectTapGestures(
                            onTap = { pos ->
                                val layoutResult = infoTextLayoutResult
                                    ?: return@detectTapGestures
                                val idx = layoutResult.getOffsetForPosition(pos)
                                val annotations = info.getStringAnnotations(
                                    tag = INFO_ANNOTATION_AUTHOR,
                                    start = idx,
                                    end = idx,
                                )
                                if (annotations.isNotEmpty()) {
                                    onUserClick()
                                }
                            },
                        )
                    },
                    onTextLayout = { infoTextLayoutResult = it },
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Divider(
            modifier = Modifier.padding(top = 16.dp),
            color = MaterialTheme.colors.placeholder,
        )
    }
}