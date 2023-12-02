package any.ui.common.image

import android.content.Context
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
import androidx.core.graphics.drawable.toDrawable
import any.base.image.ImageLoader
import any.base.image.ImageRequest
import any.base.image.frescoRequestBuilders
import any.base.image.toFrescoRequestBuilder
import any.download.PostImageDownloader
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.controller.ControllerListener
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.drawee.drawable.ScalingUtils.AbstractScaleType
import com.facebook.drawee.generic.GenericDraweeHierarchy
import com.facebook.drawee.generic.GenericDraweeHierarchyInflater
import com.facebook.drawee.view.DraweeHolder
import com.facebook.fresco.middleware.HasExtraData
import com.facebook.imagepipeline.common.ImageDecodeOptions
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.image.ImageInfo
import com.facebook.imagepipeline.image.RegionDecoder
import com.facebook.imagepipeline.request.ImageRequestBuilder
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.math.min

private class ControllerListenerWrapper<T>(
    private val listeners: List<ControllerListener<T>>
) : ControllerListener<T> {
    override fun onSubmit(id: String?, callerContext: Any?) {
        listeners.forEach { it.onSubmit(id, callerContext) }
    }

    override fun onFinalImageSet(id: String?, imageInfo: T?, animatable: Animatable?) {
        listeners.forEach { it.onFinalImageSet(id, imageInfo, animatable) }
    }

    override fun onIntermediateImageSet(id: String?, imageInfo: T?) {
        listeners.forEach { it.onIntermediateImageSet(id, imageInfo) }
    }

    override fun onIntermediateImageFailed(id: String?, throwable: Throwable?) {
        listeners.forEach { it.onIntermediateImageFailed(id, throwable) }
    }

    override fun onFailure(id: String?, throwable: Throwable?) {
        listeners.forEach { it.onFailure(id, throwable) }
    }

    override fun onRelease(id: String?) {
        listeners.forEach { it.onRelease(id) }
    }
}

private data class RequestRegion(
    val decode: IntRect,
    val aspectRatio: Float,
    val requestSize: IntSize?,
)

private data class Regions(
    val aspectRatio: Float,
    val list: List<RequestRegion>,
)

