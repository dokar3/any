package any.richtext.html

import android.graphics.Typeface
import android.os.Build
import android.text.Html
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.BulletSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.QuoteSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import any.base.richstring.spans.CodeSpan
import any.base.richstring.spans.HeadingSpan
import any.base.richstring.spans.PreSpan
import any.base.util.indexOfFirst
import any.richtext.InlineCodeBlockMark
import any.richtext.LinkMark
import any.richtext.RichContent
import any.richtext.RichElement
import any.richtext.RichString
import any.richtext.buildRichString
import org.jsoup.Jsoup

@Stable
class DefaultHtmlParser : HtmlParser {
    @Suppress("deprecation")
    override fun parse(html: String): RichContent {
        // Process <pre> and <code> tags
        val doc = Jsoup.parse(html)
        val preElements = doc.select("pre")
        for (ele in preElements) {
            val prevHtml = ele.html()
                .replace("\n", "<br\\>")
                .replace(" ", "&nbsp;")
                .replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;")
            ele.html(prevHtml)
        }

        // Process <h1> - <h6> tags
        val headingElements = doc.select("h1, h2, h3, h4, h5, h6")
        for (ele in headingElements) {
            // Change to a custom heading tag, this will allow us to handle headings in
            // our custom tag handler
            ele.tagName("heading" + ele.tagName()[1])
        }

        val processedHtml = doc.html()

        val spanned = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(processedHtml, Html.FROM_HTML_MODE_COMPACT, null, ExtraHtmlTagHandler())
        } else {
            Html.fromHtml(processedHtml, null, ExtraHtmlTagHandler())
        }

        val spans = spanned.getSpans(0, spanned.length, Any::class.java)
        val spannedString = spanned.toString()
        val content = RichContent.Builder()
        var startIndex = 0

        if (spannedString.isEmpty()) {
            content.addElement(RichElement.Text(RichString()))
            return content.build()
        }

        // Render to AnnotatedString
        val richString = buildRichString {
            append(spannedString)
            spans.forEach { span ->
                val start = spanned.getSpanStart(span)
                val end = spanned.getSpanEnd(span)
                applySpan(span, start, end)
            }
        }

        // Split by special tags
        spans.forEach { span ->
            when (span) {
                is HeadingSpan -> {
                    startIndex = content.addTextAndElement(
                        spanned = spanned,
                        richString = richString,
                        startIndex = startIndex,
                        span = span,
                    ) { start, end ->
                        RichElement.Heading(
                            text = richString.subSequence(start, end),
                            level = span.level
                        )
                    }
                }

                is ImageSpan -> {
                    startIndex = content.addTextAndElement(
                        spanned = spanned,
                        richString = richString,
                        startIndex = startIndex,
                        span = span,
                    ) { _, _ ->
                        val url = span.source
                        if (!url.isNullOrEmpty()) {
                            RichElement.Image(url)
                        } else {
                            null
                        }
                    }
                }

                is BulletSpan -> {
                    startIndex = content.addTextAndElement(
                        spanned = spanned,
                        richString = richString,
                        startIndex = startIndex,
                        span = span,
                    ) { start, end ->
                        val itemContent = RichContent.Builder()
                        val itemText = if (richString[end - 1] == '\n') {
                            richString.subSequence(start, end - 1)
                        } else {
                            richString.subSequence(start, end)
                        }
                        itemContent.addElement(RichElement.Text(itemText))
                        RichElement.UnorderedListItem(itemContent.build())
                    }
                }

                is QuoteSpan -> {
                    startIndex = content.addTextAndElement(
                        spanned = spanned,
                        richString = richString,
                        startIndex = startIndex,
                        span = span,
                    ) { start, end ->
                        val itemContent = RichContent.Builder()
                        val itemText = if (richString[end - 1] == '\n') {
                            richString.subSequence(start, end - 1)
                        } else {
                            richString.subSequence(start, end)
                        }
                        itemContent.addElement(RichElement.Text(itemText))
                        RichElement.BlockQuote(itemContent.build())
                    }
                }

                is PreSpan -> {
                    startIndex = content.addTextAndElement(
                        spanned = spanned,
                        richString = richString,
                        startIndex = startIndex,
                        span = span,
                    ) { start, end ->
                        RichElement.CodeBlock(richString.subSequence(start, end).toString())
                    }
                }
            }
        }

        // Add rest trimmed text
        val start = richString.indexOfFirst(start = startIndex) { it != '\n' }
        val end = richString.indexOfLast { it != '\n' }
        if (start in 0 until end) {
            content.addElement(RichElement.Text(richString.subSequence(start, end + 1)))
        }

        return content.build()
    }

    private fun <T> RichContent.Builder.addTextAndElement(
        spanned: Spanned,
        richString: RichString,
        startIndex: Int,
        span: T,
        append: (spanStart: Int, spanEnd: Int) -> RichElement?,
    ): Int {
        val spanStart = spanned.getSpanStart(span)
        val spanEnd = spanned.getSpanEnd(span)

        if (startIndex < spanStart) {
            val string = richString.subSequence(startIndex, spanStart).trim('\n')
            if (string.isNotEmpty()) {
                addElement(RichElement.Text(string))
            }
        }

        if (spanEnd > spanStart) {
            append(spanStart, spanEnd)?.let {
                addElement(it)
            }
        }

        return spanEnd
    }

    private fun RichString.Builder.applySpan(span: Any, start: Int, end: Int) {
        when (span) {
            is RelativeSizeSpan -> applyFontSize(span.sizeChange.em, start, end)

            is AbsoluteSizeSpan -> applyFontSize(span.size.sp, start, end)

            is StyleSpan -> when (span.style) {
                Typeface.BOLD -> applyBold(start, end)
                Typeface.ITALIC -> applyItalic(start, end)
                Typeface.BOLD_ITALIC -> applyBoldItalic(start, end)
            }

            is UnderlineSpan -> applyUnderline(start, end)

            is BackgroundColorSpan -> applyBackground(Color(span.backgroundColor), start, end)

            is ForegroundColorSpan -> applyForeground(Color(span.foregroundColor), start, end)

            is URLSpan -> addMark(LinkMark(span.url, start, end))

            is CodeSpan -> addMark(InlineCodeBlockMark(start, end))
        }
    }
}