package any.richtext.html

import android.text.Editable
import android.text.Html.TagHandler
import android.text.Spanned
import any.base.richstring.spans.CodeSpan
import any.base.richstring.spans.HeadingSpan
import any.base.richstring.spans.PreSpan
import any.richtext.html.spans.HrSpan
import org.xml.sax.XMLReader

class ExtraHtmlTagHandler : TagHandler {
    override fun handleTag(
        opening: Boolean,
        tag: String,
        output: Editable,
        xmlReader: XMLReader
    ) {
        when (tag.lowercase()) {
            "pre" -> {
                if (opening) {
                    preStart(output)
                } else {
                    preEnd(output)
                }
            }

            "code" -> {
                if (opening) {
                    codeStart(output)
                } else {
                    codeEnd(output)
                }
            }

            "heading1",
            "heading2",
            "heading3",
            "heading4",
            "heading5",
            "heading6" -> {
                if (opening) {
                    headingStart(output, tag[7] - '0')
                } else {
                    headingEnd(output, tag[7] - '0')
                }
            }

            "hr" -> {
                if (opening) {
                    hrStart(output)
                } else {
                    hrEnd(output)
                }
            }
        }
    }

    private fun preStart(output: Editable) {
        val len = output.length
        output.setSpan(PreSpan(), len, len, Spanned.SPAN_MARK_MARK)
    }

    private fun preEnd(output: Editable) {
        val last = getLast(output, PreSpan::class.java) ?: return
        val start = output.getSpanStart(last)
        output.removeSpan(last)
        output.setSpan(
            PreSpan(last.language),
            start,
            output.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private fun codeStart(output: Editable) {
        val len = output.length
        output.setSpan(CodeSpan(), len, len, Spanned.SPAN_MARK_MARK)
    }

    private fun codeEnd(output: Editable) {
        val last = getLast(output, CodeSpan::class.java) ?: return
        val start = output.getSpanStart(last)
        output.removeSpan(last)
        output.setSpan(CodeSpan(), start, output.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun headingStart(output: Editable, level: Int) {
        val len = output.length
        output.setSpan(HeadingSpan(level), len, len, Spanned.SPAN_MARK_MARK)
    }

    private fun headingEnd(output: Editable, level: Int) {
        val last = getLast(output, HeadingSpan::class.java) ?: return
        val start = output.getSpanStart(last)
        output.removeSpan(last)
        output.setSpan(HeadingSpan(level), start, output.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun hrStart(output: Editable) {
        val len = output.length
        output.append("\uFFFC")
        output.setSpan(HrSpan(), len, len, Spanned.SPAN_MARK_MARK)
    }

    private fun hrEnd(output: Editable) {
        val last = getLast(output, HrSpan::class.java) ?: return
        val start = output.getSpanStart(last)
        output.removeSpan(last)
        output.setSpan(HrSpan(), start, output.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun <T> getLast(text: Spanned, kind: Class<T>): T? {
        return text.getSpans(0, text.length, kind).lastOrNull()
    }
}