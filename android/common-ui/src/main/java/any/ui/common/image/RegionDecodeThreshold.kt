package any.ui.common.image

import androidx.compose.runtime.Immutable
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2

@Suppress("FunctionName")
fun RegionDecodeThreshold(
    minHeight: Int,
    maxAspectRatio: Float,
): RegionDecodeThresholds = RegionDecodeThresholds(
    value = packFloats(minHeight.toFloat(), maxAspectRatio)
)

@Immutable
@JvmInline
value class RegionDecodeThresholds(private val value: Long) {
    val minHeight: Int
        get() = unpackFloat1(value).toInt()

    val maxAspectRatio: Float
        get() = unpackFloat2(value)

    companion object {
        /**
         * Default thresholds, which helps memory usage and quality for large and long images.
         */
        val Default = RegionDecodeThreshold(
            minHeight = 2000,
            maxAspectRatio = 2f / 3f,
        )

        // No image satisfies these thresholds
        val Disabled = RegionDecodeThreshold(
            minHeight = Int.MAX_VALUE,
            maxAspectRatio = 0f,
        )
    }
}
