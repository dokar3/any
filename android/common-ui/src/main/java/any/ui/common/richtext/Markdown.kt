package any.ui.common.richtext

import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import any.richtext.RichContent
import any.richtext.markdown.DefaultMarkdownParser
import any.richtext.markdown.MarkdownParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun Markdown(
    text: String,
    modifier: Modifier = Modifier,
    parser: MarkdownParser = remember { DefaultMarkdownParser() },
    onLinkClick: ((String) -> Unit)? = null,
    clickable: Boolean = onLinkClick != null,
    onTextClick: (() -> Unit)? = null,
    onTextLongClick: (() -> Unit)? = null,
    color: Color = MaterialTheme.colors.onBackground,
    fontSize: TextUnit = LocalTextStyle.current.fontSize,
    lineHeight: TextUnit = LocalTextStyle.current.lineHeight,
    style: RichTextStyle = RichTextStyle.Default,
    selectionEnabled: Boolean = false,
    backgroundParsing: Boolean = false,
    lazyElements: Boolean = false,
    maxBlocks: Int = Int.MAX_VALUE,
    blockMaxLines: Int = Int.MAX_VALUE,
    blockTextOverflow: TextOverflow = TextOverflow.Clip,
) {
    var content by remember(text, parser, backgroundParsing) {
        if (backgroundParsing) {
            mutableStateOf<RichContent?>(null)
        } else {
            mutableStateOf<RichContent?>(parser.parse(text))
        }
    }

    LaunchedEffect(text, parser) {
        if (text.isEmpty() || !backgroundParsing) {
            return@LaunchedEffect
        }
        launch(Dispatchers.Default) {
            content = parser.parse(text)
        }
    }

    if (content != null) {
        RichText(
            content = content!!,
            modifier = modifier,
            onLinkClick = onLinkClick,
            clickable = clickable,
            onTextClick = onTextClick,
            onTextLongClick = onTextLongClick,
            color = color,
            fontSize = fontSize,
            lineHeight = lineHeight,
            style = style,
            selectionEnabled = selectionEnabled,
            lazyElements = lazyElements,
            maxBlocks = maxBlocks,
            blockMaxLines = blockMaxLines,
            blockTextOverflow = blockTextOverflow,
        )
    }
}