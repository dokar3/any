package any.ui.common

import any.base.R as BaseR
import any.ui.common.R as CommonUiR
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import any.base.ImmutableHolder
import any.base.util.compose.performLongPress
import any.data.entity.ServiceResource
import any.domain.entity.UiServiceManifest
import any.ui.common.modifier.verticalScrollBar
import any.ui.common.theme.divider
import any.ui.common.widget.AnimatedPopup
import any.ui.common.widget.Avatar
import any.ui.common.widget.rememberAnimatedPopupDismissRequester

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun ServicesDropdown(
    services: ImmutableHolder<List<UiServiceManifest>>,
    selected: UiServiceManifest?,
    onDismissRequest: () -> Unit,
    onManagementClick: () -> Unit,
    onSelectedItem: (UiServiceManifest) -> Unit,
    modifier: Modifier = Modifier,
    onItemLongClick: ((UiServiceManifest) -> Unit)? = null,
    contentAlignmentToAnchor: Alignment = Alignment.TopEnd,
    transformOrigin: TransformOrigin = TransformOrigin(1f, 0f),
) {
    val dismissRequester = rememberAnimatedPopupDismissRequester()
    AnimatedPopup(
        dismissRequester = dismissRequester,
        onDismissed = onDismissRequest,
        modifier = modifier,
        enterTransition = scaleIn(
            animationSpec = spring(),
            transformOrigin = transformOrigin,
        ),
        exitTransition = fadeOut(
            animationSpec = tween(durationMillis = 175),
        ),
        shape = MaterialTheme.shapes.medium,
        contentAlignmentToAnchor = contentAlignmentToAnchor,
        properties = PopupProperties(focusable = true),
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(modifier = Modifier.heightIn(max = 500.dp)) {
            val scrollState = rememberScrollState()

            val topBarHeight = 32.dp
            val btmBarHeight = 48.dp

            Column(
                modifier = Modifier
                    .sizeIn(
                        minWidth = 240.dp,
                        maxWidth = 280.dp,
                    )
                    .width(IntrinsicSize.Max)
                    .verticalScroll(state = scrollState)
                    .verticalScrollBar(
                        state = scrollState,
                        padding = PaddingValues(top = topBarHeight, bottom = btmBarHeight),
                    ),
            ) {
                Spacer(modifier = Modifier.height(topBarHeight))

                val hapticFeedback = LocalHapticFeedback.current
                services.value.forEach { service ->
                    val isSelected = service == selected
                    val backgroundColor = if (isSelected) {
                        MaterialTheme.colors.primary.copy(alpha = 0.1f)
                    } else {
                        Color.Transparent
                    }

                    val bringIntoViewRequester = remember { BringIntoViewRequester() }

                    LaunchedEffect(bringIntoViewRequester) {
                        if (isSelected) {
                            bringIntoViewRequester.bringIntoView()
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .bringIntoViewRequester(bringIntoViewRequester)
                            .background(backgroundColor)
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = rememberRipple(),
                                onClick = {
                                    dismissRequester.dismiss()
                                    if (!isSelected) {
                                        onSelectedItem(service)
                                    }
                                },
                                onLongClick = {
                                    hapticFeedback.performLongPress()
                                    onItemLongClick?.invoke(service)
                                },
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

                        Text(service.name)
                    }
                }

                Spacer(modifier = Modifier.height(btmBarHeight))
            }

            val density = LocalDensity.current

            val barsBackground = MaterialTheme.colors.surface.copy(alpha = 0.94f)
            val barsDividerColor = MaterialTheme.colors.divider
            var topBarDividerAlpha by remember { mutableStateOf(0f) }
            var btmBarDividerAlpha by remember { mutableStateOf(0f) }

            LaunchedEffect(density, scrollState) {
                val topBarHeightPx = with(density) { topBarHeight.toPx() }
                val btmBarHeightPx = with(density) { btmBarHeight.toPx() }
                snapshotFlow { scrollState.value }
                    .collect {
                        topBarDividerAlpha = (it / topBarHeightPx).coerceIn(0f, 1f)
                        btmBarDividerAlpha = ((scrollState.maxValue - it) / btmBarHeightPx)
                            .coerceIn(0f, 1f)
                    }
            }

            Box(
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
                            alpha = topBarDividerAlpha,
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(BaseR.string.services),
                    fontWeight = FontWeight.Bold,
                )
            }

            DropdownMenuItem(
                onClick = {
                    dismissRequester.dismiss()
                    onManagementClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(btmBarHeight)
                    .align(Alignment.BottomCenter)
                    .background(barsBackground)
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            color = barsDividerColor,
                            size = size.copy(height = 1.dp.toPx()),
                            alpha = btmBarDividerAlpha,
                        )
                    },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(6.dp))

                    Icon(
                        painter = painterResource(CommonUiR.drawable.ic_settings),
                        contentDescription = stringResource(BaseR.string.service_management),
                    )

                    Spacer(modifier = Modifier.width(22.dp))

                    Text(stringResource(BaseR.string.management))
                }
            }
        }
    }
}
