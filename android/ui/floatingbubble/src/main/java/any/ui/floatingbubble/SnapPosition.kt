package any.ui.floatingbubble

import kotlin.math.abs

@JvmInline
value class SnapPosition private constructor(
    private val value: Float
) {
    fun isStartAligned(): Boolean {
        return value >= 0
    }

    fun verticalFriction(): Float {
        return abs(value)
    }

    companion object {
        fun startAligned(verticalFriction: Float): SnapPosition {
            require(verticalFriction in 0f..1f)
            return SnapPosition(verticalFriction)
        }

        fun endAligned(verticalFriction: Float): SnapPosition {
            require(verticalFriction in 0f..1f)
            return SnapPosition(-verticalFriction)
        }
    }
}