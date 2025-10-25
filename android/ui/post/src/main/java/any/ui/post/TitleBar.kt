package any.ui.post

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import any.base.util.compose.performLongPress
import any.domain.entity.UiPost
import any.ui.common.dialog.AddToCollectionsDialog
import any.ui.common.widget.CollectButton
import any.ui.common.widget.StatusBarSpacer
import any.base.R as BaseR

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TitleBar(
    post: UiPost?,
    onBackClick: () -> Unit,
    onCollectRequest: (UiPost) -> Unit,
    onDiscardRequest: (UiPost) -> Unit,
    modifier: Modifier = Modifier,
    onBackLongClick: (() -> Unit)? = null,
) {
    val hapticFeedback = LocalHapticFeedback.current

    Column(modifier = modifier.fillMaxWidth()) {
        StatusBarSpacer()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val iconSize = 36.dp

            val imageButtonBgColor = MaterialTheme.colors.background.copy(alpha = 0.83f)

            val currentOnBackLongClick = rememberUpdatedState(onBackLongClick)

            // Back button
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false),
                        onClick = onBackClick,
                        onLongClick = remember {
                            {
                                hapticFeedback.performLongPress()
                                currentOnBackLongClick.value?.invoke()
                            }
                        },
                    )
                    .background(imageButtonBgColor, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(BaseR.string.back),
                    modifier = Modifier,
                    tint = MaterialTheme.colors.onSurface,
                )
            }

            // Collect button
            if (post != null) {
                var showCollectionDialog by remember { mutableStateOf(false) }

                CollectButton(
                    isCollected = post.isCollected(),
                    onClick = {
                        if (post.isCollected()) {
                            onDiscardRequest(post)
                        } else {
                            onCollectRequest(post)
                        }
                    },
                    modifier = Modifier.background(
                        color = imageButtonBgColor,
                        shape = CircleShape
                    ),
                    onLongClick = remember {
                        {
                            hapticFeedback.performLongPress()
                            showCollectionDialog = true
                        }
                    },
                    size = iconSize,
                )

                if (showCollectionDialog) {
                    AddToCollectionsDialog(
                        onCollect = { onCollectRequest(it) },
                        onDismissRequest = { showCollectionDialog = false },
                        post = post,
                    )
                }
            }
        }
    }
}
