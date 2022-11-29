package any.ui.common.video

import any.base.R as BaseR
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import any.base.image.ImageRequest
import any.ui.common.R
import any.ui.common.image.AsyncImage
import any.ui.common.rememberScale
import com.google.android.exoplayer2.ui.StyledPlayerView
import kotlinx.coroutines.delay

private const val CONTROL_BACKGROUND_OPACITY = 0.6f

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun VideoView(
    state: VideoPlaybackState,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    thumbnail: String? = null,
    playImmediately: Boolean = false,
    aspectRatio: Float = 16f / 9,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    var playerView by remember {
        mutableStateOf<StyledPlayerView?>(null)
    }

    val scrimColor by animateColorAsState(
        if (state.error != null) {
            Color.Black.copy(alpha = 0.6f)
        } else {
            Color.Transparent
        }
    )

    DisposableEffect(state, playerView) {
        val view = playerView
        if (view != null) {
            state.attachToView(view)
        }
        onDispose {
            if (view != null) {
                state.detachFromView(view)
            }
        }
    }

    DisposableEffect(lifecycleOwner, state) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> state.pause()

                Lifecycle.Event.ON_START -> {
                    state.init()
                    if (playImmediately) {
                        state.play()
                    }
                }

                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    onClick?.invoke()
                    if (state.isPlaying) {
                        state.pause()
                    } else if (state.isPlayed) {
                        state.play()
                    }
                },
            ),
    ) {
        Box(
            modifier = Modifier
                .drawWithContent {
                    drawContent()
                    if (scrimColor.alpha > 0) {
                        drawRect(color = scrimColor)
                    }
                },
        ) {
            if (state.isPlayed) {
                AndroidView(
                    factory = { context ->
                        createPlayerView(context).also {
                            playerView = it
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (!state.isRenderedFirstFrame && !thumbnail.isNullOrEmpty()) {
                AsyncImage(
                    request = ImageRequest.Url(thumbnail),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        AnimatedVisibility(
            visible = !state.isPlaying && !state.isBuffering && state.error == null,
            modifier = Modifier.align(Alignment.Center),
            enter = scaleIn(initialScale = 1.5f) + fadeIn(),
            exit = scaleOut(targetScale = 1.5f) + fadeOut(),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_outline_play_arrow_24),
                contentDescription = null,
                modifier = Modifier
                    .size(96.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberScale(targetScale = 0.8f),
                        onClick = { state.play() }
                    ),
                tint = Color.White,
            )
        }

        if (state.isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.TopEnd)
                    .offset((-16).dp, 16.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
        }

        val error = state.error
        if (error != null) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(BaseR.string._playback_error, error.errorCodeName),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { state.play() },
                    shape = CircleShape,
                ) {
                    Text(stringResource(BaseR.string.retry))
                }
            }
        }

        var controlsVisible by remember { mutableStateOf(true) }

        LaunchedEffect(state.isPlaying, controlsVisible) {
            controlsVisible = if (state.isPlaying) {
                delay(2000)
                false
            } else {
                true
            }
        }

        ControlBar(
            onShowRequest = { controlsVisible = true },
            onMuteClick = { state.isMuted = !state.isMuted },
            onFullScreenClick = {},
            controlsVisible = controlsVisible,
            duration = state.duration,
            progress = state.progress,
            isMuted = state.isMuted,
            showFullScreenButton = state.isPlayed,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun ControlBar(
    onShowRequest: () -> Unit,
    onMuteClick: () -> Unit,
    onFullScreenClick: () -> Unit,
    controlsVisible: Boolean,
    duration: Long,
    progress: Float,
    isMuted: Boolean,
    showFullScreenButton: Boolean,
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
                .graphicsLayer {
                    alpha = controlsAlpha
                },
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

            ControlButton(onClick = onMuteClick) {
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

            if (showFullScreenButton) {
                Spacer(modifier = Modifier.width(8.dp))

                ControlButton(onClick = onFullScreenClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_fullscreen_24),
                        contentDescription = null,
                    )
                }
            }
        }

        if (progress >= 0f) {
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                backgroundColor = Color.Transparent,
            )
        }
    }
}

@Composable
private fun ControlButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
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

private fun createPlayerView(context: Context): StyledPlayerView {
    return StyledPlayerView(context).also {
        it.useController = false
    }
}