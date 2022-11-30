package any.ui.floatingbubble

import android.animation.Animator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.res.Configuration
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.core.EaseOutQuint
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.animation.addListener
import androidx.core.content.getSystemService
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import any.ui.common.R
import kotlin.math.abs

class FloatingView(private val context: Context) : LifecycleOwner, SavedStateRegistryOwner {
    private lateinit var actualContext: Context

    private lateinit var windowManager: WindowManager

    private var fabContainer: View? = null
    private var fab: View? = null

    private var touchableFab: View? = null
    private var touchableFabParams: WindowManager.LayoutParams? = null

    private var contentView: View? = null
    private var contentViewParams: WindowManager.LayoutParams? = null

    private var contentAnim: SpringAnimation? = null
    private var contentDimAnim: Animator? = null

    private lateinit var lifecycleRegistry: LifecycleRegistry
    private lateinit var savedStateRegistryController: SavedStateRegistryController

    private var onDismiss: (() -> Unit)? = null

    private var currentFabOffset = Offset(0f, 0f)

    private var isDismissed = true

    private var arrowColor: @Composable (() -> Color)? = null
    private var bubble: @Composable () -> Unit = { BubbleFab {} }
    private var expandedContent: @Composable () -> Unit = {}

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }

    fun setContent(
        arrowColor: @Composable (() -> Color)? = this.arrowColor,
        bubble: @Composable () -> Unit = this.bubble,
        expandedContent: @Composable () -> Unit,
    ) {
        this.arrowColor = arrowColor
        this.bubble = bubble
        this.expandedContent = expandedContent
    }

    fun show(
        position: SnapPosition = SnapPosition.endAligned(0.3f),
    ) {
        if (::lifecycleRegistry.isInitialized &&
            lifecycleRegistry.currentState != Lifecycle.State.DESTROYED
        ) {
            return
        }

        isDismissed = false

        initContext()

        setupLifecycle()

        addFloatingContent(
            context = actualContext,
            arrowColor = arrowColor,
            onRequestHide = { hideContentView(restoreFabPosition = true) },
            content = expandedContent,
        )

        addFab(
            context = actualContext,
            position = position,
            onClick = {
                if (isContentViewVisible()) {
                    hideContentView(restoreFabPosition = true)
                } else {
                    showContentView(rememberFabPosition = true)
                }
            },
            content = bubble,
        )
    }

    private fun initContext() {
        if (::actualContext.isInitialized) return
        this.actualContext = if (context is Service) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val displayManager = context.getSystemService<DisplayManager>()!!
                val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
                context.createWindowContext(display, WIN_TYPE, null)
            } else {
                context.createConfigurationContext(Configuration())
            }
        } else {
            context
        }
        windowManager = actualContext.getSystemService()!!
    }

    private fun setupLifecycle() {
        lifecycleRegistry = LifecycleRegistry(this)

        savedStateRegistryController = SavedStateRegistryController.create(this)
        savedStateRegistryController.performRestore(null)

        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addFab(
        context: Context,
        position: SnapPosition,
        onClick: () -> Unit,
        content: @Composable () -> Unit,
    ) {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val density = Density(context)

        val winFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LOCAL_FOCUS_MODE
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WIN_TYPE,
            winFlags,
            PixelFormat.TRANSPARENT,
        )
        // Force deliver touch events to behind windows
        params.alpha = 0.8f
        params.gravity = Gravity.TOP or Gravity.START

        val container = FrameLayout(context)
        container.setViewTreeSavedStateRegistryOwner(this)
        ViewTreeLifecycleOwner.set(container, this)
        this.fabContainer = container

        val dropToDismissSizeDp = 96.dp
        val dropToDismissSize = with(density) { dropToDismissSizeDp.roundToPx() }
        val dropToDismiss = createFab(context = context) {
            BubbleFab(
                modifier = Modifier.size(dropToDismissSizeDp),
                elevation = 0.dp,
                backgroundColor = Color(0xFFE03131),
                contentColor = Color.White,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_close_24),
                    contentDescription = null,
                )
            }
        }
        val dropToDismissBottomMargin = with(density) { 32.dp.roundToPx() }
        val dropToDismissLP = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = dropToDismissBottomMargin
        }
        dropToDismiss.translationY = dropToDismissBottomMargin.toFloat()
        dropToDismiss.alpha = 0f
        dropToDismiss.visibility = View.GONE
        container.addView(dropToDismiss, dropToDismissLP)

        var shouldShowContentView = false

        fun showDropToDismiss() {
            dropToDismiss.visibility = View.VISIBLE
            dropToDismiss.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(275)
                .setInterpolator(FastOutSlowInInterpolator())
                .withEndAction(null)
                .start()

            shouldShowContentView = if (isContentViewVisible()) {
                hideContentView(restoreFabPosition = false)
                true
            } else {
                false
            }
        }

        fun hideDropToDismiss(showContentViewIfNeeded: Boolean) {
            dropToDismiss.animate()
                .alpha(0f)
                .translationY(dropToDismissBottomMargin.toFloat())
                .setDuration(225)
                .setInterpolator(FastOutSlowInInterpolator())
                .withEndAction { dropToDismiss.visibility = View.GONE }
                .start()

            if (showContentViewIfNeeded && shouldShowContentView) {
                showContentView(rememberFabPosition = false)
            }
        }

        var preparedToDismiss = false

        fun showPreparedToDismiss() {
            preparedToDismiss = true
            dropToDismiss.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            dropToDismiss.animate()
                .scaleX(1.5f)
                .scaleY(1.5f)
                .setDuration(225)
                .setInterpolator(FastOutSlowInInterpolator())
                .withEndAction(null)
                .start()
        }

        fun hidePreparedToDismiss() {
            preparedToDismiss = false
            dropToDismiss.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(225)
                .setInterpolator(FastOutSlowInInterpolator())
                .withEndAction(null)
                .start()
        }

        val fab = createFab(context = context, content = content)
        this.fab = fab

        container.addView(fab)
        windowManager.addView(container, params)

        fun snapFab() {
            if (!isContentViewVisible() && !shouldShowContentView) {
                val centerX = screenWidth / 2f
                val fabWidth = fab.width.toFloat()
                val targetX = if (fab.translationX + fabWidth / 2f >= centerX) {
                    screenWidth - fabWidth
                } else {
                    0f
                }
                val targetY = fab.translationY.coerceIn(0f, screenHeight - fab.height.toFloat())
                animateFabTo(
                    targetX = targetX,
                    targetY = targetY,
                    duration = 525L,
                )
            } else {
                animateFabToTopCenter()
            }
        }

        fun hideFab(onEnd: () -> Unit) {
            fab.animate()
                .scaleX(0f)
                .scaleY(0f)
                .setDuration(225)
                .setInterpolator(FastOutSlowInInterpolator())
                .withEndAction(onEnd)
                .start()
        }

        fun isFabInsideDropToDismiss(): Boolean {
            val x = fab.translationX + fab.width / 2
            val y = fab.translationY + fab.height / 2
            val cx = dropToDismiss.x + dropToDismiss.width / 2
            val cy = dropToDismiss.y + dropToDismiss.height / 2
            val radius = dropToDismissSize / 2f
            return isPointInsideCircle(x, y, cx, cy, radius)
        }

        val touchableWinFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        val fabSize = with(density) { 56.dp.roundToPx() }
        val touchableFabParams = WindowManager.LayoutParams(
            fabSize,
            fabSize,
            WIN_TYPE,
            touchableWinFlags,
            PixelFormat.TRANSPARENT
        )
        touchableFabParams.gravity = Gravity.TOP or Gravity.START
        this.touchableFabParams = touchableFabParams

        val touchableFab = View(context)
        this.touchableFab = touchableFab
        touchableFab.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        touchableFab.setOnClickListener { onClick() }
        makeFabDraggable(
            touchableFab = touchableFab,
            onStartDrag = { showDropToDismiss() },
            onDragging = { offsetX, offsetY ->
                fab.translationX = offsetX
                fab.translationY = offsetY
                if (isFabInsideDropToDismiss()) {
                    if (!preparedToDismiss) {
                        showPreparedToDismiss()
                    }
                } else {
                    if (preparedToDismiss) {
                        hidePreparedToDismiss()
                    }
                }
            },
            onEndDrag = {
                if (dropToDismiss.alpha != 0f) {
                    hideDropToDismiss(showContentViewIfNeeded = !preparedToDismiss)
                }
                if (preparedToDismiss) {
                    hideContentView(restoreFabPosition = false)
                    hideFab { dismiss() }
                    return@makeFabDraggable
                }
                snapFab()
                touchableFabParams.x = fab.translationX.toInt()
                touchableFabParams.y = fab.translationY.toInt()
                windowManager.updateViewLayout(touchableFab, touchableFabParams)
            },
        )

        windowManager.addView(touchableFab, touchableFabParams)

        val layoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                fab.viewTreeObserver.removeOnGlobalLayoutListener(this)
                placeAndShowFab(fab, screenWidth, screenHeight, position)
            }
        }
        fab.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun makeFabDraggable(
        touchableFab: View,
        onStartDrag: () -> Unit,
        onDragging: (offsetX: Float, offsetY: Float) -> Unit,
        onEndDrag: () -> Unit,
    ) {
        val fab = this.fab ?: return
        val touchSlop = ViewConfiguration.get(actualContext).scaledTouchSlop
        var downX = 0f
        var downY = 0f
        var touchOffsetX = 0f
        var touchOffsetY = 0f
        var dragStarted = false
        touchableFab.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    touchOffsetX = event.rawX - fab.translationX
                    touchOffsetY = event.rawY - fab.translationY
                }
                MotionEvent.ACTION_MOVE -> {
                    if (dragStarted) {
                        val offX = event.rawX - touchOffsetX
                        val offY = event.rawY - touchOffsetY
                        onDragging(offX, offY)
                    } else if (abs(event.rawX - downX) >= touchSlop ||
                        abs(event.rawY - downY) >= touchSlop
                    ) {
                        dragStarted = true
                        onStartDrag()
                    }
                }
                MotionEvent.ACTION_UP -> {
                    return@setOnTouchListener if (dragStarted) {
                        onEndDrag()
                        dragStarted = false
                        true
                    } else {
                        false
                    }
                }
            }
            false
        }
    }

    private fun placeAndShowFab(
        fab: View,
        screenWidth: Int,
        screenHeight: Int,
        position: SnapPosition,
    ) {
        // Set the initial fab position
        val initialX = if (position.isStartAligned()) 0 else screenWidth - fab.width
        val halfHeight = fab.height / 2f
        val initialY = (screenHeight * position.verticalFriction() + halfHeight)
            .coerceIn(halfHeight, screenHeight - halfHeight)
            .toInt()
        fab.translationX = initialX.toFloat()
        fab.translationY = initialY.toFloat()
        checkNotNull(touchableFabParams).apply {
            x = initialX
            y = initialY
            width = fab.width
            height = fab.height
            windowManager.updateViewLayout(touchableFab, this)
        }

        // Scale in
        fab.scaleX = 0f
        fab.scaleY = 0f
        fab.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(275)
            .setInterpolator(FastOutSlowInInterpolator())
            .start()
    }

    private fun createFab(
        context: Context,
        content: @Composable () -> Unit,
    ): ComposeView {
        val composeView = ComposeView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setViewTreeSavedStateRegistryOwner(this@FloatingView)
            ViewTreeLifecycleOwner.set(this, this@FloatingView)
            setContent { content() }
        }
        return composeView
    }

    private fun animateFabToTopCenter() {
        val fab = checkNotNull(this.fab)
        val density = Density(fab.context)
        val displayMetrics = fab.context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val fabTargetX = screenWidth / 2f - fab.width / 2f
        val fabTargetY = with(density) { 32.dp.toPx() }
        animateFabTo(fabTargetX, fabTargetY)
    }

    private fun animateFabTo(
        targetX: Float,
        targetY: Float,
        duration: Long = 375,
        easing: Easing = EaseOutQuint,
    ) {
        val fab = checkNotNull(this.fab)

        val startX = fab.translationX
        val startY = fab.translationY

        val fabPath = Path().apply {
            moveTo(startX, startY)

            val screenWidth = fab.context.resources.displayMetrics.widthPixels
            val controlX = if (maxOf(startX, targetX) >= screenWidth / 2) {
                // Fab on the right side
                maxOf(startX, targetX)
            } else {
                // Fab on the left side
                minOf(startX, targetX)
            }
            val controlY = minOf(startY, targetY)

            quadTo(controlX, controlY, targetX, targetY)
        }

        val pathMeasure = PathMeasure(fabPath, false)

        val position = floatArrayOf(0f, 0f)

        ValueAnimator.ofFloat(0f, pathMeasure.length).apply {
            this.duration = duration
            this.interpolator = TimeInterpolator {
                easing.transform(it)
            }
            addUpdateListener {
                val distance = it.animatedValue as Float
                pathMeasure.getPosTan(distance, position, null)
                fab.translationX = position[0]
                fab.translationY = position[1]
            }
            addListener(
                onEnd = {
                    val params = checkNotNull(touchableFabParams)
                    params.x = targetX.toInt()
                    params.y = targetY.toInt()
                    windowManager.updateViewLayout(touchableFab, params)
                }
            )
            start()
        }
    }

    private fun addFloatingContent(
        context: Context,
        onRequestHide: () -> Unit,
        arrowColor: @Composable (() -> Color)? = null,
        content: @Composable () -> Unit,
    ) {
        val container = object : FrameLayout(context) {
            override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
                return if (event!!.keyCode == KeyEvent.KEYCODE_BACK &&
                    event.action == KeyEvent.ACTION_UP
                ) {
                    onRequestHide()
                    true
                } else {
                    super.dispatchKeyEvent(event)
                }
            }
        }
        this.contentView = container
        container.setViewTreeSavedStateRegistryOwner(this)
        ViewTreeLifecycleOwner.set(container, this)
        container.visibility = View.GONE

        val composeView = ComposeView(context)
        composeView.setViewTreeSavedStateRegistryOwner(this)
        ViewTreeLifecycleOwner.set(composeView, this)

        val winFlags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
        val params = WindowManager.LayoutParams(
            0,
            0,
            WIN_TYPE,
            winFlags,
            PixelFormat.TRANSPARENT,
        )
        params.gravity = Gravity.TOP or Gravity.START
        contentViewParams = params

        composeView.setContent {
            FloatingContent(
                arrowColor = { arrowColor?.invoke() ?: MaterialTheme.colors.surface },
                onRequestHide = onRequestHide,
                content = content,
            )
        }

        container.addView(composeView)

        windowManager.addView(container, params)
    }

    fun isContentViewVisible(): Boolean {
        val params = contentViewParams ?: return false
        return params.width != 0
    }

    private fun showContentView(
        rememberFabPosition: Boolean,
        onAnimationEnd: (() -> Unit)? = null,
    ) {
        val view = checkNotNull(this.contentView)
        val params = checkNotNull(this.contentViewParams)

        val density = Density(view.context)
        val displayMetrics = view.context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        params.dimAmount = 0.55f
        params.width = WindowManager.LayoutParams.MATCH_PARENT
        params.height = WindowManager.LayoutParams.MATCH_PARENT
        windowManager.updateViewLayout(view, params)

        if (rememberFabPosition) {
            val fab = checkNotNull(this.fab)
            val x = if (fab.translationX + fab.width / 2 >= screenWidth / 2) {
                screenWidth.toFloat() - fab.width
            } else {
                0f
            }
            currentFabOffset = Offset(x, fab.translationY)
        }
        animateFabToTopCenter()

        val initialScale = 0.6f
        val initialTranY = with(density) { -(56.dp.toPx()) }

        view.run {
            visibility = View.VISIBLE
            pivotX = screenWidth / 2f
            pivotY = screenHeight / 4f
            translationY = initialTranY
            scaleX = initialScale
            scaleY = initialScale
            alpha = 0f
        }

        contentDimAnim?.cancel()

        contentAnim?.cancel()
        contentAnim = runSpringAnimation(
            onUpdate = { _, progress ->
                view.run {
                    alpha = progress
                    scaleX = initialScale + (1f - initialScale) * progress
                    scaleY = scaleX
                    translationY = initialTranY * (1f - progress)
                }
            },
            onEnd = { onAnimationEnd?.invoke() },
        )
    }

    fun hideContentView(
        restoreFabPosition: Boolean,
        onAnimationEnd: (() -> Unit)? = null,
    ) {
        val view = checkNotNull(this.contentView)
        val params = checkNotNull(this.contentViewParams)

        if (restoreFabPosition) {
            animateFabTo(currentFabOffset.x, currentFabOffset.y)
        }

        view.run {
            val density = Density(view.context)
            pivotX = view.context.resources.displayMetrics.widthPixels / 2f
            pivotY = with(density) { 16.dp.toPx() }
        }

        contentDimAnim?.cancel()
        contentDimAnim = ValueAnimator.ofFloat(0.55f, 0f).apply {
            duration = 255
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener {
                if (contentView == null) {
                    it.cancel()
                    onAnimationEnd?.invoke()
                    return@addUpdateListener
                }
                params.dimAmount = it.animatedValue as Float
                windowManager.updateViewLayout(view, params)
            }
            addListener(onEnd = { onAnimationEnd?.invoke() })
            start()
        }

        contentAnim?.cancel()
        contentAnim = runSpringAnimation(
            quickly = true,
            onUpdate = { anim, progress ->
                if (contentView == null) {
                    anim.cancel()
                    return@runSpringAnimation
                }
                view.run {
                    alpha = 1f - progress
                    scaleX = 1f - 0.5f * progress
                    scaleY = scaleX
                }
            },
            onEnd = { canceled ->
                if (canceled || contentView == null) return@runSpringAnimation
                view.visibility = View.GONE
                params.width = 0
                params.height = 0
                windowManager.updateViewLayout(view, params)
            },
        )
    }

    private fun runSpringAnimation(
        quickly: Boolean = false,
        onEnd: ((canceled: Boolean) -> Unit)? = null,
        onUpdate: (anim: SpringAnimation, progress: Float) -> Unit,
    ): SpringAnimation {
        val valueHolder = FloatValueHolder()
        val maxValue = 10000f
        return SpringAnimation(valueHolder).apply {
            setMinValue(0f)
            setMaxValue(maxValue * 1.1f)

            setStartVelocity(0f)

            spring = SpringForce()
            if (quickly) {
                spring.dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY
                spring.stiffness = SpringForce.STIFFNESS_LOW + 800
            } else {
                spring.dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY + 0.05f
                spring.stiffness = SpringForce.STIFFNESS_LOW + 400f
            }

            addUpdateListener { _, _, _ ->
                onUpdate(this, valueHolder.value / maxValue)
            }

            addEndListener { _, canceled, _, _ ->
                onEnd?.invoke(canceled)
            }

            animateToFinalPosition(maxValue)
        }
    }

    fun dismiss() {
        if (isDismissed) return

        if (lifecycleRegistry.currentState != Lifecycle.State.INITIALIZED) {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }

        isDismissed = true

        val view = contentView ?: return
        windowManager.removeView(view)
        this.contentView = null

        val fabContainer = this.fabContainer ?: return
        windowManager.removeView(fabContainer)
        this.fabContainer = null
        this.fab = null

        val touchableFab = this.touchableFab ?: return
        windowManager.removeView(touchableFab)

        onDismiss?.invoke()
    }

    fun isDismissed() = isDismissed

    fun setOnDismissListener(onDismiss: () -> Unit) {
        this.onDismiss = onDismiss
    }

    companion object {
        private val WIN_TYPE = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("deprecation")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }
    }
}
