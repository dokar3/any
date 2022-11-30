package any.richtext

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import any.base.util.indexOfFirst

fun buildRichString(builder: RichString.Builder.() -> Unit): RichString {
    return RichString.Builder().also(builder).build()
}

/**
 * A wrapper of [AnnotatedString]. This string cannot be used directly by Compose's `Text()`,
 * instead the [RichString.render] returns a rendered [AnnotatedString] that can be passed
 * to the `Text()` composable.
 */
@Immutable
@JvmInline
value class RichString(
    private val text: AnnotatedString = AnnotatedString(""),
) : CharSequence by text {
    constructor(text: String) : this(AnnotatedString(text))

    override fun toString(): String {
        return text.toString()
    }

    override fun subSequence(startIndex: Int, endIndex: Int): RichString {
        return RichString(text.subSequence(startIndex, endIndex))
    }

    fun getStringAnnotations(
        start: Int,
        end: Int,
    ): List<AnnotatedString.Range<String>> {
        return text.getStringAnnotations(start, end)
    }

    fun getStringAnnotations(
        tag: String,
        start: Int,
        end: Int
    ): List<AnnotatedString.Range<String>> {
        return text.getStringAnnotations(tag, start, end)
    }

    inline fun <reified T : RichStringMark> findMarks(index: Int): List<T> {
        return findMarks(index, index)
    }

    inline fun <reified T : RichStringMark> findMarks(start: Int, end: Int): List<T> {
        return getStringAnnotations(start, end)
            .mapNotNull { RichStringMark.fromStringAnnotation(T::class.java, it) }
    }

    inline fun <reified T : RichStringMark> findMark(index: Int): T? {
        return findMark(index, index)
    }

    inline fun <reified T : RichStringMark> findMark(start: Int, end: Int): T? {
        val annotations = getStringAnnotations(start, end)
        for (annotation in annotations) {
            val mark = RichStringMark.fromStringAnnotation(T::class.java, annotation)
            if (mark != null) {
                return mark
            }
        }
        return null
    }

    inline fun <reified T : RichStringMark> splitByMarkType()
            : Pair<List<RichString>, List<T>> {
        val marks = findMarks<T>(0, length)
        val splits = mutableListOf<RichString>()
        var start = 0
        marks.forEach {
            val end = it.start
            splits.add(subSequence(start, end))
            start = it.end
        }
        if (start < length) {
            splits.add(subSequence(start, length))
        }
        return splits to marks
    }

    fun render(
        linkColor: Color,
        inlineCodeBackgroundColor: Color,
    ): AnnotatedString {
        val marks = findMarks<RichStringMark>(0, text.length)
        if (marks.isEmpty()) {
            return text
        }

        val linkStyle = SpanStyle(
            color = linkColor,
            textDecoration = TextDecoration.Underline,
        )
        val inlineCodeStyle = SpanStyle(
            background = inlineCodeBackgroundColor,
            fontFamily = FontFamily.Monospace,
        )

        val builder = AnnotatedString.Builder(text)
        for (mark in marks) {
            when (mark) {
                is ImageMark -> {
                    // TODO: Render inline image
                }

                is LinkMark -> builder.addStyle(
                    style = linkStyle,
                    start = mark.start,
                    end = mark.end
                )

                is InlineCodeBlockMark -> builder.addStyle(
                    style = inlineCodeStyle,
                    start = mark.start,
                    end = mark.end
                )
            }
        }
        return builder.toAnnotatedString()
    }

    fun trim(char: Char): RichString {
        if (isEmpty()) {
            return this
        }
        val start = indexOfFirst { it != char }
        if (start == -1) {
            return subSequence(0, 0)
        }
        val end = indexOfLast { it != char }
        return subSequence(start, end + 1)
    }

    class Builder {
        private val string = AnnotatedString.Builder()

        val length: Int get() = string.length

        fun isEmpty(): Boolean = length == 0

        fun isNotEmpty(): Boolean = length > 0

        fun append(text: String) {
            string.append(text)
        }

        fun append(char: Char) {
            string.append(char)
        }

        fun append(text: AnnotatedString) {
            string.append(text)
        }

        fun append(text: RichString) {
            string.append(text.text)
        }

        fun addStyle(style: SpanStyle, start: Int, end: Int) {
            string.addStyle(style, start, end)
        }

        fun addStyle(style: ParagraphStyle, start: Int, end: Int) {
            string.addStyle(style, start, end)
        }

        fun addMark(mark: RichStringMark) {
            if (mark.additionalText.isNotEmpty()) {
                string.append(mark.additionalText)
            }
            mark.toStringAnnotation().let {
                string.addStringAnnotation(
                    tag = it.tag,
                    annotation = it.item,
                    start = it.start,
                    end = it.end,
                )
            }
        }

        fun applyFontSize(size: TextUnit, start: Int, end: Int) {
            addStyle(SpanStyle(fontSize = size), start, end)
        }

        fun applyBold(start: Int, end: Int) {
            addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
        }

        fun applyItalic(start: Int, end: Int) {
            addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
        }

        fun applyBoldItalic(start: Int, end: Int) {
            addStyle(
                SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
                start,
                end,
            )
        }

        fun applyUnderline(start: Int, end: Int) {
            addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
        }

        fun applyBackground(color: Color, start: Int, end: Int) {
            addStyle(SpanStyle(background = color), start, end)
        }

        fun applyForeground(color: Color, start: Int, end: Int) {
            addStyle(SpanStyle(color = color), start, end)
        }

        fun build(): RichString {
            return RichString(string.toAnnotatedString())
        }
    }
}