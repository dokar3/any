package any.ui.common.video

import any.base.R as BaseR
import android.view.Gravity
import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import any.base.image.ImageRequest
import any.ui.common.R
import any.ui.common.image.AsyncImage
import any.ui.common.rememberScale
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun VideoView(
    state: VideoPlaybackState,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    thumbnail: String? = null,
    playImmediately: Boolean = false,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    var playerView by remember { mutableStateOf<TextureView?>(null) }

    val scrimColor by animateColorAsState(
        if (state.error != null) {
            Color.Black.copy(alpha = 0.6f)
        } else {
            Color.Transparent
        }
    )

    var size by remember { mutableStateOf(IntSize.Zero) }

    var textureViewRotation by remember { mutableStateOf(0) }

    val onLayoutChangeListener = remember {
        View.OnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
            TextureViewUtil.applyTextureViewRotation(
                view as TextureView,
                textureViewRotation
            )
        }
    }

    /**
     * Copied from StyledPlayerView.updateAspectRatio()
     */
    fun applyAspectRatio(
        view: TextureView,
        videoAspectRatio: Float,
        unappliedRotationDegrees: Int,
    ) {
        val containerSize = size
        if (containerSize.width == 0 || containerSize.height == 0) {
            return
        }

        var mutVideoAspectRatio = videoAspectRatio
        // Try to apply rotation transformation when our surface is a TextureView.
        if (mutVideoAspectRatio > 0
            && (unappliedRotationDegrees == 90 || unappliedRotationDegrees == 270)
        ) {
            // We will apply a rotation 90/270 degree to the output texture of the TextureView.
            // In this case, the output video's width and height will be swapped.
            mutVideoAspectRatio = 1 / mutVideoAspectRatio
        }
        if (textureViewRotation != 0) {
            view.removeOnLayoutChangeListener(onLayoutChangeListener)
        }
        textureViewRotation = unappliedRotationDegrees
        if (textureViewRotation != 0) {
            // The texture view's dimensions might be changed after layout step.
            // So add an OnLayoutChangeListener to apply rotation after layout step.
            view.addOnLayoutChangeListener(onLayoutChangeListener)
        }
        TextureViewUtil.applyTextureViewRotation(view, textureViewRotation)

        view.layoutParams.run {
            if (mutVideoAspectRatio > 1) {
                width = containerSize.width
                height = (containerSize.width / mutVideoAspectRatio).toInt()
            } else {
                height = containerSize.height
                width = (containerSize.height * mutVideoAspectRatio).toInt()
            }
        }
        view.requestLayout()
    }

    LaunchedEffect(state.videoSize, playerView) {
        val view = playerView ?: return@LaunchedEffect
        val videoSize = state.videoSize
        val width: Int = videoSize.width
        val height: Int = videoSize.height
        val unappliedRotationDegrees: Int = videoSize.unappliedRotationDegrees
        val videoAspectRatio: Float = if (height != 0 && width != 0) {
            width * videoSize.pixelWidthHeightRatio / height
        } else {
            0f
        }
        if (videoAspectRatio > 0f) {
            applyAspectRatio(view, videoAspectRatio, unappliedRotationDegrees)
        }
    }

    DisposableEffect(state, playerView) {
        val view = playerView
        if (view != null) {
            state.attachToView(view)
        }
        onDispose {
            if (view != null) {
                view.removeOnLayoutChangeListener(onLayoutChangeListener)
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
            .onSizeChanged { size = it }
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
                        val textureView = TextureView(context)
                        playerView = textureView

                        FrameLayout(context).apply {
                            val params = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            ).also {
                                it.gravity = Gravity.CENTER
                            }
                            addView(textureView, params)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (!thumbnail.isNullOrEmpty() && !state.isRenderedFirstFrame) {
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

        VideoControlBar(
            onShowRequest = { controlsVisible = true },
            onMuteClick = { state.isMuted = !state.isMuted },
            onFullscreenClick = {},
            controlsVisible = controlsVisible,
            duration = state.duration,
            progress = state.progress,
            isMuted = state.isMuted,
            showFullscreenButton = state.isPlayed,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
