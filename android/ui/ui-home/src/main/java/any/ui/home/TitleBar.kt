package any.ui.home

import any.base.R as BaseR
import any.ui.common.R as CommonUiR
import androidx.compose.animation.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.ui.common.theme.topBarBackground
import any.ui.common.widget.SimpleTitleBar
import any.ui.common.widget.StatusBarSpacer
import any.ui.common.widget.TitleActionButton

internal enum class IconTintTheme {
    Light,
    Dark,
    Auto,
}

@Composable
internal fun TitleBar(
    height: Dp,
    modifier: Modifier = Modifier,
    subTitle: @Composable (() -> Unit)? = null,
    startActionButton: @Composable (() -> Unit)? = null,
    endActionButton: @Composable (() -> Unit)? = null,
    iconTintTheme: IconTintTheme = IconTintTheme.Auto,
    backgroundColor: Color = MaterialTheme.colors.topBarBackground,
    dividerAlpha: Float = 0f,
    titleAlpha: Float = 1f,
    titleFontSize: TextUnit = if (subTitle != null) 18.sp else 20.sp,
    titleFontWeight: FontWeight = FontWeight.Bold,
    contentPadding: PaddingValues = PaddingValues(8.dp),
    title: @Composable () -> Unit,
) {
    TitleBar(
        height = height,
        backgroundColor = { backgroundColor },
        modifier = modifier,
        title = title,
        subTitle = subTitle,
        startActionButton = startActionButton,
        endActionButton = endActionButton,
        iconTintTheme = iconTintTheme,
        dividerAlpha = { dividerAlpha },
        titleAlpha = { titleAlpha },
        titleFontSize = titleFontSize,
        titleFontWeight = titleFontWeight,
        contentPadding = contentPadding,
    )
}

@Composable
internal fun TitleBar(
    height: Dp,
    backgroundColor: () -> Color,
    modifier: Modifier = Modifier,
    subTitle: @Composable (() -> Unit)? = null,
    startActionButton: @Composable (() -> Unit)? = null,
    endActionButton: @Composable (() -> Unit)? = null,
    iconTintTheme: IconTintTheme = IconTintTheme.Auto,
    dividerAlpha: () -> Float = { 0f },
    titleAlpha: () -> Float = { 1f },
    titleFontSize: TextUnit = if (subTitle != null) 18.sp else 20.sp,
    titleFontWeight: FontWeight = FontWeight.Bold,
    contentPadding: PaddingValues = PaddingValues(8.dp),
    title: @Composable () -> Unit,
) {
    val onBackground = MaterialTheme.colors.onBackground

    val targetIconColor = remember(iconTintTheme, onBackground) {
        when (iconTintTheme) {
            IconTintTheme.Light -> Color.White
            IconTintTheme.Dark -> Color(0xff212121)
            IconTintTheme.Auto -> onBackground
        }
    }

    val iconColor = remember { Animatable(targetIconColor) }

    LaunchedEffect(targetIconColor) {
        if (targetIconColor == iconColor.value) {
            return@LaunchedEffect
        }
        iconColor.animateTo(targetValue = targetIconColor)
    }

    val onSurface = MaterialTheme.colors.onSurface

    Column(modifier = Modifier) {
        StatusBarSpacer()

        SimpleTitleBar(
            modifier = modifier
                .drawBehind { drawRect(color = backgroundColor()) }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            height = height,
            dividerColor = { onSurface.copy(alpha = 0.12f * dividerAlpha()) },
            contentPadding = contentPadding,
            startActionButton = {
                if (startActionButton != null) {
                    CompositionLocalProvider(
                        LocalContentColor provides iconColor.value
                    ) {
                        startActionButton()
                    }
                }
            },
            endActionButton = {
                if (endActionButton != null) {
                    CompositionLocalProvider(
                        LocalContentColor provides iconColor.value
                    ) {
                        endActionButton()
                    }
                }
            },
        ) {
            Column(
                modifier = Modifier
                    .pointerInput(Unit) {
                        forEachGesture {
                            awaitPointerEventScope {
                                awaitFirstDown(requireUnconsumed = false)
                                val touchable = titleAlpha() != 0f
                                if (!touchable) {
                                    awaitPointerEvent().changes.forEach { it.consume() }
                                }
                            }
                        }
                    }
                    .graphicsLayer { alpha = titleAlpha() },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CompositionLocalProvider(
                    LocalTextStyle provides LocalTextStyle.current.copy(
                        fontSize = titleFontSize,
                        fontWeight = titleFontWeight,
                    ),
                    LocalContentColor provides iconColor.value,
                ) {
                    title()
                }

                if (subTitle != null) {
                    CompositionLocalProvider(
                        LocalTextStyle provides LocalTextStyle.current.copy(
                            fontSize = 13.sp,
                            color = MaterialTheme.colors.onSecondary
                        )
                    ) {
                        subTitle()
                    }
                }
            }
        }
    }
}

@Composable
internal fun SettingsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    TitleActionButton(
        label = stringResource(BaseR.string.settings),
        onClick = onClick,
        modifier = modifier,
        onLongClick = onLongClick,
    ) {
        Icon(
            painter = painterResource(CommonUiR.drawable.ic_settings),
            contentDescription = null,
        )
    }
}
