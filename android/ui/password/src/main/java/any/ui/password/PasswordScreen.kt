package any.ui.password

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.ui.common.gesture.detectEventActions
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import any.base.R as BaseR

private val MAX_INPUTS_WIDTH = 320.dp

private const val DEL_CHAR = "←"

private const val DONE_CHAR = "√"

private const val MAX_PASSWORD_LEN = 20

private val ERROR_COLOR = Color(0xFFE62955)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PasswordScreen(
    onConfirm: (password: String) -> Boolean,
    modifier: Modifier = Modifier,
) {
    var password by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    val errorAnimMaxOffsetX = with(LocalDensity.current) { 24.dp.toPx() }
    val errorAnimOffsetX = remember { Animatable(0f) }

    val defInputFieldColor = MaterialTheme.colors.onBackground.copy(alpha = 0.55f)
    var inputFieldColor by remember { mutableStateOf(defInputFieldColor) }

    val iconRotation = remember { Animatable(0f) }

    val hapticFeedback = LocalHapticFeedback.current

    var deleteCharJob: Job? = null

    fun startDeleteChars() {
        deleteCharJob = scope.launch {
            var deleteDelay = 300L
            while (password.isNotEmpty()) {
                delay(deleteDelay)
                password = password.substring(0, password.length - 1)
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                if (deleteDelay > 100) {
                    deleteDelay -= 50
                }
            }
        }
    }

    fun stopDeleteChars() {
        deleteCharJob?.cancel()
    }

    @Composable
    fun CharFieldHeader(modifier: Modifier = Modifier) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(BaseR.drawable.ic_app_icon),
                contentDescription = "",
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            scope.launch {
                                iconRotation.stop()
                            }
                        },
                        onLongClick = {
                            scope.launch {
                                animateIcon(iconRotation)
                            }
                        }
                    )
                    .graphicsLayer {
                        rotationZ = iconRotation.value
                    },
            )

            CharacterField(
                password = password,
                color = inputFieldColor,
                modifier = Modifier
                    .widthIn(max = MAX_INPUTS_WIDTH)
                    .offset {
                        IntOffset(
                            x = (errorAnimOffsetX.value * errorAnimMaxOffsetX).toInt(),
                            y = 0
                        )
                    },
            )
        }
    }

    @Composable
    fun NumberInputPanel(modifier: Modifier = Modifier) {
        NumberPanel(
            onTextEnter = { text ->
                if (inputFieldColor != defInputFieldColor) {
                    inputFieldColor = defInputFieldColor
                }

                when (text) {
                    DONE_CHAR -> {
                        val correct = onConfirm(password)
                        if (!correct) {
                            inputFieldColor = ERROR_COLOR
                            scope.launch {
                                animateErrorInput(errorAnimOffsetX)
                            }
                        }
                    }

                    DEL_CHAR -> {
                        if (password.isNotEmpty()) {
                            password = password.substring(0, password.length - 1)
                        }
                    }

                    else -> {
                        if (password.length < MAX_PASSWORD_LEN) {
                            password += text
                        }
                    }
                }
            },
            onDeleteDown = {
                startDeleteChars()
            },
            onDeleteUp = {
                stopDeleteChars()
            },
            modifier = modifier,
        )
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (maxWidth >= MAX_INPUTS_WIDTH * 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CharFieldHeader(
                    modifier = Modifier
                        .widthIn(max = MAX_INPUTS_WIDTH)
                )
                NumberInputPanel(
                    modifier = Modifier
                        .widthIn(max = MAX_INPUTS_WIDTH)
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(0.6f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CharFieldHeader(
                    modifier = Modifier.widthIn(max = MAX_INPUTS_WIDTH)
                )
                NumberInputPanel(
                    modifier = Modifier.widthIn(max = MAX_INPUTS_WIDTH)
                )
            }

        }
    }
}

private suspend fun animateIcon(
    rotation: Animatable<Float, AnimationVector1D>
) {
    rotation.animateTo(
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2500
                0.0f at 0
                360f at 2500
            },
            repeatMode = RepeatMode.Restart
        )
    )
}

private suspend fun animateErrorInput(
    offsetX: Animatable<Float, AnimationVector1D>
) {
    offsetX.animateTo(
        targetValue = 0f,
        animationSpec = keyframes {
            durationMillis = 900
            0.0f at 0
            1.0f at 100
            -1.9f at 200
            0.6f at 300
            -0.6f at 400
            0.4f at 500
            -0.4f at 600
            0.2f at 700
            -0.1f at 800
            0.0f at 900
        }
    )
}

