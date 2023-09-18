package any.ui.common.dialog

import androidx.annotation.StyleRes
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.flow.filter
import any.ui.common.R as CommonUiR

@Composable
fun SimpleDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: @Composable (() -> Unit)? = null,
    neutralText: @Composable (() -> Unit)? = null,
    confirmText: @Composable (() -> Unit)? = null,
    cancelText: @Composable (() -> Unit)? = null,
    onNeutralClick: (() -> Unit)? = null,
    onConfirmClick: (() -> Unit)? = null,
    onCancelClick: (() -> Unit)? = null,
    confirmEnabled: Boolean = true,
    cancelEnabled: Boolean = true,
    neutralEnabled: Boolean = true,
    dismissOnCancel: Boolean = true,
    dismissOnConfirm: Boolean = true,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    @StyleRes themeResId: Int = CommonUiR.style.ComposeDialogTheme,
    text: @Composable (() -> Unit)? = null,
) {
    BasicDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        title = title,
        content = text,
        neutralText = neutralText,
        confirmText = confirmText,
        cancelText = cancelText,
        onNeutralClick = onNeutralClick,
        onConfirmClick = onConfirmClick,
        onCancelClick = onCancelClick,
        confirmEnabled = confirmEnabled,
        cancelEnabled = cancelEnabled,
        neutralEnabled = neutralEnabled,
        dismissOnCancel = dismissOnCancel,
        dismissOnConfirm = dismissOnConfirm,
        dismissOnBackPress = dismissOnBackPress,
        dismissOnClickOutside = dismissOnClickOutside,
        contentPadding = contentPadding,
        themeResId = themeResId,
    )
}

@Composable
fun EditDialog(
    onDismissRequest: () -> Unit,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    title: @Composable (() -> Unit)? = null,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    acceptEmpty: Boolean = true,
    isError: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    confirmText: @Composable (() -> Unit)? = { Text(stringResource(android.R.string.ok)) },
    cancelText: @Composable (() -> Unit)? = { Text(stringResource(android.R.string.cancel)) },
    neutralText: @Composable (() -> Unit)? = null,
    onConfirmClick: (() -> Unit)? = null,
    confirmEnabled: Boolean = true,
    cancelEnabled: Boolean = true,
    dismissOnConfirm: Boolean = true,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    @StyleRes themeResId: Int = CommonUiR.style.ComposeDialogTheme,
) {
    BasicDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        title = title,
        content = {
            Column {
                val visualTransformation = when (keyboardType) {
                    KeyboardType.Password,
                    KeyboardType.NumberPassword -> {
                        PasswordVisualTransformation()
                    }

                    else -> VisualTransformation.None
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = label,
                    isError = isError,
                    placeholder = placeholder,
                    visualTransformation = visualTransformation,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = keyboardType
                    ),
                )
            }
        },
        neutralText = neutralText,
        confirmText = confirmText,
        cancelText = cancelText,
        onConfirmClick = {
            if (acceptEmpty || value.isNotEmpty()) {
                onConfirmClick?.invoke()
                if (dismissOnConfirm) {
                    onDismissRequest()
                }
            }
        },
        confirmEnabled = confirmEnabled,
        cancelEnabled = cancelEnabled,
        dismissOnConfirm = false,
        dismissOnClickOutside = dismissOnClickOutside,
        dismissOnBackPress = dismissOnBackPress,
        contentPadding = contentPadding,
        themeResId = themeResId,
    )
}

@Composable
fun BasicDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: @Composable (() -> Unit)? = null,
    neutralText: @Composable (() -> Unit)? = null,
    confirmText: @Composable (() -> Unit)? = null,
    cancelText: @Composable (() -> Unit)? = null,
    onNeutralClick: (() -> Unit)? = null,
    onConfirmClick: (() -> Unit)? = null,
    onCancelClick: (() -> Unit)? = null,
    neutralEnabled: Boolean = true,
    confirmEnabled: Boolean = true,
    cancelEnabled: Boolean = true,
    dismissOnNeutral: Boolean = true,
    dismissOnCancel: Boolean = true,
    dismissOnConfirm: Boolean = true,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    @StyleRes themeResId: Int = CommonUiR.style.ComposeDialogTheme,
    content: @Composable (() -> Unit)? = null,
) {
    val visibleState = remember {
        MutableTransitionState(false).also { it.targetState = true }
    }

    val currentOnDismissRequest = rememberUpdatedState(onDismissRequest)

    LaunchedEffect(visibleState) {
        snapshotFlow { visibleState.currentState to visibleState.targetState }
            .filter { !it.first && !it.second }
            .collect { currentOnDismissRequest.value.invoke() }
    }

    DisposableEffect(visibleState) {
        onDispose { visibleState.targetState = false }
    }

    StyleableDialog(
        onDismissRequest = { visibleState.targetState = false },
        content = {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = MaterialTheme.shapes.medium,
                    )
                    .background(
                        color = MaterialTheme.colors.surface,
                        shape = MaterialTheme.shapes.medium,
                    )
            ) {
                // Title
                if (title != null) {
                    Box(
                        modifier = Modifier.padding(
                            start = 20.dp,
                            top = 16.dp,
                            end = 20.dp,
                            bottom = 8.dp,
                        )
                    ) {
                        CompositionLocalProvider(
                            LocalTextStyle provides LocalTextStyle.current.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        ) {
                            title()
                        }
                    }
                }

                // Content
                if (content != null) {
                    Box(
                        modifier = Modifier
                            .weight(weight = 1f, fill = false)
                            .padding(contentPadding)
                    ) {
                        content()
                    }
                }

                // Buttons
                if (neutralText != null || cancelText != null || confirmText != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        if (neutralText != null) {
                            Box(modifier = Modifier.weight(1f)) {
                                DialogButton(
                                    enabled = neutralEnabled,
                                    onClick = {
                                        onNeutralClick?.invoke()
                                        if (dismissOnNeutral) {
                                            onDismissRequest()
                                        }
                                    },
                                    text = neutralText,
                                )
                            }
                        }

                        Row {
                            if (cancelText != null) {
                                DialogButton(
                                    enabled = cancelEnabled,
                                    onClick = {
                                        onCancelClick?.invoke()
                                        if (dismissOnCancel) {
                                            onDismissRequest()
                                        }
                                    },
                                    text = cancelText,
                                )
                            }

                            if (confirmText != null) {
                                DialogButton(
                                    enabled = confirmEnabled,
                                    onClick = {
                                        onConfirmClick?.invoke()
                                        if (dismissOnConfirm) {
                                            onDismissRequest()
                                        }
                                    },
                                    text = confirmText,
                                )
                            }
                        }
                    }
                }
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside,
        ),
        themeResId = themeResId,
    )
}

@Composable
private fun DialogButton(
    enabled: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    text: @Composable () -> Unit,
) {
    Button(
        modifier = modifier,
        enabled = enabled,
        elevation = null,
        colors = ButtonDefaults.textButtonColors(),
        onClick = {
            onClick?.invoke()
        },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
    ) {
        text()
    }
}
