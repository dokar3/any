package any.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import any.base.util.compose.performLongPress

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SelectionChip(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    text: @Composable () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val borderColor = if (isSelected) {
        MaterialTheme.colors.primary
    } else {
        MaterialTheme.colors.onBackground
    }
    val textColor = if (isSelected) {
        Color.White
    } else {
        MaterialTheme.colors.onBackground
    }
    Column(
        modifier = modifier
            .padding(end = 8.dp)
            .height(32.dp)
            .border(1.dp, borderColor, CircleShape)
            .clip(CircleShape)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onClick() },
                onLongClick = {
                    if (onLongClick != null) {
                        hapticFeedback.performLongPress()
                        onLongClick()
                    }
                },
            )
            .let {
                if (isSelected) {
                    it.background(borderColor)
                } else {
                    it
                }
            }
            .padding(16.dp, 0.dp),
        verticalArrangement = Arrangement.Center
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides LocalTextStyle.current.copy(color = textColor),
        ) {
            text()
        }
    }
}