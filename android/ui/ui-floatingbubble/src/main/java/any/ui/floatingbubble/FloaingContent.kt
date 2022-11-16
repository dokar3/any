package any.ui.floatingbubble

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import any.base.prefs.darkModeEnabledFlow
import any.base.prefs.darkModePrimaryColor
import any.base.prefs.preferencesStore
import any.base.prefs.primaryColor
import any.ui.common.theme.AnyTheme

@Composable
internal fun FloatingContent(
    onRequestHide: () -> Unit,
    modifier: Modifier = Modifier,
    fabSize: Dp = 56.dp,
    arrowColor: @Composable () -> Color,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()

    val preferencesStore = LocalContext.current.preferencesStore()

    val isDark by preferencesStore.darkModeEnabledFlow(LocalContext.current, scope)
        .collectAsState()
    val primaryColor by preferencesStore.primaryColor
        .asStateFlow(scope)
        .collectAsState()
    val darkModePrimaryColor by preferencesStore.darkModePrimaryColor
        .asStateFlow(scope)
        .collectAsState()

    AnyTheme(
        darkTheme = isDark,
        primaryColor = primaryColor,
        darkModePrimaryColor = darkModePrimaryColor,
    ) {
        val loggerViewHMargin = 20.dp
        val loggerViewVMargin = 28.dp

        val topSpacerHeight = fabSize + 16.dp

        Surface(
            modifier = modifier
                .pointerInput(null) {
                    forEachGesture {
                        awaitPointerEventScope {
                            awaitFirstDown(requireUnconsumed = true)
                            do {
                                val event = awaitPointerEvent()
                            } while (!event.changes.fastAny { it.changedToUp() })
                            onRequestHide()
                        }
                    }
                }
                .fillMaxSize()
                .padding(
                    start = loggerViewHMargin,
                    top = loggerViewVMargin + topSpacerHeight,
                    end = loggerViewHMargin,
                    bottom = loggerViewVMargin + 64.dp
                ),
            color = Color.Transparent,
            contentColor = contentColorFor(MaterialTheme.colors.surface),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                val arrowColorValue = arrowColor()
                Spacer(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(12.dp, 8.dp)
                        .drawWithCache {
                            val triangle = Path().apply {
                                moveTo(size.width / 2, 0f)
                                lineTo(size.width, size.height)
                                lineTo(0f, size.height)
                                close()
                            }
                            onDrawBehind {
                                drawPath(
                                    path = triangle,
                                    color = arrowColorValue,
                                )
                            }
                        }
                )

                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        )
                        .background(MaterialTheme.colors.surface),
                ) {
                    content()
                }
            }
        }
    }
}
