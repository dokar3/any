package any.ui.common.widget

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.compose.ImmutableHolder
import any.base.image.ImageLoader
import any.base.image.ImageRequest
import any.ui.common.image.rememberImageColorFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

private data class Block(
    val row: Int,
    val col: Int,
) {
    companion object {
        val None = Block(-1, -1)
    }
}

private data class BlocksInfo(
    val blockSize: Size = Size.Zero,
    val rows: Int = 0,
    val columns: Int = 0,
    val maxIconCount: Int = 0,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
) {
    fun findBlockAt(x: Float, y: Float): Block {
        if (x < 0 || y < 0) {
            return Block.None
        }
        if (blockSize.isEmpty()) {
            return Block.None
        }
        val row = (y / blockSize.height).toInt()
        if (row >= rows) {
            return Block.None
        }
        val col = (x / blockSize.width).toInt()
        if (col >= columns) {
            return Block.None
        }
        return Block(row, col)
    }
}

private object BlockMatrixHelper {
    fun outerBlocks(
        target: Block,
        distance: Int,
        rows: Int,
        columns: Int,
    ): List<Block> {
        if (distance <= 0) return emptyList()
        if (target == Block.None) return emptyList()
        val blocks = mutableListOf<Block>()
        if (target.row - distance >= 0) {
            // Add top side blocks
            val row = target.row - distance
            for (i in -distance..distance) {
                val col = target.col - i
                if (col in 0 until columns) {
                    blocks.add(Block(row = row, col = col))
                }
            }
        }
        if (target.col + distance < columns) {
            // Add right side blocks
            val col = target.col + distance
            for (i in (-distance + 1)..distance) {
                val row = target.row + i
                if (row in 0 until rows) {
                    blocks.add(Block(row = row, col = col))
                }
            }
        }
        if (target.row + distance < rows) {
            // Add bottom side blocks
            val row = target.row + distance
            for (i in -distance until distance) {
                val col = target.col + i
                if (col in 0 until columns) {
                    blocks.add(Block(row = row, col = col))
                }
            }
        }
        if (target.col - distance >= 0) {
            // Add left side blocks
            val col = target.col - distance
            for (i in (-distance + 1) until distance) {
                val row = target.row + i
                if (row in 0 until rows) {
                    blocks.add(Block(row = row, col = col))
                }
            }
        }
        return blocks
    }
}

private val RandomSeed = System.currentTimeMillis()