@Composable
private fun CharacterField(
    password: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    var borderVisible by remember {
        mutableStateOf(false)
    }

    var size by remember {
        mutableStateOf(IntSize(0, 0))
    }

    val density = LocalDensity.current

    val borderStoke = remember(density) {
        Stroke(width = with(density) { 1.dp.toPx() })
    }

    val animatedBorderPath = animatedBorderPath(
        shape = MaterialTheme.shapes.small,
        size = size,
        borderVisible = borderVisible
    )

    val chars by remember(password) {
        mutableStateOf(password.toCharArray().toList())
    }

    LaunchedEffect(Unit) {
        borderVisible = true
    }

    Column(
        modifier = modifier.padding(16.dp)
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .widthIn(max = MAX_INPUTS_WIDTH)
                .onSizeChanged { size = it }
                .drawBehind {
                    if (animatedBorderPath != null) {
                        drawPath(
                            path = animatedBorderPath,
                            color = color,
                            style = borderStoke
                        )
                    }
                },
            contentPadding = PaddingValues(0.dp, 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            items(chars) {
                val state = remember {
                    MutableTransitionState(false).apply {
                        targetState = true
                    }
                }
                AnimatedVisibility(
                    visibleState = state,
                    enter = scaleIn() + slideInVertically { it / 2 },
                    exit = scaleOut(),
                ) {
                    Text(
                        text = "*",
                        fontSize = 22.sp,
                        modifier = Modifier.padding(6.dp, 0.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun animatedBorderPath(
    shape: Shape,
    size: IntSize,
    borderVisible: Boolean,
    animationSpec: AnimationSpec<Float> = tween(durationMillis = 375, delayMillis = 75)
): Path? {
    val borderAnimValue = animateFloatAsState(
        targetValue = if (borderVisible) 1f else 0f,
        animationSpec = animationSpec,
        label = "border",
    )
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val borderPath = remember(size) {
        if (size.width != 0 && size.height != 0) {
            Path().apply {
                addOutline(
                    shape.createOutline(
                        size = Size(size.width.toFloat(), size.height.toFloat()),
                        layoutDirection = layoutDirection,
                        density = density
                    )
                )
            }
        } else {
            null
        }
    }
    val pathMeasure = remember(borderPath) {
        if (borderPath != null) {
            PathMeasure().apply {
                setPath(borderPath, forceClosed = false)
            }
        } else {
            null
        }
    }
    val animatedPath = remember { Path() }
    return remember(borderPath, borderAnimValue.value) {
        if (borderPath != null) {
            pathMeasure!!.getSegment(
                startDistance = 0f,
                stopDistance = pathMeasure.length * borderAnimValue.value,
                destination = animatedPath,
                startWithMoveTo = true
            )
            animatedPath
        } else {
            null
        }
    }
}

@Composable
private fun NumberPanel(
    onTextEnter: (text: String) -> Unit,
    modifier: Modifier = Modifier,
    onDeleteDown: (() -> Unit)? = null,
    onDeleteUp: (() -> Unit)? = null,
) {
    val chars = (1..9).map { it.toString() }
    val rowCharCount = 3
    val rows = chars.size / rowCharCount
    Column(modifier = modifier) {
        var animDelay = 0
        for (i in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (j in 0 until rowCharCount) {
                    val char = chars[rowCharCount * i + j]
                    AnimatedNumberButton(
                        char = char,
                        onTextEnter = onTextEnter,
                        animationDelay = animDelay,
                    )
                    animDelay += 20
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AnimatedNumberButton(
                char = DEL_CHAR,
                onTextEnter = onTextEnter,
                animationDelay = animDelay,
                onPointerDown = onDeleteDown,
                onPointerUp = onDeleteUp,
            )
            AnimatedNumberButton(
                char = "0",
                onTextEnter = onTextEnter,
                animationDelay = animDelay,
            )
            AnimatedNumberButton(
                char = DONE_CHAR,
                animationDelay = animDelay,
                onTextEnter = onTextEnter,
                borderColor = Color.Transparent,
                buttonColor = MaterialTheme.colors.secondary,
                textColor = Color.White
            )
        }
    }
}

@Composable
private fun AnimatedNumberButton(
    char: String,
    onTextEnter: (text: String) -> Unit,
    modifier: Modifier = Modifier,
    animationDelay: Int = 0,
    clickable: Boolean = true,
    borderColor: Color = MaterialTheme.colors.onBackground.copy(alpha = 0.2f),
    buttonColor: Color = Color.Transparent,
    textColor: Color = MaterialTheme.colors.onBackground,
    onPointerDown: (() -> Unit)? = null,
    onPointerUp: (() -> Unit)? = null,
) {
    var visible by remember {
        mutableStateOf(false)
    }
    val scale = animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy + 0.05f,
            stiffness = Spring.StiffnessMediumLow + 150
        ),
        label = "scale"
    )
    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        visible = true
    }
    NumberButton(
        char = char,
        onTextEnter = onTextEnter,
        modifier = modifier.scale(scale.value),
        clickable = clickable,
        borderColor = borderColor,
        buttonColor = buttonColor,
        textColor = textColor,
        onPointerDown = onPointerDown,
        onPointerUp = onPointerUp,
    )
}

@Composable
private fun NumberButton(
    char: String,
    onTextEnter: (text: String) -> Unit,
    modifier: Modifier = Modifier,
    clickable: Boolean = true,
    borderColor: Color = MaterialTheme.colors.onBackground.copy(alpha = 0.2f),
    buttonColor: Color = Color.Transparent,
    textColor: Color = MaterialTheme.colors.onBackground,
    onPointerDown: (() -> Unit)? = null,
    onPointerUp: (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()

    val scale = remember {
        Animatable(1f)
    }

    val onBgColor = MaterialTheme.colors.onBackground
    val buttonColorPressed = remember(buttonColor) {
        lerp(buttonColor, onBgColor, 0.1f)
    }
    var currButtonColor by remember(buttonColor) {
        mutableStateOf(buttonColor)
    }

    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .scale(scale.value)
            .size(70.dp)
            .padding(8.dp)
            .border(
                width = 1.2.dp,
                color = borderColor,
                shape = CircleShape
            )
            .background(currButtonColor, CircleShape)
            .pointerInput(EventAction) {
                detectEventActions(
                    onDown = {
                        currButtonColor = buttonColorPressed
                        onPointerDown?.invoke()
                        scope.launch {
                            scale.animateTo(1.25f)
                        }
                    },
                    onUp = {
                        currButtonColor = buttonColor
                        onPointerUp?.invoke()
                        scope.launch {
                            scale.animateTo(1f)
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (!clickable) {
                            return@detectTapGestures
                        }
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onTextEnter(char)
                    }
                )
            }
    ) {
        Text(
            text = char,
            modifier = Modifier.align(Alignment.Center),
            fontSize = 24.sp,
            color = textColor,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Light
        )
    }
}

object EventAction