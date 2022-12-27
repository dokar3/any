package any.ui.service

import android.webkit.WebSettings
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.ContentAlpha
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.compose.ImmutableHolder
import any.data.entity.ServiceConfigOption
import any.ui.browser.Browser
import any.ui.common.theme.secondaryText
import any.ui.common.widget.FlatSwitch

@Composable
internal fun BoolFieldItem(
    name: String,
    description: String?,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FieldName(
                name = name,
                modifier = Modifier.weight(1f)
            )

            var checked by remember(value) { mutableStateOf(value) }

            FlatSwitch(
                checked = checked,
                onCheckedChange = {
                    checked = it
                    onValueChange(it)
                },
                enabled = enabled,
            )
        }

        if (!description.isNullOrEmpty()) {
            FieldDescription(description = description)
        }
    }
}

@Composable
internal fun OptionFieldItem(
    name: String,
    description: String?,
    options: ImmutableHolder<List<ServiceConfigOption>>,
    value: String?,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column {
        FieldName(name = name)

        if (!description.isNullOrEmpty()) {
            FieldDescription(description = description)

            Spacer(modifier = Modifier.height(8.dp))
        }

        var selectedOption by remember(value) {
            val defaultValue = value?.let {
                if (it.isEmpty() && options.value.isNotEmpty()) {
                    val first = options.value.first()
                    onValueChange(first.value)
                    first.name
                } else {
                    options.value.firstOrNull { opt -> opt.value == value }?.name
                }
            }
            mutableStateOf(defaultValue ?: "")
        }

        var showOptionsPopup by remember { mutableStateOf(false) }

        BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
            val interactionSource = remember { MutableInteractionSource() }
            val indicatorColor by TextFieldDefaults.outlinedTextFieldColors()
                .indicatorColor(
                    enabled = true,
                    isError = false,
                    interactionSource = interactionSource
                )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (enabled) 1f else ContentAlpha.disabled)
                    .clip(TextFieldDefaults.OutlinedTextFieldShape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = rememberRipple(),
                        onClick = {
                            showOptionsPopup = true
                        },
                        enabled = enabled,
                    )
                    .border(
                        shape = TextFieldDefaults.OutlinedTextFieldShape,
                        color = indicatorColor,
                        width = 1.dp,
                    )
                    .padding(
                        start = 16.dp,
                        top = 12.dp,
                        end = 8.dp,
                        bottom = 12.dp,
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(selectedOption)

                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                )
            }

            if (showOptionsPopup && options.value.isNotEmpty()) {
                DropdownMenu(
                    expanded = true,
                    modifier = Modifier.width(maxWidth),
                    onDismissRequest = { showOptionsPopup = false },
                ) {
                    for (option in options.value) {
                        DropdownMenuItem(
                            onClick = {
                                selectedOption = option.name
                                onValueChange(option.value)
                                showOptionsPopup = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(option.name)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun TextFieldItem(
    name: String,
    description: String?,
    value: String?,
    digitsOnly: Boolean,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    error: String?,
    modifier: Modifier = Modifier,
) {
    Column {
        FieldName(name = name)

        var currValue by remember(value) { mutableStateOf(value ?: "") }

        OutlinedTextField(
            value = currValue,
            onValueChange = { text ->
                val filtered = if (digitsOnly) {
                    text.filter { it.isDigit() || it == '.' || it == '-' }
                } else {
                    text
                }
                currValue = filtered
                onValueChange(filtered)
            },
            modifier = modifier.fillMaxWidth(),
            enabled = enabled,
            label = if (!error.isNullOrEmpty()) {
                { Text(error) }
            } else {
                null
            },
            placeholder = if (!description.isNullOrEmpty()) {
                {
                    Text(description)
                }
            } else {
                null
            },
            isError = error != null,
            singleLine = true,
            maxLines = 1,
        )
    }
}

@Composable
internal fun CookiesFieldItem(
    name: String,
    description: String?,
    requestUrl: String,
    targetUrl: String,
    userAgent: String?,
    onValueChange: (ua: String, cookies: String) -> Unit,
    enabled: Boolean,
    error: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        val context = LocalContext.current

        var showBrowser by remember { mutableStateOf(false) }

        Button(
            onClick = { showBrowser = true },
            enabled = enabled && requestUrl.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(name)
        }

        if (!description.isNullOrEmpty()) {
            FieldDescription(description = description)
        }

        if (error != null) {
            Text(
                text = error,
                fontSize = 14.sp,
                color = MaterialTheme.colors.error,
            )
        }

        if (showBrowser && requestUrl.isNotEmpty()) {
            Browser(
                url = requestUrl,
                title = name,
                userAgent = userAgent,
                cookiesTargetUrl = targetUrl,
                onGetCookies = {
                    showBrowser = false
                    if (it != null) {
                        val ua = userAgent ?: WebSettings.getDefaultUserAgent(context)
                        onValueChange(ua, it)
                    }
                },
            )
        }
    }
}

@Composable
private fun FieldName(
    name: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = name,
        modifier = modifier.padding(vertical = 4.dp),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun FieldDescription(
    description: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = description,
        modifier = modifier,
        color = MaterialTheme.colors.secondaryText,
        fontSize = 14.sp,
    )
}