@Composable
internal fun FrescoAsyncImage(
    request: ImageRequest,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    listener: ImageStateListener? = null,
    size: IntSize? = null,
    reloadFactor: Int? = null,
    placeholder: ImageRequest? = null,
    fadeIn: Boolean = false,
    showProgressbar: Boolean = false,
    regionDecodeThresholds: RegionDecodeThresholds = RegionDecodeThresholds.Disabled,
) {
    var sourceImageSize by remember(request) { mutableStateOf<IntSize?>(null) }

    var regions by remember(request) { mutableStateOf<Regions?>(null) }

    var isRegionsDisplayed by remember(request) { mutableStateOf(false) }

    var isLoaded by remember(request) { mutableStateOf(false) }

    val listenerWrapper = remember(listener) {
        object : ImageStateListener {
            override fun onLoading(size: IntSize?) {
                isLoaded = false
                listener?.onLoading(size)
            }

            override fun onSuccess(size: IntSize, originalSize: IntSize?) {
                isLoaded = true
                listener?.onSuccess(size, originalSize)
                sourceImageSize = originalSize
            }

            override fun onFailure(error: Throwable?) {
                listener?.onFailure(error)
            }
        }
    }

    /// Calculate regions for long images
    LaunchedEffect(request, regionDecodeThresholds, sourceImageSize, size) {
        val srcSize = sourceImageSize
        if (srcSize == null) {
            regions = null
            return@LaunchedEffect
        }
        val width = srcSize.width
        val height = srcSize.height
        val aspectRatio = width.toFloat() / height
        if (height < regionDecodeThresholds.minHeight ||
            aspectRatio > regionDecodeThresholds.maxAspectRatio
        ) {
            regions = null
            return@LaunchedEffect
        }
        // Calculate regions
        val maxHeightPerRegion = (width / regionDecodeThresholds.maxAspectRatio).toInt()
        val regionList = mutableListOf<RequestRegion>()
        var top = 0
        while (top < height) {
            val targetHeight = min(height - top, maxHeightPerRegion)
            val rect = IntRect(0, top, width, top + targetHeight)
            val requestSize = size?.run { IntSize(width, rect.height / (rect.width / width)) }
            regionList.add(
                RequestRegion(
                    decode = rect,
                    aspectRatio = rect.width.toFloat() / rect.height,
                    requestSize = requestSize,
                )
            )
            top += targetHeight
        }
        regions = Regions(aspectRatio, regionList)
    }

    Box(modifier = modifier) {
        val regionsVal = regions
        if (regionsVal != null) {
            LaunchedEffect(request) {
                isRegionsDisplayed = false
                awaitFrame()
                delay(100)
                isRegionsDisplayed = true
            }

            Column {
                for (region in regionsVal.list) {
                    FrescoAsyncImageImpl(
                        request = request,
                        contentDescription = contentDescription,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(region.aspectRatio),
                        alignment = alignment,
                        contentScale = ContentScale.FillWidth,
                        alpha = 1f,
                        colorFilter = colorFilter,
                        listener = null,
                        size = region.requestSize,
                        decodeRegion = region.decode,
                        reloadKey = reloadFactor,
                        fadeIn = false,
                        showProgressbar = false,
                    )
                }
            }
        }
    }

    if (regions == null || !isRegionsDisplayed) {
        FrescoAsyncImageImpl(
            request = request,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            alignment = alignment,
            contentScale = contentScale,
            alpha = alpha,
            colorFilter = colorFilter,
            listener = listenerWrapper,
            size = size,
            reloadKey = reloadFactor,
            fadeIn = fadeIn,
            showProgressbar = showProgressbar,
        )
    }

    if (!isLoaded && placeholder != null && placeholder != request) {
        // Show placeholder if image is not ready
        FrescoAsyncImageImpl(
            request = placeholder,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            alignment = alignment,
            contentScale = contentScale,
            alpha = alpha,
            colorFilter = colorFilter,
            size = size,
            checkMemoryCacheFirst = true,
        )
    }
}

@Composable
private fun FrescoAsyncImageImpl(
    request: ImageRequest,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    listener: ImageStateListener? = null,
    size: IntSize? = null,
    decodeRegion: IntRect? = null,
    reloadKey: Int? = null,
    fadeIn: Boolean = false,
    checkMemoryCacheFirst: Boolean = false,
    showProgressbar: Boolean = false,
) {
    val context = LocalContext.current

    val drawable = remember(request, checkMemoryCacheFirst, decodeRegion) {
        val d: Drawable? = if (checkMemoryCacheFirst && decodeRegion == null) {
            ImageLoader.fetchBitmapFromCache(request)?.toDrawable(context.resources)
        } else {
            null
        }
        mutableStateOf(d)
    }

    var updatedProgress by remember { mutableFloatStateOf(0f) }

    val progressDrawable = rememberProgressDrawable(
        onUpdateStyle = { updatedProgress = it },
        request = request,
        showProgressbar = showProgressbar,
    )

    val holder = remember(request, reloadKey) {
        frescoHolderOf(
            context = context,
            request = request,
            size = size,
            decodeRegion = decodeRegion,
            fadeIn = fadeIn,
            progressDrawable = progressDrawable,
            checkMemoryCacheFirst = checkMemoryCacheFirst,
            currDrawable = drawable,
            listener = listener,
        )
    }

    val frescoScaleType = rememberFrescoScaleType(alignment, contentScale)

    LaunchedEffect(holder, frescoScaleType) {
        holder.hierarchy.actualImageScaleType = frescoScaleType
    }

    LaunchedEffect(updatedProgress) {
        // Update progress drawable
        holder.hierarchy.setProgress(updatedProgress, true)
    }

    DisposableEffect(holder) {
        holder.onVisibilityChange(true)
        holder.onAttach()
        onDispose {
            holder.onVisibilityChange(false)
            holder.onDetach()
        }
    }

    SideEffect {
        drawable.value?.let {
            val bounds = it.bounds
            val width = it.intrinsicWidth
            val height = it.intrinsicHeight
            if (bounds.width() != width || bounds.height() != height) {
                it.bounds = Rect(0, 0, width, height)
            }
        }
    }

    Image(
        painter = rememberDrawablePainter(drawable.value),
        contentDescription = contentDescription,
        modifier = modifier,
        alpha = alpha,
        colorFilter = colorFilter,
        alignment = alignment,
        contentScale = contentScale,
    )
}

