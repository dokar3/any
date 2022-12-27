package any.ui.service

import android.webkit.WebSettings
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import any.data.entity.ServiceConfig
import any.data.entity.ServiceConfigType
import any.data.entity.value
import any.ui.browser.Browser
import any.ui.common.theme.secondaryText
import any.ui.common.widget.FlatSwitch

@Composable
internal fun BoolFieldItem(
    field: ServiceConfig,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val description = field.description

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FieldName(
            name = field.name,
            modifier = Modifier.weight(1f)
        )

        var checked by remember(field.value) {
            val value = field.value
            val boolValue = if (value is Boolean) {
                value
            } else {
                false
            }
            mutableStateOf(boolValue)
        }
        FlatSwitch(
            checked = checked,
            onCheckedChange = {
                checked = it
                onValueChange(it.toString())
            },
            enabled = enabled,
        )
    }

    if (!description.isNullOrEmpty()) {
        Text(
            text = description,
            color = MaterialTheme.colors.secondaryText,
            fontSize = 14.sp,
        )
    }
}

@Composable
internal fun OptionFieldItem(
    field: ServiceConfig,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    require(field is ServiceConfig.Option) {
        "Unsupported config type: ${field::class.java}"
    }

    FieldName(name = field.name)

    val options = field.options

    var value by remember(field.value) {
        val defaultValue = field.value?.let {
            if (it.isEmpty() && options.isNotEmpty()) {
                val first = options.first()
                onValueChange(first.value)
                first.name
            } else {
                val value = field.value
                if (value is String) {
                    options.firstOrNull { opt -> opt.value == value }?.name
                } else {
                    options.firstOrNull()?.name
                }
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
            Text(value)

            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
            )
        }

        if (showOptionsPopup && options.isNotEmpty()) {
            DropdownMenu(
                expanded = true,
                modifier = Modifier.width(maxWidth),
                onDismissRequest = { showOptionsPopup = false },
            ) {
                for (option in options) {
                    DropdownMenuItem(
                        onClick = {
                            value = option.name
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

@Composable
internal fun TextFieldItem(
    field: ServiceConfig,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    error: String?,
    modifier: Modifier = Modifier,
) {
    FieldName(name = field.name)

    val digitsOnly = field.type == ServiceConfigType.Number

    var value by remember(field.value) {
        val text = when (val value = field.value) {
            is String -> value
            else -> ""
        }
        mutableStateOf(text)
    }

    OutlinedTextField(
        value = value,
        onValueChange = { text ->
            val filtered = if (digitsOnly) {
                text.filter { it.isDigit() || it == '.' || it == '-' }
            } else {
                text
            }
            value = filtered
            onValueChange(filtered)
        },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        label = if (!error.isNullOrEmpty()) {
            { Text(error) }
        } else {
            null
        },
        placeholder = if (!field.description.isNullOrEmpty()) {
            {
                Text(field.description!!)
            }
        } else {
            null
        },
        isError = error != null,
        singleLine = true,
        maxLines = 1,
    )
}

@Composable
internal fun CookiesFieldItem(
    field: ServiceConfig,
    onValueChange: (ua: String, cookies: String) -> Unit,
    enabled: Boolean,
    error: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    var showBrowser by remember { mutableStateOf(false) }

    val requestUrl: String
    val targetUrl: String
    val userAgent: String?
    when (field) {
        is ServiceConfig.Cookies -> {
            requestUrl = field.requestUrl
            targetUrl = field.targetUrl
            userAgent = field.userAgent
        }

        is ServiceConfig.CookiesUa -> {
            requestUrl = field.requestUrl
            targetUrl = field.targetUrl
            userAgent = field.userAgent
        }

        else -> {
            throw IllegalArgumentException("Unsupported config type: ${field::class.java}")
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Button(
            onClick = { showBrowser = true },
            enabled = enabled && requestUrl.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(field.name)
        }

        val description = field.description
        if (!description.isNullOrEmpty()) {
            Text(
                text = description,
                fontSize = 14.sp,
                color = MaterialTheme.colors.secondaryText,
            )
        }

        if (error != null) {
            Text(
                text = error,
                fontSize = 14.sp,
                color = MaterialTheme.colors.error,
            )
        }
    }

    if (showBrowser && requestUrl.isNotEmpty()) {
        Browser(
            url = requestUrl,
            title = field.name,
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

@Composable
internal fun FieldName(
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
