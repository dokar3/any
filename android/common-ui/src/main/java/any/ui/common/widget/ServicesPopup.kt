package any.ui.common.widget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import any.base.compose.ImmutableHolder
import any.base.util.compose.performLongPress
import any.data.entity.ServiceResource
import any.domain.entity.UiServiceManifest
import any.ui.common.modifier.verticalScrollBar
import any.ui.common.theme.divider
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import any.base.R as BaseR
import any.ui.common.R as CommonUiR

@Composable
fun ServicesPopup(
    services: ImmutableHolder<List<UiServiceManifest>>,
    selected: UiServiceManifest?,
    onDismissRequest: () -> Unit,
    onManagementClick: () -> Unit,
    onSelectedItem: (UiServiceManifest) -> Unit,
    modifier: Modifier = Modifier,
    onItemLongClick: ((UiServiceManifest) -> Unit)? = null,
) {
    val dismissRequester = rememberAnimatedPopupDismissRequester()

    val currentOnDismissRequest = rememberUpdatedState(onDismissRequest)

    LaunchedEffect(dismissRequester.visibleState) {
        dismissRequester.visibleState.targetState = true
        snapshotFlow {
            dismissRequester.visibleState.currentState to
                    dismissRequester.visibleState.targetState
        }
            .distinctUntilChanged()
            .filter { !it.first && !it.second }
            .collect { currentOnDismissRequest.value() }
    }

    Dialog(
        onDismissRequest = { dismissRequester.dismiss() },
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(vertical = 32.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { dismissRequester.dismiss() },
                )
        ) {
            AnimatedDialogContent(dismissRequester = dismissRequester) {
                val hapticFeedback = LocalHapticFeedback.current

                val listState = rememberLazyListState()

                val topBarHeight = 48.dp

                val barsBackground = MaterialTheme.colors.surface.copy(alpha = 0.94f)
                val barsDividerColor = MaterialTheme.colors.divider

                LaunchedEffect(listState) {
                    val selectedIndex = services.value.indexOf(selected)
                    if (selectedIndex != -1) {
                        listState.scrollToItem(selectedIndex)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 300.dp, max = 600.dp)
                        .verticalScrollBar(
                            state = listState,
                            padding = PaddingValues(top = topBarHeight),
                        ),
                    contentPadding = PaddingValues(top = topBarHeight),
                ) {
                    items(items = services.value) {
                        val isSelected = it == selected
                        ServiceItem(
                            onClick = {
                                dismissRequester.dismiss()
                                if (!isSelected) {
                                    onSelectedItem(it)
                                }
                            },
                            onLongClick = {
                                hapticFeedback.performLongPress()
                                onItemLongClick?.invoke(it)
                            },
                            service = it,
                            isSelected = isSelected,
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(topBarHeight)
                        .background(barsBackground)
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                color = barsDividerColor,
                                topLeft = Offset(0f, size.height - 1.dp.toPx()),
                                size = size.copy(height = 1.dp.toPx()),
                            )
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        )
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(CommonUiR.drawable.ic_settings),
                        contentDescription = stringResource(BaseR.string.service_management),
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(bounded = false),
                            onClick = {
                                dismissRequester.dismiss()
                                onManagementClick()
                            },
                        ),
                    )

                    Text(
                        text = stringResource(BaseR.string.services),
                        fontWeight = FontWeight.Bold,
                    )

                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(BaseR.string.close),
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(bounded = false),
                            onClick = { dismissRequester.dismiss() },
                        ),
                    )
                }
            }
        }
    }
}

private val slideSpring = spring<IntOffset>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

private val floatSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessLow,
)

@Composable
private fun AnimatedDialogContent(
    dismissRequester: AnimatedPopupDismissRequester,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visibleState = dismissRequester.visibleState,
        enter = slideIn(animationSpec = slideSpring) {
            IntOffset(0, -it.height)
        } + fadeIn(
            animationSpec = tween(
                durationMillis = 300,
                easing = FastOutSlowInEasing,
            )
        ) + scaleIn(
            initialScale = 0.9f,
            animationSpec = floatSpring,
        ),
        exit = slideOut(
            animationSpec = tween(
                durationMillis = 255,
                easing = FastOutSlowInEasing,
            )
        ) {
            IntOffset(0, -it.height)
        } + fadeOut(
            animationSpec = tween(
                durationMillis = 255,
                easing = FastOutSlowInEasing,
            )
        ) + scaleOut(
            targetScale = 0.9f,
            animationSpec = tween(
                durationMillis = 255,
                easing = FastOutSlowInEasing,
            )
        ),
    ) {
        Surface(
            modifier = modifier,
            color = MaterialTheme.colors.surface,
            shape = MaterialTheme.shapes.medium,
            elevation = 6.dp,
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ServiceItem(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    service: UiServiceManifest,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    val textColor = if (isSelected) {
        if (MaterialTheme.colors.isLight) {
            if (service.themeColor != null) {
                Color(service.themeColor!!)
            } else {
                MaterialTheme.colors.primary
            }
        } else {
            if (service.darkThemeColor != null) {
                Color(service.darkThemeColor!!)
            } else {
                MaterialTheme.colors.primary
            }
        }
    } else {
        MaterialTheme.colors.onBackground
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(MaterialTheme.shapes.small)
            .drawWithContent {
                drawContent()
                if (isSelected) {
                    drawRoundRect(
                        color = textColor,
                        topLeft = Offset(2.dp.toPx(), this.size.height * 0.1f),
                        size = Size(6.dp.toPx(), this.size.height * 0.8f),
                        cornerRadius = CornerRadius(6.dp.toPx())
                    )
                }
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(),
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(
            name = service.name,
            url = service.localFirstResourcePath(
                type = ServiceResource.Type.Icon,
                fallback = { service.icon }
            ),
            size = 36.dp
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(text = service.name, color = textColor)
    }
}
