package any.ui.common.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import any.ui.common.R

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun rememberSearchBarState(): SearchBarState {
    val keyboardController = LocalSoftwareKeyboardController.current
    return remember(keyboardController) { SearchBarState(keyboardController) }
}

@OptIn(ExperimentalComposeUiApi::class)
class SearchBarState(
    private val keyboardController: SoftwareKeyboardController?,
) {
    internal val inputFocusRequester = FocusRequester()

    internal var enabled = false

    fun showKeyboard() {
        if (!enabled) {
            return
        }
        inputFocusRequester.requestFocus()
        keyboardController?.show()
    }

    fun hideKeyboard() {
        if (!enabled) {
            return
        }
        inputFocusRequester.freeFocus()
        keyboardController?.hide()
    }
}

@Composable
fun SearchBar(
    text: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    state: SearchBarState = rememberSearchBarState(),
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = { SearchIcon() },
    placeholder: @Composable (() -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }

    SideEffect {
        state.enabled = enabled
    }

    DisposableEffect(state) {
        onDispose {
            state.enabled = false
        }
    }

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.06f),
                shape = CircleShape
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (enabled) {
                        state.showKeyboard()
                    }
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            icon()
        }

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = text,
                onValueChange = {
                    if (it.text.contains('\n')) {
                        onValueChange(it.copy(text = it.text.replace(Regex("\\n"), "")))
                        onSearch?.invoke()
                    } else {
                        onValueChange(it)
                    }
                },
                modifier = Modifier
                    .focusRequester(state.inputFocusRequester)
                    .fillMaxWidth(),
                enabled = enabled,
                interactionSource = interactionSource,
                textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colors.primary),
                singleLine = true,
                maxLines = 1,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search,
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        onSearch?.invoke()
                    },
                ),
                cursorBrush = SolidColor(MaterialTheme.colors.primary),
            )

            if (text.text.isEmpty() && placeholder != null) {
                val color = MaterialTheme.colors.onBackground.copy(alpha = 0.5f)
                CompositionLocalProvider(
                    LocalTextStyle provides LocalTextStyle.current.copy(color = color)
                ) {
                    Box(modifier = Modifier.padding(start = 4.dp)) {
                        placeholder()
                    }
                }
            }
        }

        if (text.text.isNotEmpty()) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Clear text",
                modifier = Modifier
                    .size(24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(bounded = false),
                        onClick = { onValueChange(TextFieldValue()) }
                    ),
                tint = MaterialTheme.colors.onBackground.copy(alpha = 0.4f),
            )
        }
    }
}

@Composable
private fun SearchIcon() {
    Icon(
        painter = painterResource(R.drawable.ic_search),
        contentDescription = "SearchIcon",
        modifier = Modifier
            .size(24.dp)
            .padding(2.dp),
        tint = MaterialTheme.colors.onBackground.copy(alpha = 0.4f),
    )
}
