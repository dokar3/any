@file:Suppress("deprecation")

package any.ui.imagepager

import any.base.R as BaseR
import android.annotation.SuppressLint
import android.app.DialogFragment
import android.app.ProgressDialog
import android.graphics.ColorFilter
import android.graphics.PointF
import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.PopupMenu
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.forEach
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import any.base.image.ImageLoader
import any.base.image.ImageRequest
import any.base.image.ImageResult
import any.base.image.PostImageSaver
import any.base.image.SubsamplingImageCache
import any.base.image.toFrescoRequestBuilder
import any.base.util.ClipboardUtil
import any.base.util.Intents
import any.base.util.PackageUtil
import any.base.util.getActivity
import any.base.util.hideBars
import any.base.util.showBars
import any.download.PostImageDownloader
import any.ui.common.image.rememberImageColorFilter
import any.ui.imagepager.databinding.FragmentImagePagerBinding
import any.ui.imagepager.databinding.ItemImagePageBinding
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.OnImageEventListener
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imageformat.DefaultImageFormats
import com.facebook.imageformat.ImageFormatChecker
import com.facebook.imagepipeline.image.ImageInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.saket.flick.ContentSizeProvider2
import me.saket.flick.FlickCallbacks
import me.saket.flick.FlickGestureListener
import java.io.File
import java.io.InputStream
import kotlin.math.absoluteValue

private const val FRAGMENT_TAG = "ImagePager"

@Composable
fun FragmentImagePager(
    onBack: () -> Unit,
    currentIndexUpdater: (Int) -> Unit,
    title: String?,
    images: List<String>,
    initialPage: Int = 0,
) {
    val context = LocalContext.current

    val activity = context.getActivity()

    val fragmentManager = activity.fragmentManager

    val fragment = remember(fragmentManager) {
        val curr = fragmentManager.findFragmentByTag(FRAGMENT_TAG)
        if (curr != null) {
            (curr as DialogFragment).dismiss()
        }
        ImagePagerFragment()
    }

    var currentPage: Int by rememberSaveable(
        inputs = arrayOf(initialPage),
    ) {
        mutableStateOf(initialPage)
    }

    val onPageChangeListener = remember {
        object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                currentPage = position
            }
        }
    }

    val colorFilter = rememberImageColorFilter()

    LaunchedEffect(fragment, title) {
        fragment.title = title ?: ""
    }

    LaunchedEffect(fragment, images) {
        fragment.images = images
    }

    LaunchedEffect(fragment, colorFilter) {
        fragment.imageColorFilter = colorFilter?.asAndroidColorFilter()
    }

    DisposableEffect(fragment) {
        fragment.onBack = {
            onBack()
            currentIndexUpdater(currentPage)
        }
        fragment.onTranslucentBackground = {
            currentIndexUpdater(currentPage)
        }
        fragment.initialPage = currentPage
        fragment.addOnPageChangeListener(onPageChangeListener)
        fragment.show(fragmentManager, FRAGMENT_TAG)
        onDispose {
            fragment.onBack = null
            fragment.onTranslucentBackground = null
            fragment.removeOnPageChangeListener(onPageChangeListener)
            try {
                fragment.dismiss()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }
    }
}

class ImagePagerFragment : DialogFragment() {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var _binding: FragmentImagePagerBinding? = null
    private val binding get() = _binding!!

    private var onPageChangeListeners = mutableListOf<ViewPager.OnPageChangeListener>()

    private var isTitleBarVisible = true

    private lateinit var optionsMenu: PopupMenu

    private val currentPage: Int
        get() = _binding?.imagePager?.currentItem ?: 0

    internal var title: String? = null
        set(value) {
            field = value
            updateTitle()
        }

    private var loadedImageIndices = booleanArrayOf()

    internal var images: List<String> = emptyList()
        set(value) {
            field = value
            loadedImageIndices = BooleanArray(value.size)
            _binding?.imagePager?.adapter?.notifyDataSetChanged()
        }

