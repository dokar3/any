package any.domain.post

import any.data.ThumbAspectRatio
import any.data.entity.ContentElement
import any.data.entity.ContentElementType
import any.data.json.Json
import any.data.json.fromJson
import any.domain.entity.UiContentElement
import any.richtext.RichElement
import any.richtext.html.DefaultHtmlParser
import any.richtext.html.HtmlParser

class PostContentParser(
    private val htmlParser: HtmlParser = DefaultHtmlParser(),
    private val json: Json = Json,
) {
    fun parse(
        content: List<ContentElement>,
        reverseElement: Boolean,
    ): ParsedPostContent {
        val uiElements = content
            .filter { it.value != null }
            .let { if (reverseElement) it.reversed() else it }
            .mapNotNull { runCatching { it.toUiElements() }.getOrNull() }
            .flatten()

        val images = mutableListOf<String>()
        for (element in uiElements) {
            when (element) {
                is UiContentElement.Image -> {
                    images.add(element.url)
                }

                is UiContentElement.FullWidthImage -> {
                    images.add(element.url)
                }

                is UiContentElement.Carousel -> {
                    images.addAll(element.items.mapNotNull { it.image })
                }

                else -> {}
            }
        }

        val sections = mutableListOf<ContentSection>()
        for ((index, element) in uiElements.withIndex()) {
            val section = when (element) {
                is UiContentElement.Heading -> ContentSection(
                    name = element.text.toString(),
                    targetElementIndex = index,
                    depth = element.level - 1,
                )

                is UiContentElement.Section -> ContentSection(
                    name = element.title,
                    targetElementIndex = index,
                )

                else -> null
            }
            if (section != null) {
                sections.add(section)
            }
        }
        if (sections.isEmpty() || sections.first().targetElementIndex != 0) {
            sections.add(0, ContentSection.start())
        }
        if (sections.last().targetElementIndex != uiElements.lastIndex) {
            sections.add(ContentSection.end(uiElements.lastIndex))
        }

        return ParsedPostContent(uiElements, images, sections)
    }

    private fun ContentElement.toUiElements(): List<UiContentElement> {
        return when (this.type) {
            ContentElementType.Text -> {
                listOf(UiContentElement.Text(value ?: ""))
            }

            ContentElementType.Html -> {
                htmlParser.parse(value ?: "").elements.map {
                    when (it) {
                        is RichElement.BlockQuote -> {
                            UiContentElement.BlockQuote(text = it.text)
                        }

                        is RichElement.CodeBlock -> {
                            UiContentElement.CodeBlock(
                                text = it.text.toString(),
                                language = it.language
                            )
                        }

                        is RichElement.Heading -> {
                            UiContentElement.Heading(text = it.text, level = it.level)
                        }

                        is RichElement.Image -> {
                            UiContentElement.Image(url = it.url, aspectRatio = null)
                        }

                        is RichElement.OrderedListItem -> {
                            UiContentElement.OrderedListItem(text = it.text, order = it.order)
                        }

                        is RichElement.UnorderedListItem -> {
                            UiContentElement.UnorderedListItem(text = it.text)
                        }

                        is RichElement.Text -> {
                            UiContentElement.RichText(text = it.text)
                        }

                        RichElement.HorizontalRule -> {
                            UiContentElement.HorizontalRule
                        }
                    }
                }
            }

            ContentElementType.Image -> {
                val image = try {
                    json.fromJson(value ?: "")
                } catch (e: Exception) {
                    ContentElement.Image(url = value ?: "", aspectRatio = null)
                }
                checkNotNull(image) { "Incorrect image element: $value" }
                listOf(
                    UiContentElement.Image(
                        url = image.url,
                        aspectRatio = ThumbAspectRatio.parseOrNull(image.aspectRatio)
                    )
                )
            }

            ContentElementType.FullWidthImage -> {
                val image = try {
                    json.fromJson(value ?: "")
                } catch (e: Exception) {
                    ContentElement.Image(url = value ?: "", aspectRatio = null)
                }
                checkNotNull(image) { "Incorrect image element: $value" }
                listOf(
                    UiContentElement.FullWidthImage(
                        url = image.url,
                        aspectRatio = ThumbAspectRatio.parseOrNull(image.aspectRatio)
                    )
                )
            }

            ContentElementType.Carousel -> {
                val carousel = json.fromJson<ContentElement.Carousel>(value ?: "")
                checkNotNull(carousel) { "Incorrect carousel element: $value" }
                listOf(
                    UiContentElement.Carousel(
                        items = carousel.items,
                        aspectRatio = ThumbAspectRatio.parseOrNull(carousel.aspectRatio)
                    )
                )
            }

            ContentElementType.Video -> {
                val video = json.fromJson<ContentElement.Video>(value ?: "")
                checkNotNull(video) { "Incorrect video element: $value" }
                listOf(
                    UiContentElement.Video(
                        url = video.url,
                        thumbnail = video.thumbnail,
                        aspectRatio = ThumbAspectRatio.parseOrNull(video.aspectRatio)
                    )
                )
            }

            ContentElementType.Section -> {
                listOf(UiContentElement.Section(value ?: ""))
            }
        }
    }

    data class ParsedPostContent(
        val contentElements: List<UiContentElement>,
        val images: List<String>,
        val sections: List<ContentSection>
    ) {
        companion object {
            val Empty = ParsedPostContent(emptyList(), emptyList(), emptyList())
        }
    }
}