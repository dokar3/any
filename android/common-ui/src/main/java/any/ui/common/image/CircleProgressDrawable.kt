package any.ui.common.image

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.drawable.Drawable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.graphics.withRotation
import com.facebook.drawee.drawable.CloneableDrawable
import com.facebook.drawee.drawable.DrawableUtils

class CircleProgressDrawable : Drawable(), CloneableDrawable {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }
    private val path = Path()
    private val tempPath = Path()
    private val pathMeasure = PathMeasure()

    var progressColor: Int = 0xFF25BB77.toInt()
        set(value) {
            field = value
            invalidateSelf()
        }

    var secondaryProgressColor: Int = 0xFF567768.toInt()
        set(value) {
            field = value
            invalidateSelf()
        }

    var barWidth: Float = 8f
        set(value) {
            field = value
            paint.strokeWidth = value
            invalidateSelf()
        }

    var radius: Float = 40f
        set(value) {
            field = value
            invalidateSelf()
        }

    var padding: Float = 20f
        set(value) {
            field = value
            invalidateSelf()
        }

    var alignment: Alignment = Alignment.TopEnd
        set(value) {
            field = value
            invalidateSelf()
        }

    var layoutDirection: LayoutDirection = LayoutDirection.Ltr
        set(value) {
            field = value
            invalidateSelf()
        }

    override fun draw(canvas: Canvas) {
        val progress = (level / MAX_LEVEL.toFloat()).coerceIn(0f, 1f)
        if (progress == 0f) {
            return
        }
        canvas.drawBar(progress = 1f, color = secondaryProgressColor)
        canvas.drawBar(progress = progress, color = progressColor)
    }

    private fun Canvas.drawBar(
        progress: Float,
        color: Int,
    ) {
        val bounds = bounds
        val width = bounds.width()
        val height = bounds.height()
        val barSize = (radius * 2).toInt()

        val offset = alignment.align(
            size = IntSize(barSize, barSize),
            space = IntSize(width - padding.toInt() * 2, height - padding.toInt() * 2),
            layoutDirection = layoutDirection
        )

        val circleCx = offset.x + padding + barSize / 2f
        val circleCy = offset.y + padding + barSize / 2f

        path.reset()
        path.addCircle(circleCx, circleCy, radius, Path.Direction.CW)

        tempPath.reset()
        pathMeasure.setPath(path, true)
        pathMeasure.getSegment(0f, pathMeasure.length * progress, tempPath, true)

        paint.color = color
        withRotation(
            degrees = -90f,
            pivotX = circleCx,
            pivotY = circleCy
        ) {
            drawPath(tempPath, paint)
        }
    }

    override fun onLevelChange(level: Int): Boolean {
        super.onLevelChange(level)
        invalidateSelf()
        return true
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int {
        return DrawableUtils.getOpacityFromColor(paint.color)
    }

    override fun cloneDrawable(): Drawable {
        return CircleProgressDrawable().also {
            it.progressColor = progressColor
            it.secondaryProgressColor = progressColor
            it.barWidth = barWidth
            it.radius = radius
            it.padding = padding
            it.alignment = alignment
            it.layoutDirection = layoutDirection
        }
    }

    companion object {
        private const val MAX_LEVEL = 10000
    }
}