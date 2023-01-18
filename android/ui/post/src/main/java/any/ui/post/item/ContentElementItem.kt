package any.ui.post.item

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import any.base.util.Intents
import any.domain.entity.UiContentElement
import any.ui.common.richtext.RichTextBlocks

private const val CONTENT_TYPE_TEXT = 0
private const val CONTENT_TYPE_RICH_TEXT = 1
private const val CONTENT_TYPE_BLOCK_QUOTE = 2
private const val CONTENT_TYPE_CODE_BLOCK = 3
private const val CONTENT_TYPE_HEADING = 4
private const val CONTENT_TYPE_OL = 5
private const val CONTENT_TYPE_UL = 6
private const val CONTENT_TYPE_IMAGE = 7
private const val CONTENT_TYPE_FULL_WIDTH_IMAGE = 8
private const val CONTENT_TYPE_VIDEO = 9
private const val CONTENT_TYPE_CAROUSEL = 10
private const val CONTENT_TYPE_SECTION = 11
private const val CONTENT_TYPE_HR = 12

internal fun LazyListScope.contentElementItem(
    onDetectedPicSize: (IntSize) -> Unit,
    onClick: () -> Unit,
    onLinkClick: (String) -> Unit,
    onImageClick: (images: List<String>?, url: String) -> Unit,
    onImageLongClick: (images: List<String>?, url: String) -> Unit,
    onAddToBookmarkClick: () -> Unit,
    imageIndex: Int,
    showIndexOnFullWidthImage: Boolean,
    element: UiContentElement,
) {
    when (element) {
        is UiContentElement.Text -> {
            item(
                contentType = CONTENT_TYPE_TEXT,
            ) {
                TextItem(text = element.value)
            }
        }

        is UiContentElement.RichText -> {
            item(
                contentType = CONTENT_TYPE_RICH_TEXT,
            ) {
                RichStringItem(
                    richString = element.text,
                    onLinkClick = onLinkClick,
                ) { text, textLayoutUpdater, clickModifier ->
                    RichTextBlocks.Text(
                        text = text,
                        onTextLayout = textLayoutUpdater,
                        modifier = clickModifier,
                        contentPadding = ItemsDefaults.ContentPadding,
                    )
                }
            }
        }

        is UiContentElement.BlockQuote -> {
            item(
                contentType = CONTENT_TYPE_BLOCK_QUOTE,
            ) {
                RichStringItem(
                    richString = element.text,
                    onLinkClick = onLinkClick,
                ) { text, textLayoutUpdater, clickModifier ->
                    RichTextBlocks.BlockQuote(
                        contentPadding = ItemsDefaults.ContentPadding,
                    ) {
                        Text(
                            text = text,
                            modifier = clickModifier,
                            onTextLayout = textLayoutUpdater,
                        )
                    }
                }
            }
        }

        is UiContentElement.CodeBlock -> {
            item(
                contentType = CONTENT_TYPE_CODE_BLOCK,
            ) {
                RichTextBlocks.CodeBlock(
                    text = element.text,
                    language = element.language,
                    contentPadding = ItemsDefaults.ContentPadding,
                )
            }
        }

        is UiContentElement.Heading -> {
            item(
                contentType = CONTENT_TYPE_HEADING,
            ) {
                HeadingItem(
                    onAddToBookmarkClick = onAddToBookmarkClick,
                    onLinkClick = onLinkClick,
                    richString = element.text,
                    level = element.level,
                )
            }
        }

        is UiContentElement.OrderedListItem -> {
            item(
                contentType = CONTENT_TYPE_OL,
            ) {
                RichStringItem(
                    richString = element.text,
                    onLinkClick = onLinkClick,
                ) { text, textLayoutUpdater, clickModifier ->
                    RichTextBlocks.OrderedListItem(
                        order = element.order,
                        contentPadding = ItemsDefaults.ContentPadding,
                    ) {
                        Text(
                            text = text,
                            modifier = clickModifier,
                            onTextLayout = textLayoutUpdater,
                        )
                    }
                }
            }
        }

        is UiContentElement.UnorderedListItem -> {
            item(
                contentType = CONTENT_TYPE_UL,
            ) {
                RichStringItem(
                    richString = element.text,
                    onLinkClick = onLinkClick,
                ) { text, textLayoutUpdater, clickModifier ->
                    RichTextBlocks.UnorderedListItem(
                        modifier = clickModifier,
                        contentPadding = ItemsDefaults.ContentPadding,
                    ) {
                        Text(
                            text = text,
                            onTextLayout = textLayoutUpdater,
                        )
                    }
                }
            }
        }

        is UiContentElement.Image -> {
            item(
                contentType = CONTENT_TYPE_IMAGE,
            ) {
                ImageItem(
                    url = element.url,
                    defaultImageRatio = element.aspectRatio ?: (4 / 5f),
                    onDetectImageSize = onDetectedPicSize,
                    onClick = { onImageClick(null, element.url) },
                    onLongClick = { onImageLongClick(null, element.url) },
                )
            }
        }

        is UiContentElement.FullWidthImage -> {
            item(
                contentType = CONTENT_TYPE_FULL_WIDTH_IMAGE,
            ) {
                FullWidthImageItem(
                    url = element.url,
                    imageIndex = imageIndex,
                    showIndex = showIndexOnFullWidthImage,
                    defaultImageRatio = element.aspectRatio ?: (4 / 5f),
                    onDetectImageSize = onDetectedPicSize,
                    onClick = { onImageClick(null, element.url) },
                    onLongClick = { onImageLongClick(null, element.url) },
                )
            }
        }

        is UiContentElement.Carousel -> {
            item(
                contentType = CONTENT_TYPE_CAROUSEL,
            ) {
                val context = LocalContext.current
                CarouselItem(
                    carousel = element,
                    onPlayVideoClick = { url -> Intents.playVideo(context, url) },
                    onImageClick = { url -> onImageClick(null, url) },
                    onImageLongClick = { url -> onImageLongClick(null, url) },
                )
            }
        }

        is UiContentElement.Video -> {
            item(
                contentType = CONTENT_TYPE_VIDEO,
            ) {
                val context = LocalContext.current
                VideoItem(
                    video = element,
                    onPlayClick = { Intents.playVideo(context, element.url) },
                )
            }
        }

        is UiContentElement.Section -> {
            item(
                contentType = CONTENT_TYPE_SECTION,
            ) {
                SectionItem(
                    chapterName = element.title,
                    onClick = onClick,
                )
            }
        }

        is UiContentElement.HorizontalRule -> {
            item(
                contentType = CONTENT_TYPE_HR,
            ) {
                RichTextBlocks.HorizontalRule()
            }
        }
    }
}
