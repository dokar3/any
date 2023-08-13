package any.ui.post.item

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import any.base.compose.copy
import any.richtext.RichString
import any.ui.common.richtext.RichTextBlocks
import any.ui.post.menu.PostHeadingOptionsPopup
import kotlinx.coroutines.delay

@Composable
internal fun HeadingItem(
    onAddToBookmarkClick: () -> Unit,
    onLinkClick: (String) -> Unit,
    richString: RichString,
    level: Int,
    modifier: Modifier = Modifier,
) {
    var showMoreButton by remember { mutableStateOf(false) }
    var showHeadingMenu by remember { mutableStateOf(false) }

    LaunchedEffect(showMoreButton, showHeadingMenu) {
        if (showMoreButton && !showHeadingMenu) {
            delay(1500)
            showMoreButton = false
        }
    }

    RichStringItem(
        richString = richString,
        onLinkClick = onLinkClick,
        onOtherTextClick = { showMoreButton = true },
    ) { text, textLayoutUpdater, clickModifier ->
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RichTextBlocks.Heading(
                text = text,
                onTextLayout = textLayoutUpdater,
                level = level,
                modifier = clickModifier.weight(1f),
                contentPadding = ItemsDefaults.ContentPadding.copy(
                    layoutDirection = LocalLayoutDirection.current,
                    top = 0.dp,
                    bottom = 0.dp,
                ),
            )

            Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                val alpha by animateFloatAsState(if (showMoreButton) 0.5f else 0f)
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null,
                    modifier = Modifier
                        .alpha(alpha)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(bounded = false),
                            onClick = {
                                if (alpha != 0f) {
                                    showHeadingMenu = true
                                } else {
                                    showMoreButton = true
                                }
                            },
                        ),
                )

                if (showHeadingMenu) {
                    PostHeadingOptionsPopup(
                        onDismissed = { showHeadingMenu = false },
                        onAddToBookmarkClick = onAddToBookmarkClick,
                    )
                }
            }
        }
    }
}