package any.richtext.markdown

import androidx.compose.runtime.Stable
import any.richtext.RichContent

@Stable
interface MarkdownParser {
    fun parse(text: String): RichContent
}