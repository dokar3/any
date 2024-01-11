package any.ui.common.widget

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import any.base.R
import any.base.model.UiMessage
import any.base.util.Intents
import any.ui.common.richtext.Html
import any.ui.common.richtext.RichTextStyle
import com.dokar.sonner.LocalToastContentColor
import com.dokar.sonner.Toast
import com.dokar.sonner.ToastType
import com.dokar.sonner.ToastWidthPolicy
import com.dokar.sonner.Toaster
import com.dokar.sonner.ToasterDefaults
import com.dokar.sonner.listen
import com.dokar.sonner.rememberToasterState
import kotlin.time.Duration

@Composable
fun UiMessagePopup(
    message: UiMessage?,
    onMessageDismissed: (id: Long) -> Unit,
    modifier: Modifier = Modifier,
    maxTextLines: Int = 3,
    onRetry: (() -> Unit)? = null,
    showRetry: Boolean = onRetry != null,
    offset: DpOffset = DpOffset.Zero,
    applyWindowInsetsToOffset: Boolean = true,
    retryText: @Composable () -> Unit = { Text(stringResource(R.string.reload)) },
) {
    val toaster = rememberToasterState(onToastDismissed = { onMessageDismissed(it.id as Long) })

    val currentMessage by rememberUpdatedState(message)

    val context = LocalContext.current

    val density = LocalDensity.current

    val intOffset = if (applyWindowInsetsToOffset) {
        with(density) {
            IntOffset(
                x = offset.x.roundToPx(),
                y = offset.y.roundToPx() - WindowInsets.navigationBars.getBottom(this),
            )
        }
    } else {
        with(density) { IntOffset(offset.x.roundToPx(), offset.y.roundToPx()) }
    }

    LaunchedEffect(toaster) {
        toaster.listen { currentMessage?.toToast() }
    }

    Toaster(
        state = toaster,
        modifier = modifier,
        darkTheme = !MaterialTheme.colors.isLight,
        richColors = true,
        offset = intOffset,
        shape = { if (it.type == ToastType.Normal) CircleShape else ToasterDefaults.Shape },
        widthPolicy = {
            if (it.type == ToastType.Normal) {
                ToastWidthPolicy(fillMaxWidth = false)
            } else {
                ToastWidthPolicy()
            }
        },
        actionSlot = {
            if (it.type == ToastType.Error && showRetry && onRetry != null) {
                TextButton(onClick = { onRetry() }) {
                    CompositionLocalProvider(
                        LocalTextStyle provides LocalTextStyle.current.copy(
                            fontWeight = FontWeight.Bold,
                            color = LocalToastContentColor.current,
                        )
                    ) {
                        retryText()
                    }
                }
            } else {
                ToasterDefaults.actionSlot(toast = it)
            }
        },
        messageSlot = {
            Html(
                html = it.message.toString(),
                onLinkClick = { Intents.openInBrowser(context, it) },
                style = RichTextStyle.Default.copy(linkColor = MaterialTheme.colors.secondary),
                color = LocalToastContentColor.current,
                fontSize = 14.sp,
                blockMaxLines = maxTextLines,
                blockTextOverflow = TextOverflow.Ellipsis,
            )
        },
    )
}

private fun UiMessage.toToast(): Toast = when (this) {
    is UiMessage.Error -> Toast(
        id = id,
        message = message,
        type = ToastType.Error,
        duration = Duration.INFINITE
    )

    is UiMessage.Warn -> Toast(id = id, message = message, type = ToastType.Warning)
    is UiMessage.Normal -> Toast(
        id = id,
        message = message,
        type = ToastType.Normal,
        duration = ToasterDefaults.DurationShort,
    )
}
