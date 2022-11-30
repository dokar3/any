package any.richtext

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString

/**
 * Render and concat all elements to a [AnnotatedString]
 */
fun List<RichElement>.renderTexts(
    linkColor: Color,
    inlineCodeBackgroundColor: Color,
): AnnotatedString {
    val list = this
    return buildAnnotatedString {
        for (i in list.indices) {
            val element = list[i]
            append(
                element.text.render(
                    linkColor = linkColor,
                    inlineCodeBackgroundColor = inlineCodeBackgroundColor,
                )
            )
            if (i != list.lastIndex) {
                append('\n')
            }
        }
    }
}

@Immutable
sealed interface RichElement {
    val text: RichString

    @Immutable
    class Heading(override val text: RichString, val level: Int) : RichElement

    @Immutable
    class Text(override val text: RichString) : RichElement

    @Immutable
    class Image(val url: String) : RichElement {
        override val text: RichString = RichString(url)
    }

    @Immutable
    class UnorderedListItem(val content: RichContent) : RichElement {
        override val text: RichString = content.toRichString()
    }

    @Immutable
    class OrderedListItem(val content: RichContent, val order: Int) : RichElement {
        override val text: RichString = content.toRichString()
    }

    @Immutable
    class CodeBlock(val code: String, val language: String? = null) : RichElement {
        override val text: RichString = RichString(code)
    }

    @Immutable
    class BlockQuote(val content: RichContent) : RichElement {
        override val text: RichString = content.toRichString()
    }

    @Immutable
    object HorizontalRule : RichElement {
        override val text: RichString = RichString("---")
    }
}

fun <T : RichElement> List<T>.joinToRichString(separator: String): RichString {
    val list = this
    return buildRichString {
        for (i in indices) {
            append(list[i].text)
            if (i != lastIndex) {
                append(separator)
            }
        }
    }
}