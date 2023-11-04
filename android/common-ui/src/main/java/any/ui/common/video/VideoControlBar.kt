package any.ui.common.video

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.ui.common.R
import any.ui.common.widget.VideoSlider

private const val CONTROL_BACKGROUND_OPACITY = 0.6f

@Composable
internal fun VideoControlBar(
    onShowRequest: () -> Unit,
    onMuteClick: () -> Unit,
    onFullscreenClick: () -> Unit,
    onSeek: (progress: Float) -> Unit,
    controlsVisible: Boolean,
    duration: Long,
    progress: Float,
    isMuted: Boolean,
    showFullscreenButton: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .run {
                if (!controlsVisible) {
                    clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onShowRequest,
                    )
                } else {
                    this
                }
            },
    ) {
        val controlsAlpha by animateFloatAsState(
            targetValue = if (controlsVisible) 1f else 0f,
            animationSpec = if (controlsVisible) {
                tween(durationMillis = 175)
            } else {
                tween(durationMillis = 575)
            },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .graphicsLayer { alpha = controlsAlpha },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (duration >= 0 && progress >= 0f) {
                val rest = (duration * (1f - progress)).toLong()
                val minutes = rest / 1000 / 60
                val seconds = (rest - minutes * 1000 * 60) / 1000
                Text(
                    text = String.format("%d:%02d", minutes, seconds),
                    modifier = Modifier
                        .background(
                            color = Color.Black.copy(alpha = CONTROL_BACKGROUND_OPACITY),
                            shape = MaterialTheme.shapes.small,
                        )
                        .padding(
                            horizontal = 8.dp,
                            vertical = 2.dp
                        ),
                    fontSize = 14.sp,
                    color = Color.White,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            ControlButton(
                onClick = {
                    if (controlsVisible) {
                        onMuteClick()
                    } else {
                        onShowRequest()
                    }
                },
            ) {
                Icon(
                    painter = painterResource(
                        if (isMuted) {
                            R.drawable.ic_baseline_volume_off_24
                        } else {
                            R.drawable.ic_baseline_volume_up_24
                        }
                    ),
                    contentDescription = null,
                )
            }

            if (showFullscreenButton) {
                Spacer(modifier = Modifier.width(8.dp))

                ControlButton(
                    onClick = {
                        if (controlsVisible) {
                            onFullscreenClick()
                        } else {
                            onShowRequest()
                        }
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_fullscreen_24),
                        contentDescription = null,
                    )
                }
            }
        }

        if (progress >= 0f) {
            VideoSlider(
                value = progress,
                onValueChange = onSeek,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun ControlButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    icon: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Black.copy(0.6f))
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        CompositionLocalProvider(
            LocalContentColor provides Color.White,
        ) {
            icon()
        }
    }
}