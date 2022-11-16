package any.domain.entity

import any.data.entity.ContentElement
import any.richtext.RichString

sealed class UiContentElement {
    class Text(val value: String) : UiContentElement()

    class RichText(val text: RichString) : UiContentElement()

    class Heading(val text: RichString, val level: Int) : UiContentElement()

    class OrderedListItem(val text: RichString, val order: Int) : UiContentElement()

    class UnorderedListItem(val text: RichString) : UiContentElement()

    class BlockQuote(val text: RichString) : UiContentElement()

    class CodeBlock(val text: String, val language: String?) : UiContentElement()

    class Image(val url: String, val aspectRatio: Float?) : UiContentElement()

    class FullWidthImage(val url: String, val aspectRatio: Float?) : UiContentElement()

    class Carousel(
        val items: List<ContentElement.Carousel.Item>,
        val aspectRatio: Float?,
    ) : UiContentElement()

    class Video(
        val url: String,
        val thumbnail: String?,
        val aspectRatio: Float?
    ) : UiContentElement()

    class Section(val title: String) : UiContentElement()

    object HorizontalRule : UiContentElement()
}
