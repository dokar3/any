package any.base.util

import android.graphics.Bitmap

fun Bitmap.copyTo(target: Bitmap) {
    val w = width
    val h = height
    require(target.width == w && target.height == h) {
        "Two bitmaps must have same size"
    }
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, w, 0, 0, w, h)
    target.setPixels(pixels, 0, w, 0, 0, w, h)
}