@OptIn(ExperimentalTextApi::class)
@Composable
fun DefaultServiceHeader(
    currentServiceName: String?,
    modifier: Modifier = Modifier,
    blockSize: Dp = 120.dp,
    blockColor: Color = MaterialTheme.colors.primary,
    onBlockColor: Color = Color.White,
    backgroundColor: Color = Color.Black,
    icons: ImmutableHolder<List<ImageRequest>> = ImmutableHolder(emptyList()),
) {
    val scope = rememberCoroutineScope()

    val fontFamilyResolver = LocalFontFamilyResolver.current

    val density = LocalDensity.current

    val layoutDirection = LocalLayoutDirection.current

    val iconColorFilter = rememberImageColorFilter()

    val blockScales = remember { mutableMapOf<Block, Float>() }

    val animJobs = remember { mutableListOf<Job>() }

    val blockAnimGroupMap = remember { mutableMapOf<Block, Int>() }

    val iconImages = remember { mutableMapOf<Int, ImageBitmap>() }

    var blocksInfo by remember { mutableStateOf(BlocksInfo()) }

    var redrawTick by remember { mutableStateOf(0) }

    val blockColorAnim = remember { Animatable(blockColor) }
    val onBlockColorAnim = remember { Animatable(onBlockColor) }

    val topInset = WindowInsets.statusBars.getTop(LocalDensity.current)

    suspend fun animateBlocksScale(
        blocks: List<Block>,
        from: Float,
        to: Float,
        delay: Long,
        duration: Int,
        easing: Easing,
    ) = coroutineScope {
        delay(delay)
        animate(
            initialValue = from,
            targetValue = to,
            animationSpec = tween(durationMillis = duration, easing = easing),
        ) { value, _ ->
            blocks.forEach { b -> blockScales[b] = value }
            redrawTick++
        }
    }

    fun onPointerDown(position: Offset) {
        // Find target block
        val block = blocksInfo.findBlockAt(
            x = position.x - blocksInfo.offsetX,
            y = position.y - blocksInfo.offsetY,
        )
        if (block == Block.None) return

        val minScale = 0.8f
        val scaleStep = (1f - minScale) / max(blocksInfo.rows, blocksInfo.columns)

        val animGroups = mutableListOf(listOf(block))

        var d = 1
        while (true) {
            // Get outer blocks of the target block in the block matrix, These blocks
            // will be used to simulate the animation when touching bouncing material
            val outerBlocks = BlockMatrixHelper.outerBlocks(
                target = block,
                distance = d,
                rows = blocksInfo.rows,
                columns = blocksInfo.columns,
            )
            if (outerBlocks.isEmpty()) {
                break
            }
            animGroups.add(outerBlocks)
            d++
        }

        // Cancel running animations
        animJobs.forEach { it.cancel() }
        animJobs.clear()

        for ((idx, blocks) in animGroups.withIndex()) {
            blocks.forEach { blockAnimGroupMap[it] = idx }
            val job = scope.launch {
                animateBlocksScale(
                    blocks = blocks,
                    from = 1f,
                    to = minScale + scaleStep * idx,
                    delay = idx * 75L,
                    duration = 500 + idx * 150,
                    easing = FastOutSlowInEasing,
                )
                redrawTick++
            }
            animJobs.add(job)
        }
    }

    fun onPointerUp() {
        if (blockScales.isEmpty()) return

        val animGroups = blockScales.entries
            .filter { it.value != 1f }
            .map { it.key }
            .groupBy { blockAnimGroupMap[it]!! }

        // Cancel running animations
        animJobs.forEach { it.cancel() }
        animJobs.clear()

        for ((idx, blocks) in animGroups) {
            val currScale = blockScales[blocks.first()]
            if (currScale == null) {
                blocks.forEach { blockScales.remove(it) }
                continue
            }
            val job = scope.launch {
                animateBlocksScale(
                    blocks = blocks,
                    from = currScale,
                    to = 1f,
                    delay = idx * 75L,
                    duration = 225,
                    easing = EaseOutBack,
                )
                blocks.forEach { blockScales.remove(it) }
                redrawTick++
            }
            animJobs.add(job)
        }

        blockAnimGroupMap.clear()
    }

    LaunchedEffect(icons, blocksInfo.maxIconCount) {
        if (blocksInfo.maxIconCount <= 0) {
            return@LaunchedEffect
        }
        val mutex = Mutex()
        val semaphore = Semaphore(permits = 5)
        for (i in 0 until blocksInfo.maxIconCount) {
            launch(Dispatchers.IO) {
                if (i >= icons.value.size) {
                    if (iconImages.contains(i)) {
                        delay(100)
                        iconImages.remove(i)
                        redrawTick++
                    }
                    return@launch
                }
                semaphore.withPermit {
                    val bitmap = ImageLoader.fetchBitmap(
                        request = icons.value[i],
                        finalResultOnly = true,
                    ).firstOrNull()
                    if (bitmap != null) {
                        mutex.withLock { iconImages[i] = bitmap.asImageBitmap() }
                    } else {
                        iconImages.remove(i)
                    }
                    redrawTick++
                }
            }
        }
    }

    LaunchedEffect(blockColorAnim, onBlockColorAnim) {
        snapshotFlow { blockColorAnim.value to onBlockColorAnim.value }
            .distinctUntilChanged()
            .collect { redrawTick++ }
    }

    LaunchedEffect(blockColor) {
        if (blockColorAnim.value == blockColor) {
            return@LaunchedEffect
        }
        blockColorAnim.animateTo(
            targetValue = blockColor,
            animationSpec = tween(durationMillis = 1200),
        )
    }

    LaunchedEffect(onBlockColor) {
        if (onBlockColorAnim.value == onBlockColor) {
            return@LaunchedEffect
        }
        onBlockColorAnim.animateTo(
            targetValue = onBlockColor,
            animationSpec = tween(durationMillis = 700),
        )
    }

    DisposableEffect(icons) {
        onDispose {
            icons.value.forEach { ImageLoader.detachRequest(it) }
        }
    }

    Spacer(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    onPointerDown(down.position)
                    waitForUpOrCancellation()
                    onPointerUp()
                }
            }
            .drawWithCache {
                val blockSizePx = blockSize.toPx()
                val columns = ceil(size.width / blockSizePx).toInt()
                val rows = ceil(size.height / blockSizePx).toInt()
                val maxIconCount = rows * columns / 2 + 1

                val blocksOffX = (size.width - columns * blockSizePx) / 2
                val blocksOffY = (size.height - rows * blockSizePx) / 2

                blocksInfo = BlocksInfo(
                    blockSize = Size(blockSizePx, blockSizePx),
                    rows = rows,
                    columns = columns,
                    maxIconCount = maxIconCount,
                    offsetX = blocksOffX,
                    offsetY = blocksOffY,
                )

                val blockSpacing = 8.dp.toPx()
                val blockCornerRadius = CornerRadius(8.dp.toPx())

                val colorBlocksOverlayBrush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.0f to Color.White.copy(alpha = 0.42f),
                        1.0f to Color.Transparent,
                    ),
                )

                val backgroundBrush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.0f to Color.White
                            .copy(alpha = 0.2f)
                            .compositeOver(backgroundColor),
                        1.0f to Color.White
                            .copy(alpha = 0.1f)
                            .compositeOver(backgroundColor),
                    ),
                )

                // Shadow
                val maskStop2 = topInset.toFloat() / size.height + 0.2f
                val maskBrush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color.Black.copy(alpha = 0.85f),
                        maskStop2 to Color.Black.copy(alpha = 0.2f),
                        1.0f to Color.Transparent,
                    )
                )

                // Random letters
                val textMargin = 8.dp.toPx()
                val textMeasurer = TextMeasurer(
                    fallbackFontFamilyResolver = fontFamilyResolver,
                    fallbackDensity = density,
                    fallbackLayoutDirection = layoutDirection,
                )
                val textStyle = TextStyle(
                    fontSize = 60.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 4.sp,
                )
                val chars = (currentServiceName ?: "")
                    .uppercase()
                    .toCharArray()
                    .toList()
                    .shuffled(Random(RandomSeed))
                val maxColorBlockCount = rows * columns / 2 + 1
                val blockChars = if (chars.size >= maxColorBlockCount) {
                    chars.subList(0, maxColorBlockCount)
                } else {
                    chars + List(maxColorBlockCount - chars.size) {
                        if (chars.isNotEmpty()) chars.random() else ' '
                    }
                }
                val blockTexts = blockChars.map { AnnotatedString(it.toString()) }

                // Random block order
                val showColorBlockFirst = RandomSeed % 2 == 0L

                val imageClipPath = Path()

                onDrawBehind {
                    // Redraw after tick is updated
                    redrawTick + 0

                    drawRect(brush = backgroundBrush)

                    var colorBlockIdx = 0
                    var iconBlockIdx = 0

                    for (i in 0 until rows) {
                        for (j in 0 until columns) {
                            val b = Block(row = i, col = j)
                            val scale = blockScales[b] ?: 1f

                            val idx = i * columns + j
                            val isEven = if (columns % 2 == 0) {
                                (idx + i) % 2 == 0
                            } else {
                                idx % 2 == 0
                            }
                            val displayBlockSize = blockSizePx * scale - blockSpacing
                            val top = blocksOffY + i * blockSizePx +
                                    blockSpacing / 2 +
                                    (blockSizePx - displayBlockSize) / 2
                            val left = blocksOffX + j * blockSizePx +
                                    blockSpacing / 2 +
                                    (blockSizePx - displayBlockSize) / 2
                            val topLeft = Offset(left, top)
                            val rectSize = Size(displayBlockSize, displayBlockSize)
                            if (isEven && showColorBlockFirst ||
                                !isEven && !showColorBlockFirst
                            ) {
                                // Draw the color block
                                drawRoundRect(
                                    color = blockColorAnim.value,
                                    topLeft = topLeft,
                                    size = rectSize,
                                    cornerRadius = blockCornerRadius,
                                )

                                val textLayoutResult = textMeasurer.measure(
                                    text = blockTexts[colorBlockIdx],
                                    style = textStyle,
                                )
                                val textSize = textLayoutResult.size
                                val rightMargin = textMargin * scale
                                val textTopLeft = Offset(
                                    topLeft.x + rectSize.width - textSize.width - rightMargin,
                                    topLeft.y + rectSize.height - textSize.height
                                )
                                // Draw the random letter
                                scale(
                                    scale = scale,
                                    pivot = Offset(
                                        textTopLeft.x + textSize.width / 2,
                                        textTopLeft.y + textSize.height / 2
                                    )
                                ) {
                                    drawText(
                                        textLayoutResult = textLayoutResult,
                                        topLeft = textTopLeft,
                                        color = onBlockColorAnim.value,
                                    )
                                }

                                // Draw the gradient overlay
                                drawRoundRect(
                                    brush = colorBlocksOverlayBrush,
                                    topLeft = topLeft,
                                    size = rectSize,
                                    cornerRadius = blockCornerRadius,
                                )

                                colorBlockIdx++
                            } else {
                                if (iconBlockIdx >= iconImages.size) continue
                                val image = iconImages[iconBlockIdx]
                                if (image == null) {
                                    iconBlockIdx++
                                    continue
                                }
                                val imageSize = min(image.width, image.height)
                                val srcSize = IntSize(imageSize, imageSize)
                                val srcOffset = Alignment.Center.align(
                                    size = srcSize,
                                    space = IntSize(image.width, image.height),
                                    layoutDirection = LayoutDirection.Ltr,
                                )

                                imageClipPath.reset()
                                imageClipPath.addRoundRect(
                                    RoundRect(
                                        left = topLeft.x,
                                        top = topLeft.y,
                                        right = topLeft.x + rectSize.width,
                                        bottom = topLeft.y + rectSize.height,
                                        cornerRadius = blockCornerRadius,
                                    )
                                )

                                clipPath(path = imageClipPath) {
                                    // Draw icon
                                    drawImage(
                                        image = image,
                                        srcSize = srcSize,
                                        srcOffset = srcOffset,
                                        dstSize = IntSize(
                                            rectSize.width.toInt(),
                                            rectSize.height.toInt()
                                        ),
                                        dstOffset = IntOffset(
                                            topLeft.x.toInt(),
                                            topLeft.y.toInt(),
                                        ),
                                        colorFilter = iconColorFilter,
                                    )
                                }

                                iconBlockIdx++
                            }
                        }
                    }

                    // Draw the mask
                    drawRect(brush = maskBrush)
                }
            },
    )
}
