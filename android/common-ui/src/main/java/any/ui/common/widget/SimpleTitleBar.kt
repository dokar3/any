package any.ui.common.widget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.ui.common.ViewConfiguration
import kotlin.math.max
import any.base.R as BaseR

@Composable
fun SimpleTitleBar(
    modifier: Modifier = Modifier,
    height: Dp = Dp.Unspecified,
    dividerColor: () -> Color = { Color.Unspecified },
    contentPadding: PaddingValues = PaddingValues(8.dp),
    startActionButton: @Composable (() -> Unit)? = null,
    endActionButton: @Composable (() -> Unit)? = null,
    title: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val viewConfiguration = remember(context) {
        ViewConfiguration(context = context)
    }
    val defDividerColor = MaterialTheme.colors.onBackground.copy(alpha = 0.12f)
    TitleBarLayout(
        modifier = modifier
            .let {
                if (height != Dp.Unspecified) {
                    it.height(height)
                } else {
                    it
                }
            }
            .fillMaxWidth()
            .drawBehind {
                val dividerHeight = 1.dp.toPx()
                val actualDividerColor = dividerColor().let {
                    if (it == Color.Unspecified) {
                        defDividerColor
                    } else {
                        it
                    }
                }
                drawRect(
                    color = actualDividerColor,
                    topLeft = Offset(0f, size.height - dividerHeight),
                    size = size.copy(height = dividerHeight),
                )
            }
            .padding(contentPadding),
        startButton = {
            CompositionLocalProvider(
                LocalViewConfiguration provides viewConfiguration,
            ) {
                if (startActionButton != null) {
                    startActionButton()
                } else {
                    Spacer(modifier = Modifier.minimumTouchTargetSize())
                }
            }
        },
        title = {
            Box(contentAlignment = Alignment.Center) {
                CompositionLocalProvider(
                    LocalTextStyle provides LocalTextStyle.current.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                ) {
                    title()
                }
            }
        },
        endButton = {
            CompositionLocalProvider(
                LocalViewConfiguration provides viewConfiguration,
            ) {
                if (endActionButton != null) {
                    endActionButton()
                } else {
                    Spacer(modifier = Modifier.minimumTouchTargetSize())
                }
            }
        },
    )
}

@Composable
private fun TitleBarLayout(
    startButton: @Composable () -> Unit,
    title: @Composable () -> Unit,
    endButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(
        modifier = modifier,
        content = {
            Box(contentAlignment = Alignment.Center) { startButton() }
            Box(contentAlignment = Alignment.Center) { title() }
            Box(contentAlignment = Alignment.Center) { endButton() }
        },
    ) { measurables, constraints ->
        val buttonConstraints = constraints.copy(minWidth = 0)

        val startPlaceable = measurables[0].measure(buttonConstraints)
        val endPlaceable = measurables[2].measure(buttonConstraints)

        val titleMaxWidth = constraints.maxWidth - startPlaceable.width - endPlaceable.width
        val titlePlaceable = measurables[1].measure(
            constraints.copy(minWidth = 0, maxWidth = titleMaxWidth)
        )

        val maxHeight = maxOf(startPlaceable.height, titlePlaceable.height, endPlaceable.height)

        layout(constraints.maxWidth, maxHeight) {
            startPlaceable.placeRelative(
                x = 0,
                y = (maxHeight - startPlaceable.height) / 2
            )

            titlePlaceable.place(
                x = max(startPlaceable.width, (constraints.maxWidth - titlePlaceable.width) / 2),
                y = (maxHeight - titlePlaceable.height) / 2
            )

            endPlaceable.placeRelative(
                x = constraints.maxWidth - endPlaceable.width,
                y = (maxHeight - endPlaceable.height) / 2
            )
        }
    }
}

@Composable
fun AnimatedBackButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInHorizontally { it / 2 },
        exit = fadeOut() + slideOutHorizontally { it / 2 }
    ) {
        val label = stringResource(BaseR.string.back)
        TitleActionButton(
            label = label,
            onClick = onClick,
            modifier = modifier,
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = label,
            )
        }
    }
}


@Composable
fun TitleActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    icon: @Composable (label: String) -> Unit,
) {
    TooltipBox(
        text = label,
        onLongClick = onLongClick,
    ) {
        Box(
            modifier = modifier
                .size(42.dp)
                .padding(4.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false),
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            icon(label)
        }
    }
}
