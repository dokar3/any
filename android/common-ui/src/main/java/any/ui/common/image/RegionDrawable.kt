package any.ui.common.image

import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Insets
import android.graphics.Outline
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.Region
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import androidx.annotation.RequiresApi
import androidx.compose.ui.unit.IntRect
import org.xmlpull.v1.XmlPullParser

internal class RegionDrawable(
    private val inner: Drawable,
    val region: IntRect?,
) : Drawable() {
    override fun draw(canvas: Canvas) {
        inner.draw(canvas)
    }

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        inner.setBounds(left, top, right, bottom)
    }

    override fun setBounds(bounds: Rect) {
        inner.bounds = bounds
    }

    override fun getDirtyBounds(): Rect {
        return inner.dirtyBounds
    }

    override fun setChangingConfigurations(configs: Int) {
        inner.changingConfigurations = configs
    }

    override fun getChangingConfigurations(): Int {
        return inner.changingConfigurations
    }

    @Deprecated("Deprecated in Java")
    @Suppress("deprecation")
    override fun setDither(dither: Boolean) {
        inner.setDither(dither)
    }

    override fun setFilterBitmap(filter: Boolean) {
        inner.setFilterBitmap(filter)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun isFilterBitmap(): Boolean {
        return inner.isFilterBitmap
    }

    override fun getCallback(): Callback? {
        return inner.callback
    }

    override fun invalidateSelf() {
        inner.invalidateSelf()
    }

    override fun scheduleSelf(what: Runnable, `when`: Long) {
        inner.scheduleSelf(what, `when`)
    }

    override fun unscheduleSelf(what: Runnable) {
        inner.unscheduleSelf(what)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun getLayoutDirection(): Int {
        return inner.layoutDirection
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onLayoutDirectionChanged(layoutDirection: Int): Boolean {
        return inner.onLayoutDirectionChanged(layoutDirection)
    }

    override fun setAlpha(alpha: Int) {
        inner.alpha = alpha
    }

    override fun getAlpha(): Int {
        return inner.alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        inner.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    @Suppress("deprecation")
    override fun setColorFilter(color: Int, mode: PorterDuff.Mode) {
        inner.setColorFilter(color, mode)
    }

    override fun setTint(tintColor: Int) {
        inner.setTint(tintColor)
    }

    override fun setTintList(tint: ColorStateList?) {
        inner.setTintList(tint)
    }

    override fun setTintMode(tintMode: PorterDuff.Mode?) {
        inner.setTintMode(tintMode)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun setTintBlendMode(blendMode: BlendMode?) {
        inner.setTintBlendMode(blendMode)
    }

    override fun getColorFilter(): ColorFilter? {
        return inner.colorFilter
    }

    override fun clearColorFilter() {
        inner.clearColorFilter()
    }

    override fun setHotspot(x: Float, y: Float) {
        inner.setHotspot(x, y)
    }

    override fun setHotspotBounds(left: Int, top: Int, right: Int, bottom: Int) {
        inner.setHotspotBounds(left, top, right, bottom)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun getHotspotBounds(outRect: Rect) {
        inner.getHotspotBounds(outRect)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun isProjected(): Boolean {
        return inner.isProjected
    }

    override fun isStateful(): Boolean {
        return inner.isStateful
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun hasFocusStateSpecified(): Boolean {
        return inner.hasFocusStateSpecified()
    }

    override fun setState(stateSet: IntArray): Boolean {
        return inner.setState(stateSet)
    }

    override fun getState(): IntArray {
        return inner.state
    }

    override fun jumpToCurrentState() {
        inner.jumpToCurrentState()
    }

    override fun getCurrent(): Drawable {
        return inner.current
    }

    override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
        return inner.setVisible(visible, restart)
    }

    override fun setAutoMirrored(mirrored: Boolean) {
        inner.setAutoMirrored(mirrored)
    }

    override fun isAutoMirrored(): Boolean {
        return inner.isAutoMirrored
    }

    override fun applyTheme(t: Resources.Theme) {
        inner.applyTheme(t)
    }

    override fun canApplyTheme(): Boolean {
        return inner.canApplyTheme()
    }

    @Deprecated("Deprecated in Java")
    @Suppress("deprecation")
    override fun getOpacity(): Int {
        return inner.opacity
    }

    override fun getTransparentRegion(): Region? {
        return inner.transparentRegion
    }

    override fun getIntrinsicWidth(): Int {
        return inner.intrinsicWidth
    }

    override fun getIntrinsicHeight(): Int {
        return inner.intrinsicHeight
    }

    override fun getMinimumWidth(): Int {
        return inner.getMinimumWidth()
    }

    override fun getMinimumHeight(): Int {
        return inner.getMinimumHeight()
    }

    override fun getPadding(padding: Rect): Boolean {
        return inner.getPadding(padding)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun getOpticalInsets(): Insets {
        return inner.opticalInsets
    }

    override fun getOutline(outline: Outline) {
        inner.getOutline(outline)
    }

    override fun mutate(): Drawable {
        return inner.mutate()
    }

    override fun inflate(r: Resources, parser: XmlPullParser, attrs: AttributeSet) {
        inner.inflate(r, parser, attrs)
    }

    override fun inflate(
        r: Resources,
        parser: XmlPullParser,
        attrs: AttributeSet,
        theme: Resources.Theme?
    ) {
        inner.inflate(r, parser, attrs, theme)
    }

    override fun getConstantState(): ConstantState? {
        return inner.constantState
    }

    companion object {
        fun of(drawable: Drawable?, region: IntRect?): Drawable? {
            drawable ?: return null
            return RegionDrawable(drawable, region)
        }
    }
}