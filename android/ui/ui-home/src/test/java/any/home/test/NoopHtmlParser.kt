package any.home.test

import any.richtext.RichContent
import any.richtext.html.HtmlParser

object NoopHtmlParser : HtmlParser {
    override fun parse(html: String): RichContent {
        return RichContent.Empty
    }
}