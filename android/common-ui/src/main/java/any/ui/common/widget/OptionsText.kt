package any.ui.common.widget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import any.base.compose.StableHolder
import any.base.util.compose.performLongPress
import any.base.util.compose.performTextHandleMove
import kotlinx.coroutines.flow.filter

@Composable
fun OptionsText(
    text: String,
    options: List<TextOption>,
    modifier: Modifier = Modifier,
    isSelectable: Boolean = true,
    onOptionSelected: ((TextOption) -> Unit)? = null,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = TextDecoration.None,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
) {
    var showOptions by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { showOptions = true },
            ),
    ) {
        if (isSelectable) {
            SelectionContainer {
                Text(
                    text = text,
                    modifier = modifier,
                    color = color,
                    fontSize = fontSize,
                    fontStyle = fontStyle,
                    fontWeight = fontWeight,
                    fontFamily = fontFamily,
                    letterSpacing = letterSpacing,
                    textDecoration = if (showOptions) {
                        TextDecoration.Underline
                    } else {
                        textDecoration
                    },
                    textAlign = textAlign,
                    lineHeight = lineHeight,
                    overflow = overflow,
                    softWrap = softWrap,
                    maxLines = maxLines,
                    onTextLayout = onTextLayout,
                    style = style,
                )
            }
        } else {
            Text(
                text = text,
                modifier = modifier,
                color = color,
                fontSize = fontSize,
                fontStyle = fontStyle,
                fontWeight = fontWeight,
                fontFamily = fontFamily,
                letterSpacing = letterSpacing,
                textDecoration = if (showOptions) {
                    TextDecoration.Underline
                } else {
                    textDecoration
                },
                textAlign = textAlign,
                lineHeight = lineHeight,
                overflow = overflow,
                softWrap = softWrap,
                maxLines = maxLines,
                onTextLayout = onTextLayout,
                style = style,
            )
        }

        if (showOptions) {
            DisableSelection {
                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    val clipboardManager = LocalClipboardManager.current
                    TextOptionsPopup(
                        onDismissRequest = { showOptions = false },
                        options = options,
                        onOptionSelected = {
                            showOptions = false
                            onOptionSelected?.invoke(it)
                            when (it) {
                                is TextOption.Copy -> {
                                    clipboardManager.setText(AnnotatedString(text))
                                }
                                else -> {}
                            }
                        },
                        alignment = Alignment.TopCenter,
                    )
                }
            }
        }
    }
}

@Composable
private fun TextOptionsPopup(
    onDismissRequest: () -> Unit,
    options: List<TextOption>,
    onOptionSelected: (TextOption) -> Unit,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.TopStart,
    backgroundColor: Color = MaterialTheme.colors.surface,
    shape: Shape = MaterialTheme.shapes.small,
) {
    val visibleState = remember {
        MutableTransitionState(initialState = false).also {
            it.targetState = true
        }
    }

    LaunchedEffect(visibleState) {
        snapshotFlow { visibleState.currentState to visibleState.targetState }
            .filter { !it.first && !it.second }
            .collect { onDismissRequest() }
    }

    Popup(
        onDismissRequest = { visibleState.targetState = false },
        alignment = alignment,
        properties = PopupProperties(visibleState.targetState),
    ) {
        AnimatedVisibility(
            visibleState = visibleState,
            enter = slideInVertically(
                animationSpec = tween(durationMillis = 255)
            ) {
                it / 2
            } + fadeIn(animationSpec = tween(durationMillis = 255)),
            exit = slideOutVertically(
                animationSpec = tween(durationMillis = 200)
            ) {
                it / 2
            } + fadeOut(animationSpec = tween(durationMillis = 200)),
        ) {
            val arrowSize = DpSize(12.dp, 8.dp)
            Surface(
                elevation = 6.dp,
                shape = shape,
                modifier = Modifier
                    .drawArrowIndicator(
                        backgroundColor = backgroundColor,
                        arrowSize = arrowSize,
                    )
                    .padding(top = arrowSize.height)
            ) {
                OptionsMenu(
                    options = StableHolder(options),
                    onOptionSelected = onOptionSelected,
                    modifier = modifier
                        .clip(shape)
                        .background(backgroundColor),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OptionsMenu(
    options: StableHolder<List<TextOption>>,
    onOptionSelected: (TextOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    Row(
        modifier = modifier,
    ) {
        for (option in options.value) {
            Text(
                text = option.title,
                modifier = Modifier
                    .widthIn(min = 56.dp)
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(),
                        onClick = {
                            hapticFeedback.performTextHandleMove()
                            onOptionSelected(option)
                        },
                        onLongClick = {
                            hapticFeedback.performLongPress()
                            onOptionSelected(option)
                        },
                    )
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun Modifier.drawArrowIndicator(
    backgroundColor: Color,
    arrowSize: DpSize,
): Modifier {
    return drawWithCache {
        val arrowPxSize = arrowSize.toSize()
        val path = Path()
        path.moveTo(size.width / 2f, 0f)
        path.lineTo(size.width / 2 + arrowPxSize.width / 2, arrowPxSize.height)
        path.lineTo(size.width / 2 - arrowPxSize.width / 2, arrowPxSize.height)
        path.close()
        val strokeSize = 1.dp.toPx()
        val strokeStyle = Stroke(
            width = strokeSize,
            cap = StrokeCap.Round,
            join = StrokeJoin.Bevel,
        )
        val strokeBrush = Brush.verticalGradient(
            0f to Color.Black,
            1f to Color.Transparent,
            endY = arrowPxSize.height,
        )
        onDrawWithContent {
            drawContent()
            drawPath(
                path = path,
                color = backgroundColor,
            )
            drawPath(
                path = path,
                brush = strokeBrush,
                alpha = 0.1f,
                style = strokeStyle,
            )
        }
    }
}

@Stable
sealed class TextOption(
    val title: String,
) {
    class Copy(title: String) : TextOption(title)

    class Search(title: String) : TextOption(title)

    class Custom(val id: Int, title: String) : TextOption(title) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            if (!super.equals(other)) return false

            other as Custom

            if (id != other.id) return false

            return true
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + id
            return result
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TextOption

        if (title != other.title) return false

        return true
    }

    override fun hashCode(): Int {
        return title.hashCode()
    }
}
