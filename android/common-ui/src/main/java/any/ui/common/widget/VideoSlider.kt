package any.ui.common.widget

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import java.util.Calendar
import kotlin.math.abs

/**
 * A simple video slider displays:
 * - The track bar and the progress bar when it's inactive
 * - The track bar, the progress bar, and the thumb (If present) when it's active (dragging).
 */
@Composable
fun VideoSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    thumb: @Composable (() -> Unit)? = { DefaultSliderThumb() },
    trackHeight: Dp = 3.dp,
    activeTrackHeight: Dp = 8.dp,
    trackRadius: Dp = 0.dp,
    activeTrackRadius: Dp = 0.dp,
    trackColor: Color = Color.Black.copy(alpha = 0.3f),
    progressColor: Color = MaterialTheme.colors.primary,
) {
    val density = LocalDensity.current

    val touchSlop = LocalViewConfiguration.current.touchSlop

    val sliderHeight = max(trackHeight, activeTrackHeight)

    val safeValue = value.coerceIn(0f, 1f)

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val trackWidth = constraints.maxWidth

        var isDraggingSlider by remember { mutableStateOf(false) }

        val currTrackHeight by animateDpAsState(
            targetValue = if (isDraggingSlider) activeTrackHeight else trackHeight,
            label = "trackHeight",
        )

        val currTrackRadius by animateDpAsState(
            targetValue = if (isDraggingSlider) activeTrackRadius else trackRadius,
            label = "trackRadius",
        )

        var dragStartX by remember { mutableFloatStateOf(-1f) }

        var dragX by remember { mutableFloatStateOf(-1f) }

        fun updateValueFromDragX() {
            val newValue = dragX.coerceIn(0f, trackWidth.toFloat()) / trackWidth
            onValueChange(newValue)
        }

        fun onDragEventStart(x: Float) {
            val progressX = trackWidth * safeValue
            val halfTouchSize = with(density) { 48.dp.toPx() } / 2
            val canStart = x in (progressX - halfTouchSize)..(progressX + halfTouchSize)
            isDraggingSlider = canStart
            dragStartX = x
            dragX = x
        }

        fun onDragEventStopped() {
            if (!isDraggingSlider && abs(dragX - dragStartX) <= touchSlop) {
                // Clicks
                updateValueFromDragX()
            }
            isDraggingSlider = false
            dragStartX = -1f
            dragX = -1f
        }

        fun onDrag(deltaX: Float) {
            dragX += deltaX
            if (!isDraggingSlider) {
                return
            }
            updateValueFromDragX()
        }

        Spacer(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(sliderHeight)
                .drawWithCache {
                    val trackHeightPx = currTrackHeight.toPx()
                    val trackTopLeft = Offset(0f, size.height - trackHeightPx)
                    val trackSize = size.copy(height = trackHeightPx)
                    onDrawBehind {
                        val cornerRadius = CornerRadius(currTrackRadius.toPx())
                        // Draw track
                        drawRoundRect(
                            color = trackColor,
                            topLeft = trackTopLeft,
                            size = trackSize,
                            cornerRadius = cornerRadius,
                        )
                        // Draw progress
                        clipRect(right = trackSize.width * safeValue) {
                            drawRoundRect(
                                color = progressColor,
                                topLeft = trackTopLeft,
                                size = trackSize,
                                cornerRadius = cornerRadius,
                            )
                        }
                    }
                }
                .draggable(
                    state = rememberDraggableState(onDelta = ::onDrag),
                    orientation = Orientation.Horizontal,
                    startDragImmediately = true,
                    onDragStarted = {
                        onDragEventStart(it.x)
                    },
                    onDragStopped = {
                        onDragEventStopped()
                    },
                )
        )
        if (thumb != null) {
            val thumbScale by animateFloatAsState(
                targetValue = if (isDraggingSlider) 1f else 0f,
                label = "thumbScale",
            )

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = thumbScale
                        scaleY = thumbScale
                        translationX = -size.width / 2 + trackWidth * safeValue
                        translationY = (size.height - sliderHeight.toPx()) / 2
                        val pivotY = 0.5f + translationY / size.height
                        transformOrigin = TransformOrigin(0f, pivotY)
                    },
            ) {
                thumb()
            }
        }
    }
}

@Composable
private fun DefaultSliderThumb() {
    val (month, day) = remember {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.get(Calendar.MONTH) + 1 to calendar.get(Calendar.DAY_OF_MONTH)
    }
    when {
        month == 2 && day == 14 -> {
            // Heart with arrow
            TextSliderThumb(text = "\uD83D\uDC98")
        }

        month == 3 && day == 31 -> {
            // Rabbit face
            TextSliderThumb(text = "\uD83D\uDC30")
        }

        month == 10 && day == 31 -> {
            // Jack-O-Lantern
            TextSliderThumb(text = "🎃")
        }

        month == 12 && day == 25 -> {
            // Christmas tree
            TextSliderThumb(text = "\uD83C\uDF84")
        }

        else -> {
            CircleSliderThumb()
        }
    }
}

@Composable
private fun CircleSliderThumb(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    color: Color = MaterialTheme.colors.primary,
) {
    Spacer(
        modifier = modifier
            .size(size)
            .background(
                shape = CircleShape,
                color = color,
            )
    )
}

@Composable
private fun TextSliderThumb(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
    )
}