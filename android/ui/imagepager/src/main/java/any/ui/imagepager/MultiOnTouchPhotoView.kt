package any.ui.imagepager

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView

class MultiOnTouchPhotoView : SubsamplingScaleImageView {
    private var extraOnTouchListeners = mutableListOf<OnTouchListener>()

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attr: AttributeSet?) : super(context, attr)

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        extraOnTouchListeners.forEach { it.onTouch(this, event) }
        return super.dispatchTouchEvent(event)
    }

    fun addOnTouchListener(onTouchListener: OnTouchListener) {
        extraOnTouchListeners.add(onTouchListener)
    }

    @SuppressLint("ClickableViewAccessibility")
    inline fun addOnTouchListener(
        crossinline onTouch: (View, MotionEvent) -> Boolean
    ) {
        val listener = OnTouchListener { v, event ->
            onTouch(v, event)
        }
        addOnTouchListener(listener)
    }

    fun removeOnTouchListener(onTouchListener: OnTouchListener) {
        extraOnTouchListeners.remove(onTouchListener)
    }
}