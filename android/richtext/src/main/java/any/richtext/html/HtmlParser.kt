package any.richtext.html

import androidx.compose.runtime.Stable
import any.richtext.RichContent

@Stable
interface HtmlParser {
    fun parse(html: String): RichContent
}