@Composable
private fun rememberFrescoScaleType(
    alignment: Alignment,
    contentScale: ContentScale,
): ScalingUtils.ScaleType {
    val layoutDirection = LocalLayoutDirection.current
    return remember(alignment, contentScale, layoutDirection) {
        createFrescoScaleType(alignment, contentScale, layoutDirection)
    }
}

@Composable
private fun rememberProgressDrawable(
    onUpdateStyle: (currProgress: Float) -> Unit,
    request: ImageRequest,
    showProgressbar: Boolean,
): Drawable? {
    val density = LocalDensity.current
    val progressColor = MaterialTheme.colors.primary
    val secondaryProgressColor = MaterialTheme.colors.secondary.copy(alpha = 0.5f)
    val progressWidth = with(density) { 3.dp.toPx() }
    val progressRadius = with(density) { 12.dp.toPx() }
    val progressPadding = with(density) { 16.dp.toPx() }
    val layoutDirection = LocalLayoutDirection.current

    val progressDrawable = remember(request, showProgressbar) {
        if (showProgressbar) {
            CircleProgressDrawable().also {
                it.progressColor = progressColor.toArgb()
                it.secondaryProgressColor = secondaryProgressColor.toArgb()
                it.padding = progressPadding
                it.radius = progressRadius
                it.barWidth = progressWidth
                it.layoutDirection = layoutDirection
            }
        } else {
            null
        }
    }

    LaunchedEffect(progressColor, secondaryProgressColor, layoutDirection, onUpdateStyle) {
        progressDrawable?.let {
            it.progressColor = progressColor.toArgb()
            it.secondaryProgressColor = secondaryProgressColor.toArgb()
            it.layoutDirection = layoutDirection
            val progress = it.level / CircleProgressDrawable.MAX_LEVEL.toFloat()
            onUpdateStyle(progress)
        }
    }

    return progressDrawable
}

private fun createFrescoScaleType(
    alignment: Alignment,
    contentScale: ContentScale,
    layoutDirection: LayoutDirection,
): ScalingUtils.ScaleType {
    return object : AbstractScaleType() {
        override fun getTransformImpl(
            outTransform: Matrix,
            parentRect: Rect,
            childWidth: Int,
            childHeight: Int,
            focusX: Float,
            focusY: Float,
            scaleX: Float,
            scaleY: Float
        ) {
            val parentSize = IntSize(parentRect.width(), parentRect.height())
            if (parentSize.width <= 0 || parentSize.height <= 0) {
                return
            }

            val childSize = IntSize(childWidth, childHeight)
            val scaleFactor = contentScale.computeScaleFactor(
                srcSize = childSize.toSize(),
                dstSize = parentSize.toSize(),
            )
            outTransform.setScale(scaleFactor.scaleX, scaleFactor.scaleY)

            val scaledChildSize = IntSize(
                width = (childWidth * scaleFactor.scaleX).toInt(),
                height = (childHeight * scaleFactor.scaleY).toInt()
            )
            val offset = alignment.align(
                size = scaledChildSize,
                space = parentSize,
                layoutDirection = layoutDirection
            )
            outTransform.postTranslate(offset.x.toFloat(), offset.y.toFloat())
        }
    }
}