    internal var initialPage: Int = 0
        set(value) {
            field = value
            _binding?.imagePager?.currentItem = field
        }

    internal var onBack: (() -> Unit)? = null

    internal var onTranslucentBackground: (() -> Unit)? = null

    internal var imageColorFilter: ColorFilter? = null
        set(value) {
            field = value
            updateImageColorFilter()
        }

    private val subsamplingImageCache: SubsamplingImageCache? by lazy {
        val context = activity
        if (context != null) {
            SubsamplingImageCache.get(context)
        } else {
            null
        }
    }

    private val pagerImageTargets = mutableMapOf<Int, MultiOnTouchPhotoView>()

    private val imageLoadingJobs = mutableMapOf<Int, Job>()

    @Deprecated("Deprecated in Java")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.FullScreenDialog)
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateView(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImagePagerBinding.inflate(inflater!!, container, false)
        initImagePager()
        initTitleBar()
        return binding.root
    }

    @Deprecated("Deprecated in Java")
    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(dialog.window!!, false)

        val gestureHandler = object : ImageGestureHandler {
            override fun onLeftTap(): Boolean {
                return if (currentPage > 0) {
                    binding.imagePager.currentItem = currentPage - 1
                    true
                } else {
                    false
                }
            }

            override fun onCenterTap(): Boolean {
                if (isTitleBarVisible) {
                    hideUiBars()
                } else {
                    showUiBars()
                }
                return true
            }

            override fun onRightTap(): Boolean {
                return if (currentPage < images.size - 1) {
                    binding.imagePager.currentItem = currentPage + 1
                    true
                } else {
                    false
                }
            }
        }
        binding.imagePager.adapter = object : PagerAdapter() {
            override fun getCount(): Int {
                return images.size
            }

            override fun isViewFromObject(view: View, `object`: Any): Boolean {
                return view == `object`
            }

            override fun instantiateItem(container: ViewGroup, position: Int): Any {
                val itemView = createImagePage(
                    position = position,
                    gestureHandler = gestureHandler,
                )
                container.addView(itemView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                return itemView
            }

            override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
                val itemView = `object` as View
                container.removeView(itemView)
                itemView.findViewById<MultiOnTouchPhotoView>(R.id.photo_preview).recycle()
                itemView.findViewById<MultiOnTouchPhotoView>(R.id.photo_view).recycle()
                pagerImageTargets.remove(position)
                imageLoadingJobs[position]?.cancel()
                ImageLoader.detachRequest(ImageRequest.Downloadable(images[position]))
            }
        }
        binding.imagePager.currentItem = initialPage
        addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                updateTitle()
            }
        })

        binding.root.doOnDispatchWindowInsets {
            updateWindowInsets(it.top, it.bottom)
        }

        updateTitle()
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_item_save -> saveImage()
            R.id.menu_item_copy_url -> {
                ClipboardUtil.copyText(activity, images[currentPage])
                Toast.makeText(activity, BaseR.string.url_copied, Toast.LENGTH_SHORT).show()
            }

            R.id.menu_item_glen -> shareImage(PackageUtil.PKG_GOOGLE_LENS)
            R.id.menu_item_share -> shareImage()
        }
        return super.onOptionsItemSelected(item)
    }

    @Deprecated("Deprecated in Java")
    override fun onDestroyView() {
        super.onDestroyView()
        coroutineScope.launch(Dispatchers.IO) {
            subsamplingImageCache?.clear()
            coroutineScope.cancel()
        }

        for (listener in onPageChangeListeners) {
            binding.imagePager.removeOnPageChangeListener(listener)
        }
        onPageChangeListeners.clear()

        pagerImageTargets.clear()

        imageLoadingJobs.forEach { it.value.cancel() }
        imageLoadingJobs.clear()

        onBack?.invoke()
    }

    private fun initImagePager() {
        binding.imagePager.apply {
            onPageChangeListeners.forEach {
                addOnPageChangeListener(it)
            }
        }
    }

    private fun initTitleBar() {
        binding.btnBack.setOnClickListener {
            onBack?.invoke()
        }
        binding.btnMore.apply {
            setOnClickListener {
                optionsMenu.show()
            }
            post {
                initOptionsMenu(this)
                setOnTouchListener(optionsMenu.dragToOpenListener)
            }
        }
    }

    private fun initOptionsMenu(v: View) {
        optionsMenu = object : PopupMenu(
            activity,
            v,
            Gravity.NO_GRAVITY,
            android.R.attr.actionOverflowMenuStyle,
            0
        ) {
            init {
                inflate(R.menu.image_pager)
                setOnMenuItemClickListener(::onOptionsItemSelected)
            }

            override fun show() {
                prepareOptionsMenu()
                super.show()
            }
        }
        val isGoogleLensInstalled = PackageUtil.isPackageInstalled(
            packageName = PackageUtil.PKG_GOOGLE_LENS,
            context = activity,
        )
        if (isGoogleLensInstalled) {
            optionsMenu.menu.findItem(R.id.menu_item_glen).isVisible = true
        }
    }

    private fun prepareOptionsMenu() {
        val isImageLoaded = loadedImageIndices[currentPage]
        optionsMenu.menu.findItem(R.id.menu_item_save)?.isEnabled = isImageLoaded
    }

    private fun showUiBars() {
        dialog.window?.decorView?.showBars(dialog.window!!)
        _binding?.titleBar?.let {
            it.visibility = View.VISIBLE
            it.animate().cancel()
            it.animate()
                .alpha(1f)
                .setDuration(200)
                .withEndAction(null)
                .start()
        }
        isTitleBarVisible = true
    }

    private fun hideUiBars() {
        dialog.window?.decorView?.hideBars(dialog.window!!)
        _binding?.titleBar?.let {
            it.animate().cancel()
            it.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction { it.visibility = View.GONE }
                .start()
        }
        isTitleBarVisible = false
    }

    @SuppressLint("SetTextI18n")
    private fun updateTitle() {
        _binding?.tvTitle?.text = "[${currentPage + 1} / ${images.size}] $title"
    }

    private fun updateImageColorFilter() {
        val pager = _binding?.imagePager ?: return
        pager.forEach {
            val preview: SubsamplingScaleImageView = it.findViewById(R.id.photo_preview)
            preview.setColorFilter(imageColorFilter)
            val photoView: SubsamplingScaleImageView = it.findViewById(R.id.photo_view)
            photoView.setColorFilter(imageColorFilter)
        }
    }

    private fun saveImage() {
        val progressDialog = ProgressDialog(activity).apply {
            setMessage(resources.getString(BaseR.string.saving))
            setCanceledOnTouchOutside(false)
        }
        val saveJob = coroutineScope.launch {
            val start = System.currentTimeMillis()

            val result = PostImageSaver.saveToPicturesDir(
                imageFetcher = PostImageDownloader.get(activity),
                postTitle = title,
                imageIndex = currentPage,
                url = images[currentPage],
            )

            val end = System.currentTimeMillis()
            if (end - start < 300L) {
                delay(300 - (end - start))
            }


            if (!isActive) {
                return@launch
            }
            when {
                result.isSuccess -> {
                    Toast.makeText(activity, BaseR.string.image_saved, Toast.LENGTH_SHORT).show()
                }

                result.isFailure -> {
                    Log.e(TAG, "Failed to save image: ${result.exceptionOrNull()?.message}")
                    Toast.makeText(activity, BaseR.string.failed_to_save, Toast.LENGTH_SHORT).show()
                }
            }
        }.also {
            it.invokeOnCompletion {
                progressDialog.dismiss()
            }
        }
        progressDialog.setOnDismissListener {
            if (saveJob.isActive) {
                saveJob.cancel()
            }
        }
        progressDialog.show()
    }

    private fun shareImage(targetPackage: String? = null) {
        val progressDialog = ProgressDialog(activity).apply {
            setMessage(resources.getString(BaseR.string.preparing_to_share))
            setCanceledOnTouchOutside(false)
        }
        val shareImageJob = coroutineScope.launch {
            val showSharingDialog = launch {
                delay(100)
                progressDialog.show()
            }
            Intents.shareImage(
                context = activity,
                imageFetcher = PostImageDownloader.get(activity),
                url = images[currentPage],
                packageName = targetPackage,
            )
            showSharingDialog.cancel()
        }.also {
            it.invokeOnCompletion {
                progressDialog.dismiss()
            }
        }
        progressDialog.setOnDismissListener {
            shareImageJob.cancel()
        }
    }

    fun addOnPageChangeListener(listener: ViewPager.OnPageChangeListener) {
        val pager = _binding?.imagePager
        if (pager != null) {
            pager.addOnPageChangeListener(listener)
        } else {
            onPageChangeListeners.add(listener)
        }
    }

    fun removeOnPageChangeListener(listener: ViewPager.OnPageChangeListener) {
        val pager = _binding?.imagePager
        if (pager != null) {
            pager.removeOnPageChangeListener(listener)
        } else {
            onPageChangeListeners.remove(listener)
        }
    }

    private fun updateWindowInsets(top: Int, bottom: Int) {
        if (_binding != null) {
            (binding.titleBar.layoutParams as ViewGroup.MarginLayoutParams).topMargin = top
        }
    }

    private fun createImagePage(
        position: Int,
        gestureHandler: ImageGestureHandler,
    ): View {
        val binding = ItemImagePageBinding.inflate(LayoutInflater.from(activity))

        fun showLoading() {
            if (!isAdded) return
            binding.pbLoading.visibility = View.VISIBLE
        }

        fun hideLoading() {
            if (!isAdded) return
            binding.pbLoading.visibility = View.GONE
        }

        fun showError(e: Throwable?) {
            if (!isAdded) return
            binding.loadFailedContainer.visibility = View.VISIBLE
            binding.tvErrorMessage.text = resources.getString(
                BaseR.string._failed_to_load_with_message,
                e?.message ?: ""
            )
        }

        fun hideError() {
            if (!isAdded) return
            binding.loadFailedContainer.visibility = View.GONE
        }

        val flickCallbacks = object : FlickCallbacks {
            override fun onFlickDismiss(flickAnimationDuration: Long) {
                onBack?.invoke()
            }

            override fun onMove(moveRatio: Float) {
                updateBackground(moveRatio = moveRatio)
            }
        }

        val contentSizeProvider = ContentSizeProvider2 { binding.photoView.height }
        binding.flickDismissLayout.gestureListener =
            FlickGestureListener(activity, contentSizeProvider, flickCallbacks)

        val preview = binding.photoPreview
        setupSubsamplingScale(preview, gestureHandler)

        val photoView = binding.photoView
        setupSubsamplingScale(photoView, gestureHandler)
        photoView.setOnImageEventListener(object : OnImageEventListener {
            override fun onReady() {
                updatePreview(photoView) { preview ->
                    preview.visibility = View.GONE
                    preview.recycle()
                }
                updateScaleAndAlignment(photoView)
            }

            override fun onImageLoaded() {}

            override fun onPreviewLoadError(e: java.lang.Exception?) {}

            override fun onImageLoadError(e: java.lang.Exception?) {}

            override fun onTileLoadError(e: java.lang.Exception?) {}

            override fun onPreviewReleased() {}
        })

        preview.setColorFilter(imageColorFilter)
        photoView.setColorFilter(imageColorFilter)

        fun loadImage() {
            hideError()
            showLoading()
            loadImage(
                onRequestShowError = ::showError,
                onRequestHideLoading = ::hideLoading,
                photoView = photoView,
                position = position,
            )
        }
        loadImage()

        binding.btnReload.setOnClickListener { loadImage() }

        return binding.root
    }


    private fun loadImage(
        onRequestShowError: (Throwable?) -> Unit,
        onRequestHideLoading: () -> Unit,
        photoView: MultiOnTouchPhotoView,
        position: Int,
    ) {
        val url = images[position]

        pagerImageTargets[position] = photoView

        val currentJob = imageLoadingJobs[position]
        if (currentJob != null && currentJob.isActive) {
            currentJob.cancel()
        }

        val job = coroutineScope.launch {
            val request = ImageRequest.Downloadable(url = url)
            ImageLoader.fetchImage(request = request)
                .catch { onRequestShowError(it) }
                .collect { onImageResult(position, request, onRequestShowError, it) }
            imageLoadingJobs.remove(position)
            onRequestHideLoading()
        }
        imageLoadingJobs[position] = job
    }

    private suspend fun onImageResult(
        position: Int,
        request: ImageRequest.Downloadable,
        onRequestShowError: (Throwable?) -> Unit,
        result: ImageResult,
    ) = withContext(Dispatchers.IO) {
        val target = pagerImageTargets[position] ?: return@withContext
        if (activity == null) return@withContext

        suspend fun displayByDrawee() {
            withContext(Dispatchers.Main) {
                updateDraweeView(target) {
                    it.visibility = View.VISIBLE
                    it.controller = Fresco.newDraweeControllerBuilder()
                        .setImageRequest(request.toFrescoRequestBuilder().build())
                        .setAutoPlayAnimations(true)
                        .setControllerListener(object : BaseControllerListener<ImageInfo>() {
                            override fun onFailure(id: String?, throwable: Throwable?) {
                                onRequestShowError(throwable)
                            }

                            override fun onFinalImageSet(
                                id: String?,
                                imageInfo: ImageInfo?,
                                animatable: Animatable?
                            ) {
                                loadedImageIndices[position] = true
                            }
                        })
                        .build()
                    it.hierarchy.fadeDuration = 0
                }
                updatePreview(target) {
                    it.visibility = View.GONE
                }
            }
        }

        suspend fun onFile(file: File) {
            val format = ImageFormatChecker.getImageFormat(file.absolutePath)
            if (format == DefaultImageFormats.PNG || format == DefaultImageFormats.JPEG) {
                withContext(Dispatchers.Main) {
                    target.visibility = View.VISIBLE
                    target.setImage(ImageSource.uri(Uri.fromFile(file)))
                }
                loadedImageIndices[position] = true
            } else {
                // Not supported for subsampling scaling
                displayByDrawee()
            }
        }

        suspend fun onInputStream(inputStream: InputStream) {
            val format = ImageFormatChecker.getImageFormat(inputStream)
            if (format == DefaultImageFormats.PNG || format == DefaultImageFormats.JPEG) {
                val file = subsamplingImageCache?.put(request.url, inputStream)
                inputStream.close()
                if (file != null) {
                    withContext(Dispatchers.Main) {
                        target.visibility = View.VISIBLE
                        target.setImage(ImageSource.uri(Uri.fromFile(file)))
                    }
                    loadedImageIndices[position] = true
                } else {
                    val error = "Cannot write image to subsampling cache"
                    withContext(Dispatchers.Main) {
                        onRequestShowError(Exception(error))
                    }
                }
            } else {
                // Not supported for subsampling scaling
                displayByDrawee()
            }
        }

        when (result) {
            is ImageResult.Bitmap -> {
                withContext(Dispatchers.Main) {
                    updatePreview(target) { preview ->
                        preview.visibility = View.VISIBLE
                        preview.setImage(ImageSource.bitmap(result.value))
                    }
                }
            }

            is ImageResult.File -> {
                onFile(result.value)
            }

            is ImageResult.InputStream -> {
                onInputStream(result.value)
            }

            is ImageResult.Failure -> {
                withContext(Dispatchers.Main) {
                    onRequestShowError(result.error)
                }
            }
        }
    }

    private fun setupSubsamplingScale(
        view: MultiOnTouchPhotoView,
        gestureHandler: ImageGestureHandler,
    ) {
        view.apply {
            val doubleTapDuration = 225

            maxScale = 10f
            isQuickScaleEnabled = false
            setDoubleTapZoomDuration(doubleTapDuration)
            setDoubleTapZoomScale(3f)

            val toggleBarRunnable = Runnable {
                if (isTitleBarVisible) {
                    hideUiBars()
                } else {
                    showUiBars()
                }
            }

            val onGestureListener = object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    removeCallbacks(toggleBarRunnable)
                    postDelayed(toggleBarRunnable, doubleTapDuration.toLong())
                    return false
                }
            }
            val gestureDetector = GestureDetector(context, onGestureListener)

            var touchX = 0f
            var touchY = 0f
            addOnTouchListener { _, ev ->
                touchX = ev.x
                touchY = ev.y
                gestureDetector.onTouchEvent(ev)
                false
            }

            setOnClickListener {
                handleImageTapGesture(
                    gestureHandler = gestureHandler,
                    width = it.width,
                    x = touchX,
                    y = touchY,
                    onLeftTap = {},
                    onRightTap = {},
                )
            }
        }
    }

    private inline fun updatePreview(
        photoView: MultiOnTouchPhotoView,
        action: (preview: MultiOnTouchPhotoView) -> Unit,
    ) {
        val preview = (photoView.parent as ViewGroup)
            .findViewById<MultiOnTouchPhotoView>(R.id.photo_preview)
        action(preview)
    }

    private inline fun updateDraweeView(
        photoView: MultiOnTouchPhotoView,
        action: (preview: SimpleDraweeView) -> Unit
    ) {
        val draweeView = (photoView.parent as ViewGroup)
            .findViewById<SimpleDraweeView>(R.id.drawee_view)
        action(draweeView)
    }

    private fun updateBackground(moveRatio: Float) {
        val progress = moveRatio.absoluteValue.coerceIn(0f, 1f)
        val alpha = 1f - progress
        val backgroundColor = Color(0xff212121).copy(alpha = alpha)
        _binding?.imagePager?.setBackgroundColor(backgroundColor.toArgb())
        if (alpha < 1f) {
            onTranslucentBackground?.invoke()
        }
        _binding?.titleBar?.also {
            it.alpha = alpha
            it.translationY = -it.height * progress
        }
    }

    private fun updateScaleAndAlignment(photoView: MultiOnTouchPhotoView) {
        val viewWidth = photoView.measuredWidth
        val viewHeight = photoView.measuredHeight
        if (viewWidth == 0 || viewHeight == 0) {
            return
        }
        val viewAspectRatio = viewWidth.toFloat() / viewHeight
        val sourceWidth = photoView.sWidth
        val sourceHeight = photoView.sHeight
        if (sourceWidth == 0 || sourceHeight == 0) {
            return
        }
        val sourceAspectRatio = sourceWidth.toFloat() / sourceHeight
        // Make the image fill-width
        val scale = viewWidth.toFloat() / sourceWidth
        // Calculate the center point of the source image
        val center = if (sourceAspectRatio < viewAspectRatio) {
            // For long images, align to the top edge
            PointF(sourceWidth / 2f, 0f)
        } else {
            // Otherwise, align to the center
            PointF(sourceWidth / 2f, sourceHeight / 2f)
        }
        photoView.setScaleAndCenter(scale, center)
    }

    companion object {
        private const val TAG = "FragmentImagePager"

        private const val LEFT_TAP_THRESHOLD = 0.3

        private const val RIGHT_TAP_THRESHOLD = 0.7

        private inline fun handleImageTapGesture(
            gestureHandler: ImageGestureHandler,
            width: Int,
            x: Float,
            y: Float,
            onLeftTap: () -> Unit,
            onRightTap: () -> Unit,
        ) {
            when {
                x <= width * LEFT_TAP_THRESHOLD -> {
                    if (gestureHandler.onLeftTap()) {
                        onLeftTap()
                    }
                }

                x >= width * RIGHT_TAP_THRESHOLD -> {
                    if (gestureHandler.onRightTap()) {
                        onRightTap()
                    }
                }

                else -> {
                    gestureHandler.onCenterTap()
                }
            }
        }
    }
}

