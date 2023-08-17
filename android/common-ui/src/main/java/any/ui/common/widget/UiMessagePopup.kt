package any.ui.common.widget

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.R
import any.base.model.UiMessage
import any.base.util.Intents
import any.ui.common.richtext.Html
import any.ui.common.richtext.RichTextStyle
import any.ui.common.theme.warn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun UiMessagePopup(
    message: UiMessage?,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
    maxTextLines: Int = 3,
    onRetry: (() -> Unit)? = null,
    showRetry: Boolean = onRetry != null,
    swipeable: Boolean = message is UiMessage.Warn || message is UiMessage.Error,
    offset: DpOffset = DpOffset.Zero,
    applyWindowInsetsToOffset: Boolean = true,
    contentMargin: PaddingValues = PaddingValues(16.dp),
    retryText: @Composable () -> Unit = { Text(stringResource(R.string.reload)) },
) {
    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    val density = LocalDensity.current

    var msg: UiMessage by remember { mutableStateOf(UiMessage.Normal("")) }

    var showMessage by remember { mutableStateOf(false) }

    DisposableEffect(message) {
        var isMessageCleared = false
        if (message != null) {
            msg = message
            showMessage = true
            if (message !is UiMessage.Error) {
                scope.launch {
                    delay(MessagePopupDefaults.POPUP_DURATION)
                    onClearMessage()
                    isMessageCleared = true
                }
            }
        } else {
            showMessage = false
        }
        onDispose {
            if (message != null && !isMessageCleared) {
                onClearMessage()
                isMessageCleared = true
            }
        }
    }

    val backgroundColor = when (msg) {
        is UiMessage.Error -> MaterialTheme.colors.error
        is UiMessage.Normal -> MaterialTheme.colors.primary
        is UiMessage.Warn -> MaterialTheme.colors.warn
    }

    val offY = if (applyWindowInsetsToOffset) {
        val bottomInset = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
        offset.y - bottomInset
    } else {
        offset.y
    }

    val isError = msg is UiMessage.Error

    MessagePopup(
        visible = showMessage,
        modifier = modifier,
        backgroundColor = backgroundColor,
        offset = DpOffset(offset.x, offY),
        contentMargin = contentMargin,
        swipeable = swipeable,
        onDismissed = { onClearMessage() },
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .run { if (isError) fillMaxWidth() else this },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Html(
                html = msg.message,
                onLinkClick = { Intents.openInBrowser(context, it) },
                modifier = Modifier
                    .run { if (isError) weight(1f) else this },
                style = RichTextStyle.Default.copy(linkColor = MaterialTheme.colors.secondary),
                color = contentColorFor(backgroundColor),
                fontSize = 14.sp,
                blockMaxLines = maxTextLines,
                blockTextOverflow = TextOverflow.Ellipsis,
            )

            if (isError && showRetry) {
                TextButton(onClick = { onRetry?.invoke() }) {
                    CompositionLocalProvider(
                        LocalTextStyle provides LocalTextStyle.current.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.onError,
                        )
                    ) {
                        retryText()
                    }
                }
            }
        }
    }
}