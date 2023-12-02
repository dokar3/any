package any.ui.common.image

import android.util.Size
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import any.base.image.ImageLoader
import any.base.image.ImageRequest
import any.base.image.ImageState
import any.base.prefs.darkenedImages
import any.base.prefs.monochromeImages
import any.base.prefs.preferencesStore
import any.base.prefs.transparentImages
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine

@Stable
interface ImageStateListener {
    fun onLoading(size: IntSize?)

    fun onSuccess(size: IntSize, originalSize: IntSize?)

    fun onFailure(error: Throwable?)
}

@Stable
internal open class OnImageStateListener(
    var onState: ((ImageState) -> Unit)? = null
) : ImageStateListener {
    override fun onLoading(size: IntSize?) {
        onState?.invoke(ImageState.Loading(size))
    }

    override fun onSuccess(size: IntSize, originalSize: IntSize?) {
        onState?.invoke(ImageState.Success(size))
    }

    override fun onFailure(error: Throwable?) {
        onState?.invoke(ImageState.Failure(error))
    }
}

/**
 * Image that load image synchronously.
 */
@Composable
fun AsyncImage(
    request: ImageRequest,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = rememberImageColorFilter(),
    onState: ((ImageState) -> Unit)? = null,
    reloadKey: Int? = null,
    placeholder: ImageRequest? = null,
    fadeIn: Boolean = false,
    showProgressbar: Boolean = false,
    restrictRequestSizeToContainer: Boolean = true,
    autoFreeRequest: Boolean = true,
    regionDecodeThresholds: RegionDecodeThresholds = RegionDecodeThresholds.Disabled,
) {
    val density = LocalDensity.current

    val imageSizeState = remember(request) { mutableStateOf(IntSize.Zero) }
    val layoutSizeState = remember(request) { mutableStateOf(IntSize.Zero) }

    var autoWidth by remember(request) { mutableIntStateOf(0) }
    var autoHeight by remember(request) { mutableIntStateOf(0) }

    fun autoSizing() {
        val imageSize = imageSizeState.value
        if (imageSize.width <= 0 || imageSize.height <= 0) {
            return
        }
        val ratio = imageSize.width.toFloat() / imageSize.height
        val layoutSize = layoutSizeState.value
        if (layoutSize.width == 0 && layoutSize.height > 0) {
            autoWidth = (layoutSize.height * ratio).toInt()
        } else if (layoutSize.width > 0 && layoutSize.height == 0) {
            autoHeight = (layoutSize.width / ratio).toInt()
        }
    }

    val listener = remember(request, onState, layoutSizeState) {
        object : OnImageStateListener(onState) {
            override fun onSuccess(size: IntSize, originalSize: IntSize?) {
                super.onSuccess(size, originalSize)
                imageSizeState.value = size
                autoSizing()
            }
        }
    }

    val sizeModifier = remember(autoWidth, autoHeight) {
        Modifier
            .let {
                if (autoWidth > 0) {
                    it.width(with(density) { autoWidth.toDp() })
                } else {
                    it
                }
            }
            .let {
                if (autoHeight > 0) {
                    it.height(with(density) { autoHeight.toDp() })
                } else {
                    it
                }
            }
    }

    SideEffect {
        listener.onState = onState
    }

    if (restrictRequestSizeToContainer) {
        BoxWithConstraints(modifier = modifier) {
            check(
                constraints.maxWidth != Constraints.Infinity ||
                        constraints.maxHeight != Constraints.Infinity
            ) {
                "Illegal constraints for AsyncImage(), both width and height " +
                        "are Constraints.Infinite"
            }

            val requestSize = IntSize(constraints.maxWidth, constraints.maxHeight)

            if (autoFreeRequest) {
                DisposableEffect(request) {
                    val androidUtilSize = Size(requestSize.width, requestSize.height)
                    ImageLoader.attachRequest(request, androidUtilSize)
                    onDispose {
                        ImageLoader.detachRequest(request)
                    }
                }
            }

            FrescoAsyncImage(
                request = request,
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged {
                        layoutSizeState.value = it
                        autoSizing()
                    }
                    .then(sizeModifier),
                alignment = alignment,
                contentScale = contentScale,
                alpha = alpha,
                colorFilter = colorFilter,
                listener = listener,
                size = requestSize,
                reloadFactor = reloadKey,
                placeholder = placeholder,
                fadeIn = fadeIn,
                showProgressbar = showProgressbar,
                regionDecodeThresholds = regionDecodeThresholds,
            )
        }
    } else {
        if (autoFreeRequest) {
            DisposableEffect(request) {
                ImageLoader.attachRequest(request)
                onDispose {
                    ImageLoader.detachRequest(request)
                }
            }
        }

        FrescoAsyncImage(
            request = request,
            contentDescription = contentDescription,
            modifier = modifier
                .fillMaxSize()
                .onSizeChanged {
                    layoutSizeState.value = it
                    autoSizing()
                }
                .then(sizeModifier),
            alignment = alignment,
            contentScale = contentScale,
            alpha = alpha,
            colorFilter = colorFilter,
            listener = listener,
            reloadFactor = reloadKey,
            placeholder = placeholder,
            fadeIn = fadeIn,
            showProgressbar = showProgressbar,
            regionDecodeThresholds = regionDecodeThresholds,
        )
    }
}

@Composable
fun rememberImageColorFilter(): ColorFilter? {
    var colorFilter by remember { mutableStateOf<ColorFilter?>(null) }

    val context = LocalContext.current

    LaunchedEffect(context) {
        val prefStore = context.preferencesStore()
        combine(
            prefStore.darkenedImages.asFlow(),
            prefStore.monochromeImages.asFlow(),
            prefStore.transparentImages.asFlow(),
        ) { darkenImages, monoImages, transparentImages ->
            colorFilter = if (darkenImages || monoImages || transparentImages) {
                val colorMatrix = ColorMatrixHelper.get(
                    Filters(
                        darkenImages = darkenImages,
                        monoImages = monoImages,
                        transparentImages = transparentImages,
                    )
                )
                ColorFilter.colorMatrix(colorMatrix)
            } else {
                null
            }
        }.collect()
    }

    return colorFilter
}

private data class Filters(
    val darkenImages: Boolean,
    val monoImages: Boolean,
    val transparentImages: Boolean,
)

private object ColorMatrixHelper {
    private val cache = mutableMapOf<Filters, ColorMatrix>()

    fun get(filters: Filters): ColorMatrix {
        return cache.getOrPut(filters) {
            // https://developer.android.com/reference/android/graphics/ColorMatrix
            ColorMatrix().also {
                if (filters.transparentImages) {
                    // Transparent
                    it.values.fill(0f)
                    return@also
                }
                // Grayscale
                it.setToSaturation(if (filters.monoImages) 0f else 1f)
                // Brightness
                val brightness = if (filters.darkenImages) 0.7f else 1f
                val add = -((1f - brightness) * 255)
                it[0, 4] = add
                it[1, 4] = add
                it[2, 4] = add
            }
        }
    }
}

