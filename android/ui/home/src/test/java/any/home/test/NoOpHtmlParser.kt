package any.home.test

import any.richtext.RichContent
import any.richtext.html.HtmlParser

object NoOpHtmlParser : HtmlParser {
    override fun parse(html: String): RichContent {
        return RichContent.Empty
    }
}