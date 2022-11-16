package any.ui.imagepager

import android.graphics.ColorFilter
import android.graphics.Paint
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView

fun SubsamplingScaleImageView.setColorFilter(filter: ColorFilter?) {
    SubsamplingIVColorFilterHook.applyColorFilterTo(this, filter)
}

private object SubsamplingIVColorFilterHook {
    private val bitmapPaintField by lazy {
        SubsamplingScaleImageView::class.java.getDeclaredField("bitmapPaint").also {
            it.isAccessible = true
        }
    }

    private val createPaintsMethod by lazy {
        SubsamplingScaleImageView::class.java.getDeclaredMethod("createPaints").also {
            it.isAccessible = true
        }
    }

    fun applyColorFilterTo(
        imageView: SubsamplingScaleImageView,
        colorFilter: ColorFilter?
    ) {
        createPaintsMethod.invoke(imageView)
        (bitmapPaintField.get(imageView) as? Paint)?.colorFilter = colorFilter
    }
}