private fun frescoHolderOf(
    context: Context,
    request: ImageRequest,
    size: IntSize?,
    decodeRegion: IntRect?,
    fadeIn: Boolean,
    progressDrawable: Drawable?,
    currDrawable: MutableState<Drawable?>,
    checkMemoryCacheFirst: Boolean,
    listener: ImageStateListener?,
): DraweeHolder<GenericDraweeHierarchy> {
    val hierarchy = GenericDraweeHierarchyInflater.inflateBuilder(context, null)
        .setFadeDuration(if (fadeIn) 255 else 0)
        .setProgressBarImage(progressDrawable)
        .build()
    val holder = DraweeHolder.create(hierarchy, context)

    val currVale = currDrawable.value
    if (checkMemoryCacheFirst &&
        currVale is RegionDrawable &&
        currVale.region == decodeRegion
    ) {
        // Hit memory cache, skip
        return holder
    }

    val imageStateListener = object : BaseControllerListener<ImageInfo>() {
        override fun onSubmit(id: String?, callerContext: Any?) {
            listener?.onLoading(null)
        }

        override fun onFinalImageSet(
            id: String?,
            imageInfo: ImageInfo?,
            animatable: Animatable?
        ) {
            listener?.onSuccess(
                imageInfo?.getIntSize() ?: IntSize.Zero,
                imageInfo?.originalSize()
            )
        }

        override fun onIntermediateImageSet(id: String?, imageInfo: ImageInfo?) {
            listener?.onLoading(imageInfo?.getIntSize())
        }

        override fun onFailure(id: String?, throwable: Throwable?) {
            listener?.onFailure(throwable)
        }

    }
    val updateDrawableListener = object : BaseControllerListener<ImageInfo>() {
        override fun onSubmit(id: String?, callerContext: Any?) {
            currDrawable.value = RegionDrawable.of(holder.topLevelDrawable, decodeRegion)
        }

        override fun onFinalImageSet(
            id: String?,
            imageInfo: ImageInfo?,
            animatable: Animatable?
        ) {
            currDrawable.value = RegionDrawable.of(holder.topLevelDrawable, decodeRegion)
        }

        override fun onIntermediateImageSet(id: String?, imageInfo: ImageInfo?) {
            currDrawable.value = RegionDrawable.of(holder.topLevelDrawable, decodeRegion)
        }
    }
    val controllerListener = ControllerListenerWrapper(
        listeners = listOf(imageStateListener, updateDrawableListener)
    )

    val resizeOptions = if (decodeRegion != null) {
        val resize = if (size.isNonZero()) size else decodeRegion.size
        // Disable down-sampling
        ResizeOptions(resize.width, resize.height, maxBitmapSize = 20_000f)
    } else if (size.isNonZero()) {
        ResizeOptions(size.width, size.height)
    } else {
        null
    }

    val controllerBuilder = Fresco.newDraweeControllerBuilder()
        .let { controllerBuilder ->
            if (request is ImageRequest.Downloadable) {
                val fetcher = PostImageDownloader.get(context)
                val requests = request.frescoRequestBuilders(fetcher)
                    .map {
                        it.setResizeOptions(resizeOptions)
                            .setImageRegionDecode(decodeRegion)
                            .build()
                    }
                    .toTypedArray()
                if (requests.size == 1) {
                    controllerBuilder.setImageRequest(requests.first())
                } else {
                    controllerBuilder.setFirstAvailableImageRequests(requests)
                }
            } else {
                val frescoRequest = request.toFrescoRequestBuilder()
                    .setResizeOptions(resizeOptions)
                    .setImageRegionDecode(decodeRegion)
                    .build()
                controllerBuilder.setImageRequest(frescoRequest)
            }
        }
        .setAutoPlayAnimations(true)
        .setOldController(holder.controller)
        .setControllerListener(controllerListener)

    holder.controller = controllerBuilder.build()

    return holder
}

private fun ImageRequestBuilder.setImageRegionDecode(region: IntRect?): ImageRequestBuilder {
    region ?: return this
    return this.setImageDecodeOptions(
        ImageDecodeOptions.newBuilder()
            .setCustomImageDecoder(RegionDecoder.create(region))
            .build()
    )
}

private fun ImageInfo.getIntSize(): IntSize {
    return IntSize(width, height)
}

private fun ImageInfo.originalSize(): IntSize? {
    val width = extras[HasExtraData.KEY_ENCODED_WIDTH] as? Int ?: return null
    val height = extras[HasExtraData.KEY_ENCODED_HEIGHT] as? Int ?: return null
    return IntSize(width, height)
}

@OptIn(ExperimentalContracts::class)
private fun <T : IntSize?> T.isNonZero(): Boolean {
    contract { returns(true) implies (this@isNonZero != null) }
    this ?: return false
    return width > 0 && height > 0
}