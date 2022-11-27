package any.ui.post.item

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import any.richtext.LinkMark
import any.richtext.RichString
import any.ui.common.richtext.RichTextStyle

@Composable
internal fun RichStringItem(
    richString: RichString,
    onLinkClick: (String) -> Unit,
    onOtherTextClick: (() -> Unit)? = null,
    content: @Composable (
        text: AnnotatedString,
        textLayoutUpdater: (TextLayoutResult) -> Unit,
        clickModifier: Modifier
    ) -> Unit,
) {
    val richTextStyle = RichTextStyle.Default
    val text = richString.render(
        linkColor = richTextStyle.linkColor,
        inlineCodeBackgroundColor = richTextStyle.inlineCodeBackground,
    )
    val textLayoutResult = remember {
        mutableStateOf<TextLayoutResult?>(null)
    }
    val modifier = remember(richString) {
        Modifier.pointerInput(null) {
            detectTapGestures { pos ->
                val result = textLayoutResult.value ?: return@detectTapGestures
                val offset = result.getOffsetForPosition(pos)
                val linkMark = richString.findMark<LinkMark>(offset)
                if (linkMark != null) {
                    onLinkClick(linkMark.url)
                } else {
                    onOtherTextClick?.invoke()
                }
            }
        }
    }
    content(text, { textLayoutResult.value = it }, modifier)
}
