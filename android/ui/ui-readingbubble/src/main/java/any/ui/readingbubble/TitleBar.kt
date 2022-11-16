package any.ui.readingbubble

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.R
import kotlinx.coroutines.delay

@Composable
internal fun TitleBar(
    showClearAll: Boolean,
    onClearAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
    ) {
        var showClearConfirm by remember { mutableStateOf(false) }

        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.now_reading),
                modifier = Modifier.weight(1f),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )

            if (showClearAll) {
                IconButton(onClick = { showClearConfirm = true }) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colors.onBackground.copy(alpha = 0.1f),
                                shape = CircleShape,
                            )
                            .padding(6.dp),
                    ) {
                        Icon(
                            painter = painterResource(
                                any.ui.common.R.drawable.ic_baseline_cleaning_services_24
                            ),
                            contentDescription = stringResource(R.string.clear_all),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colors.error,
                        )
                    }
                }
            } else {
                Spacer(
                    modifier = Modifier.size(
                        LocalViewConfiguration.current.minimumTouchTargetSize
                    )
                )
            }
        }

        AnimatedVisibility(
            visible = showClearConfirm,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
        ) {
            LaunchedEffect(Unit) {
                delay(2500)
                showClearConfirm = false
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .background(MaterialTheme.colors.error)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.sure_to_clear),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colors.onError,
                    fontSize = 18.sp,
                )

                ClearConfirmButton(
                    onClick = {
                        showClearConfirm = false
                        onClearAllClick()
                    },
                ) {
                    Text(stringResource(R.string.clear))
                }

                Spacer(modifier = Modifier.width(8.dp))

                ClearConfirmButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        }
    }
}

@Composable
private fun ClearConfirmButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .border(
                width = 1.dp,
                color = MaterialTheme.colors.onError,
                shape = CircleShape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colors.onError,
            LocalTextStyle provides LocalTextStyle.current.copy(fontSize = 14.sp),
        ) {
            text()
        }
    }
}
