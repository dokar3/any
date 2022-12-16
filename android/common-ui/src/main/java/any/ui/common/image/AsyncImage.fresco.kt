package any.ui.common.image

import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
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
import com.facebook.drawee.generic.GenericDraweeHierarchyInflater
import com.facebook.drawee.view.DraweeHolder
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.image.ImageInfo
import com.google.accompanist.drawablepainter.rememberDrawablePainter

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
) {
    var isLoaded by remember(request) { mutableStateOf(false) }

    val listenerWrapper = remember(listener) {
        object : ImageStateListener {
            override fun onLoading(size: IntSize?) {
                isLoaded = false
                listener?.onLoading(size)
            }

            override fun onSuccess(size: IntSize) {
                isLoaded = true
                listener?.onSuccess(size)
            }

            override fun onFailure(error: Throwable?) {
                listener?.onFailure(error)
            }
        }
    }

    Box(modifier = modifier) {
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
            reloadFactor = reloadFactor,
            fadeIn = fadeIn,
            showProgressbar = showProgressbar,
        )

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
    reloadFactor: Int? = null,
    fadeIn: Boolean = false,
    checkMemoryCacheFirst: Boolean = false,
    showProgressbar: Boolean = false,
) {
    val context = LocalContext.current

    var drawable: Drawable? by remember(request, checkMemoryCacheFirst) {
        val d = if (checkMemoryCacheFirst) {
            ImageLoader.fetchBitmapFromCache(request)?.toDrawable(context.resources)
        } else {
            null
        }
        mutableStateOf(d)
    }

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

    val holder = remember(request, reloadFactor) {
        val hierarchy = GenericDraweeHierarchyInflater.inflateBuilder(context, null)
            .setFadeDuration(if (fadeIn) 255 else 0)
            .setProgressBarImage(progressDrawable)
            .build()
        val holder = DraweeHolder.create(hierarchy, context)

        if (drawable != null && checkMemoryCacheFirst) {
            // Hit memory cache, skip
            return@remember holder
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
                listener?.onSuccess(imageInfo?.getIntSize() ?: IntSize.Zero)
            }

            override fun onIntermediateImageSet(id: String?, imageInfo: ImageInfo?) {
                listener?.onLoading(imageInfo?.getIntSize())
            }

            override fun onFailure(id: String?, throwable: Throwable?) {
                listener?.onFailure(throwable)
            }

            private fun ImageInfo.getIntSize(): IntSize {
                return IntSize(width, height)
            }
        }
        val updateDrawableListener = object : BaseControllerListener<ImageInfo>() {
            override fun onSubmit(id: String?, callerContext: Any?) {
                drawable = holder.topLevelDrawable
            }

            override fun onFinalImageSet(
                id: String?,
                imageInfo: ImageInfo?,
                animatable: Animatable?
            ) {
                drawable = holder.topLevelDrawable
            }

            override fun onIntermediateImageSet(id: String?, imageInfo: ImageInfo?) {
                drawable = holder.topLevelDrawable
            }
        }
        val controllerListener = ControllerListenerWrapper(
            listeners = listOf(imageStateListener, updateDrawableListener)
        )

        val resizeOptions = if (size != null && size.width > 0 && size.height > 0) {
            ResizeOptions(size.width, size.height)
        } else {
            null
        }

        val controllerBuilder = Fresco.newDraweeControllerBuilder()
            .let { controllerBuilder ->
                if (request is ImageRequest.Downloadable) {
                    val fetcher = PostImageDownloader.get(context)
                    val requests = request.frescoRequestBuilders(fetcher)
                        .map { it.setResizeOptions(resizeOptions).build() }
                        .toTypedArray()
                    if (requests.size == 1) {
                        controllerBuilder.setImageRequest(requests.first())
                    } else {
                        controllerBuilder.setFirstAvailableImageRequests(requests)
                    }
                } else {
                    val frescoRequest = request.toFrescoRequestBuilder()
                        .setResizeOptions(resizeOptions)
                        .build()
                    controllerBuilder.setImageRequest(frescoRequest)
                }
            }
            .setAutoPlayAnimations(true)
            .setOldController(holder.controller)
            .setControllerListener(controllerListener)

        holder.controller = controllerBuilder.build()

        holder
    }

    val frescoScaleType = rememberFrescoScaleType(alignment, contentScale)

    LaunchedEffect(holder, frescoScaleType) {
        holder.hierarchy.actualImageScaleType = frescoScaleType
    }

    LaunchedEffect(progressColor, secondaryProgressColor, layoutDirection) {
        progressDrawable?.let {
            it.progressColor = progressColor.toArgb()
            it.secondaryProgressColor = secondaryProgressColor.toArgb()
            it.layoutDirection = layoutDirection
            val progress = it.level / 10000f
            // Update progress drawable
            holder.hierarchy.setProgress(progress, true)
        }
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
        drawable?.let {
            val bounds = it.bounds
            val width = it.intrinsicWidth
            val height = it.intrinsicHeight
            if (bounds.width() != width || bounds.height() != height) {
                it.bounds = Rect(0, 0, width, height)
            }
        }
    }

    Image(
        painter = rememberDrawablePainter(drawable),
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