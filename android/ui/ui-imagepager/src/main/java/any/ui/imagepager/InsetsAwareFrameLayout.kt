package any.ui.imagepager

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.WindowInsets
import android.widget.FrameLayout

class InsetsAwareFrameLayout : FrameLayout {
    private var onDispatchWindowInsets: ((Rect) -> Unit)? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    @Suppress("deprecation")
    override fun dispatchApplyWindowInsets(insets: WindowInsets?): WindowInsets {
        val rect = Rect(
            insets?.systemWindowInsetLeft ?: 0,
            insets?.systemWindowInsetTop ?: 0,
            insets?.systemWindowInsetRight ?: 0,
            insets?.systemWindowInsetTop ?: 0,
        )
        onDispatchWindowInsets?.invoke(rect)
        return super.dispatchApplyWindowInsets(insets)
    }

    fun doOnDispatchWindowInsets(action: (Rect) -> Unit) {
        this.onDispatchWindowInsets = action
    }
}