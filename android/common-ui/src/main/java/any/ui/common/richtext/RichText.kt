package any.ui.common.richtext

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import any.base.util.compose.copy
import any.base.util.performLongPress
import any.richtext.LinkMark
import any.richtext.RichContent
import any.richtext.RichElement
import kotlin.math.min

@Composable
fun RichText(
    content: RichContent,
    modifier: Modifier = Modifier,
    onLinkClick: ((url: String) -> Unit)? = null,
    clickable: Boolean = onLinkClick != null,
    onTextClick: (() -> Unit)? = null,
    onTextLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    elementContentPadding: PaddingValues = RichTextBlocks.DefElementContentPadding,
    addPaddingToFirstElementTop: Boolean = true,
    addPaddingToLastElementBottom: Boolean = true,
    color: Color = MaterialTheme.colors.onBackground,
    fontSize: TextUnit = LocalTextStyle.current.fontSize,
    lineHeight: TextUnit = LocalTextStyle.current.lineHeight,
    style: RichTextStyle = RichTextStyle.Default,
    selectionEnabled: Boolean = false,
    lazyElements: Boolean = false,
    maxBlocks: Int = Int.MAX_VALUE,
    blockMaxLines: Int = Int.MAX_VALUE,
    blockTextOverflow: TextOverflow = TextOverflow.Clip,
) {
    val layoutDirection = LocalLayoutDirection.current

    val firstElementPadding = if (addPaddingToFirstElementTop) {
        elementContentPadding
    } else {
        elementContentPadding.copy(
            layoutDirection = layoutDirection,
            top = 0.dp,
        )
    }

    val lastElementPadding = if (addPaddingToLastElementBottom) {
        elementContentPadding
    } else if (content.elements.size == 1) {
        firstElementPadding.copy(
            layoutDirection = layoutDirection,
            bottom = 0.dp,
        )
    } else {
        elementContentPadding.copy(
            layoutDirection = layoutDirection,
            bottom = 0.dp,
        )
    }

    CompositionLocalProvider(
        LocalTextStyle provides LocalTextStyle.current.copy(fontSize = fontSize),
    ) {
        if (lazyElements) {
            LazyColumn(modifier = modifier) {
                val elements = content.elements
                val count = min(elements.size, maxBlocks)
                items(
                    count = count,
                    contentType = { elements[it]::class.java },
                    key = { elements[it].text },
                ) {
                    val padding = when (it) {
                        0 -> {
                            firstElementPadding
                        }

                        elements.lastIndex -> {
                            lastElementPadding
                        }

                        else -> {
                            elementContentPadding
                        }
                    }

                    ElementItem(
                        element = elements[it],
                        style = style,
                        clickable = clickable,
                        onLinkClick = onLinkClick,
                        onTextClick = onTextClick,
                        onTextLongClick = onTextLongClick,
                        selectionEnabled = selectionEnabled,
                        color = color,
                        maxLines = blockMaxLines,
                        lineHeight = lineHeight,
                        textOverflow = blockTextOverflow,
                        interactionSource = interactionSource,
                        contentPadding = padding,
                    )
                }
            }
        } else {
            Column(modifier = modifier) {
                val elements = content.elements
                if (elements.isNotEmpty()) {
                    val count = min(elements.size, maxBlocks)
                    for (i in 0 until count) {
                        val padding = when (i) {
                            0 -> {
                                firstElementPadding
                            }

                            elements.lastIndex -> {
                                lastElementPadding
                            }

                            else -> {
                                elementContentPadding
                            }
                        }

                        ElementItem(
                            element = elements[i],
                            style = style,
                            clickable = clickable,
                            onLinkClick = onLinkClick,
                            onTextClick = onTextClick,
                            onTextLongClick = onTextLongClick,
                            selectionEnabled = selectionEnabled,
                            color = color,
                            maxLines = blockMaxLines,
                            lineHeight = lineHeight,
                            textOverflow = blockTextOverflow,
                            interactionSource = interactionSource,
                            contentPadding = padding,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ElementItem(
    element: RichElement,
    style: RichTextStyle,
    clickable: Boolean,
    onLinkClick: ((url: String) -> Unit)?,
    onTextClick: (() -> Unit)?,
    onTextLongClick: (() -> Unit)?,
    selectionEnabled: Boolean,
    color: Color,
    maxLines: Int,
    lineHeight: TextUnit,
    textOverflow: TextOverflow,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource,
    contentPadding: PaddingValues,
) {
    val hapticFeedback = LocalHapticFeedback.current

    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val linkClickModifier = remember(element.text, clickable) {
        if (!clickable) {
            return@remember Modifier
        }
        Modifier.pointerInput(null) {
            detectTapGestures(
                onPress = {
                    val press = PressInteraction.Press(it)
                    interactionSource.emit(press)
                    if (tryAwaitRelease()) {
                        interactionSource.emit(PressInteraction.Release(press))
                    } else {
                        interactionSource.emit(PressInteraction.Cancel(press))
                    }
                },
                onLongPress = {
                    hapticFeedback.performLongPress()
                    onTextLongClick?.invoke()
                },
                onTap = { pos ->
                    val result = layoutResult
                    if (result == null) {
                        onTextClick?.invoke()
                        return@detectTapGestures
                    }
                    val offset = result.getOffsetForPosition(pos)
                    val linkMark = element.text.findMark<LinkMark>(offset)
                    if (linkMark != null) {
                        onLinkClick?.invoke(linkMark.url)
                    } else {
                        onTextClick?.invoke()
                    }
                },
            )
        }
    }


    when (element) {
        is RichElement.BlockQuote -> {
            RichTextBlocks.BlockQuote(
                modifier = modifier.then(linkClickModifier),
                selectionEnabled = selectionEnabled,
                contentPadding = contentPadding,
            ) {
                RichText(
                    content = element.content,
                    onLinkClick = onLinkClick,
                    clickable = clickable,
                    onTextClick = onTextClick,
                    onTextLongClick = onTextLongClick,
                    interactionSource = interactionSource,
                    elementContentPadding = contentPadding.copy(horizontal = 0.dp),
                    addPaddingToFirstElementTop = false,
                    addPaddingToLastElementBottom = false,
                    color = color,
                    lineHeight = lineHeight,
                    style = style,
                    selectionEnabled = selectionEnabled,
                    lazyElements = false,
                    blockMaxLines = maxLines,
                    blockTextOverflow = textOverflow,
                )
            }
        }

        is RichElement.CodeBlock -> {
            RichTextBlocks.CodeBlock(
                text = element.code,
                language = null,
                modifier = modifier,
                selectionEnabled = selectionEnabled,
                contentPadding = contentPadding,
                color = color,
                maxLines = maxLines,
                lineHeight = lineHeight,
                textOverflow = textOverflow,
            )
        }

        is RichElement.Heading -> {
            val text = remember(element.text) {
                element.text.render(
                    linkColor = style.linkColor,
                    inlineCodeBackgroundColor = style.inlineCodeBackground
                )
            }
            RichTextBlocks.Heading(
                text = text,
                level = element.level,
                onTextLayout = { layoutResult = it },
                modifier = modifier.then(linkClickModifier),
                selectionEnabled = selectionEnabled,
                contentPadding = contentPadding,
                color = color,
                maxLines = maxLines,
                textOverflow = textOverflow,
            )
        }

        is RichElement.Image -> {
            RichTextBlocks.Image(
                url = element.url,
                contentDescription = null,
                modifier = modifier,
                contentPadding = contentPadding,
            )
        }

        is RichElement.OrderedListItem -> {
            RichTextBlocks.OrderedListItem(
                order = element.order,
                modifier = modifier.then(linkClickModifier),
                selectionEnabled = selectionEnabled,
                contentPadding = contentPadding,
            ) {
                RichText(
                    content = element.content,
                    onLinkClick = onLinkClick,
                    clickable = clickable,
                    onTextClick = onTextClick,
                    onTextLongClick = onTextLongClick,
                    interactionSource = interactionSource,
                    elementContentPadding = contentPadding.copy(horizontal = 0.dp),
                    addPaddingToFirstElementTop = false,
                    addPaddingToLastElementBottom = false,
                    color = color,
                    lineHeight = lineHeight,
                    style = style,
                    selectionEnabled = selectionEnabled,
                    lazyElements = false,
                    blockMaxLines = maxLines,
                    blockTextOverflow = textOverflow,
                )
            }
        }

        is RichElement.UnorderedListItem -> {
            RichTextBlocks.UnorderedListItem(
                modifier = modifier.then(linkClickModifier),
                selectionEnabled = selectionEnabled,
                contentPadding = contentPadding,
            ) {
                RichText(
                    content = element.content,
                    onLinkClick = onLinkClick,
                    clickable = clickable,
                    onTextClick = onTextClick,
                    onTextLongClick = onTextLongClick,
                    interactionSource = interactionSource,
                    elementContentPadding = contentPadding.copy(horizontal = 0.dp),
                    addPaddingToFirstElementTop = false,
                    addPaddingToLastElementBottom = false,
                    color = color,
                    lineHeight = lineHeight,
                    style = style,
                    selectionEnabled = selectionEnabled,
                    lazyElements = false,
                    blockMaxLines = maxLines,
                    blockTextOverflow = textOverflow,
                )
            }
        }

        is RichElement.Text -> {
            val text = remember(element.text) {
                element.text.render(
                    linkColor = style.linkColor,
                    inlineCodeBackgroundColor = style.inlineCodeBackground
                )
            }
            RichTextBlocks.Text(
                text = text,
                onTextLayout = { layoutResult = it },
                modifier = modifier.then(linkClickModifier),
                selectionEnabled = selectionEnabled,
                contentPadding = contentPadding,
                color = color,
                maxLines = maxLines,
                lineHeight = lineHeight,
                textOverflow = textOverflow,
            )
        }

        is RichElement.HorizontalRule -> {
            RichTextBlocks.HorizontalRule()
        }
    }
}
