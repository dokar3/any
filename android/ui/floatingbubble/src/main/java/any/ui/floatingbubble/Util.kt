package any.ui.floatingbubble

import kotlin.math.pow
import kotlin.math.sqrt

internal fun isPointInsideCircle(
    x: Float,
    y: Float,
    centerX: Float,
    centerY: Float,
    radius: Float,
): Boolean {
    return sqrt((x - centerX).pow(2) + (y - centerY).pow(2)) <= radius
}