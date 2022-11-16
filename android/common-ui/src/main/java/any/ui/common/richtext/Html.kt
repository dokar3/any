package any.ui.common.richtext

import androidx.compose.foundation.layout.Spacer
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
import any.richtext.html.DefaultHtmlParser
import any.richtext.html.HtmlParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun Html(
    html: String?,
    modifier: Modifier = Modifier,
    onLinkClick: ((url: String) -> Unit)? = null,
    clickable: Boolean = onLinkClick != null,
    onTextClick: (() -> Unit)? = null,
    onTextLongClick: (() -> Unit)? = null,
    color: Color = MaterialTheme.colors.onBackground,
    fontSize: TextUnit = LocalTextStyle.current.fontSize,
    lineHeight: TextUnit = LocalTextStyle.current.lineHeight,
    style: RichTextStyle = RichTextStyle.Default,
    parser: HtmlParser = remember { DefaultHtmlParser() },
    selectionEnabled: Boolean = false,
    backgroundParsing: Boolean = false,
    lazyElements: Boolean = false,
    maxBlocks: Int = Int.MAX_VALUE,
    blockMaxLines: Int = Int.MAX_VALUE,
    blockTextOverflow: TextOverflow = TextOverflow.Clip,
) {
    var content by remember(html, parser, backgroundParsing) {
        if (backgroundParsing) {
            mutableStateOf<RichContent?>(null)
        } else {
            mutableStateOf<RichContent?>(parser.parse(html ?: ""))
        }
    }

    LaunchedEffect(html, parser) {
        if (html.isNullOrEmpty() || !backgroundParsing) {
            return@LaunchedEffect
        }
        launch(Dispatchers.Default) {
            content = parser.parse(html)
        }
    }

    if (content != null) {
        val c = content!!
        RichText(
            content = c,
            onLinkClick = onLinkClick,
            modifier = modifier,
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
    } else {
        Spacer(modifier = modifier)
    }
}
