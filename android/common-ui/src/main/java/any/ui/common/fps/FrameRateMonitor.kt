package any.ui.common.fps

import android.app.Activity
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Choreographer
import android.view.FrameMetrics
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.Window.OnFrameMetricsAvailableListener
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

object FrameRateMonitor {
    private val FRAME_RATE_TV_ID = View.generateViewId()

    private var monitorJob: Job? = null

    @OptIn(FlowPreview::class)
    fun start(activity: ComponentActivity) {
        stop(activity)
        monitorJob = activity.lifecycleScope.launch {
            activity.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val root: ViewGroup = activity.findViewById(android.R.id.content)
                val tvFrameRate = TextView(root.context).apply {
                    id = FRAME_RATE_TV_ID
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                    setTypeface(Typeface.defaultFromStyle(Typeface.BOLD))
                }
                val lp = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    gravity = Gravity.END
                }
                root.addView(tvFrameRate, lp)
                // Update position from insets
                val margin = (8 * activity.resources.displayMetrics.density).toInt()
                ViewCompat.setOnApplyWindowInsetsListener(tvFrameRate) { view, windowInsets ->
                    val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                    view.updateLayoutParams<MarginLayoutParams> {
                        topMargin = margin + insets.top
                        bottomMargin = margin + insets.bottom
                        leftMargin = margin + insets.left
                        rightMargin = margin + insets.right
                    }
                    windowInsets
                }
                tvFrameRate.requestApplyInsets()

                // Cleanup
                this.coroutineContext.job.invokeOnCompletion {
                    root.removeView(tvFrameRate)
                }

                // Collect frame rate updates
                frameRateFlow(activity)
                    .distinctUntilChanged()
                    .debounce(5)
                    .collect { frameRate ->
                        tvFrameRate.text = String.format("%.1f", frameRate)
                        if (frameRate >= 50) {
                            tvFrameRate.setTextColor(Color.Green.toArgb())
                        } else if (frameRate >= 40) {
                            tvFrameRate.setTextColor(Color.Yellow.toArgb())
                        } else {
                            tvFrameRate.setTextColor(Color.Red.toArgb())
                        }
                    }
            }
        }
    }

    fun stop(activity: Activity) {
        monitorJob?.cancel()
        val tv = activity.findViewById<View>(FRAME_RATE_TV_ID) ?: return
        val parent = tv.parent as ViewGroup
        parent.removeView(tv)
    }

    private fun frameRateFlow(activity: Activity): Flow<Float> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            frameRateFlowApi24(activity)
        } else {
            frameRateFlowPreApi24(activity)
        }
    }

    private fun frameRateFlowPreApi24(activity: Activity): Flow<Float> {
        val choreographer = Choreographer.getInstance()
        val refreshRate = displayRefreshRate(activity)
        return channelFlow {
            var lastFrameTimeNanos = 0L
            val callback = object : Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    if (!isActive) {
                        close()
                        return
                    }
                    if (lastFrameTimeNanos == 0L) {
                        lastFrameTimeNanos = frameTimeNanos
                        choreographer.postFrameCallback(this)
                        return
                    }
                    val diffMillis = (frameTimeNanos - lastFrameTimeNanos) / 1000000f
                    val frameRate = 1000f / diffMillis
                    lastFrameTimeNanos = frameTimeNanos
                    choreographer.postFrameCallback(this)
                    trySend(frameRate.coerceIn(0f, refreshRate))
                }
            }
            choreographer.postFrameCallback(callback)
            awaitClose()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun frameRateFlowApi24(activity: Activity): Flow<Float> {
        val refreshRate = displayRefreshRate(activity)
        return channelFlow {
            val listener = OnFrameMetricsAvailableListener { _, frameMetrics, _ ->
                val frameDurationNanos = frameMetrics.getMetric(FrameMetrics.TOTAL_DURATION)
                val frameDurationMillis = frameDurationNanos / 1000000f
                val frameRate = 1000f / frameDurationMillis
                trySend(frameRate.coerceIn(0f, refreshRate))
            }
            val handler = Handler(Looper.getMainLooper())
            activity.window.addOnFrameMetricsAvailableListener(listener, handler)
            awaitClose()
            activity.window.removeOnFrameMetricsAvailableListener(listener)
        }
    }

    private fun displayRefreshRate(activity: Activity): Float {
        return activity.window.decorView.display?.refreshRate ?: 60f
    }
}